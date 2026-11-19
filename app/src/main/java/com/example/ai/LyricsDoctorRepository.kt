package com.example.ai

import android.util.Log
import com.example.audio.AppLanguage
import com.example.audio.DecodedAudio
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import kotlin.math.min
import kotlin.math.roundToInt

private const val TAG = "LyricsDoctorRepo"
private const val MAX_INLINE_AUDIO_BYTES = 18 * 1024 * 1024 // Stay safely under 20 MB limit

data class LyricCorrectionItem(
    val originalLine: String,
    val fixedLine: String,
    val reason: String
)

data class LyricsFixResult(
    val summary: String,
    val correctedLyrics: String,
    val corrections: List<LyricCorrectionItem>
)

sealed interface LyricsDoctorUiState {
    data object Idle : LyricsDoctorUiState
    data object Loading : LyricsDoctorUiState
    data class Success(val result: LyricsFixResult) : LyricsDoctorUiState
    data class Error(val message: String) : LyricsDoctorUiState
}

class LyricsDoctorRepository {

    private val lyricFixSchema = Schema.obj(
        properties = mapOf(
            "summary" to Schema.string(description = "Brief summary of the lyric errors found and fixed"),
            "correctedLyrics" to Schema.string(description = "Complete corrected lyrics preserving verse/chorus line breaks"),
            "corrections" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "originalLine" to Schema.string(description = "Original line or phrase containing an error"),
                        "fixedLine" to Schema.string(description = "Corrected version of the line or phrase"),
                        "reason" to Schema.string(description = "Why it was fixed (spelling, grammar, rhythm, or clarity)")
                    )
                ),
                description = "Line-by-line list of lyric mistakes fixed"
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = lyricFixSchema
            }
        )
    }

    suspend fun fixLyricsText(
        draftLyrics: String,
        language: AppLanguage
    ): LyricsFixResult {
        val trimmed = draftLyrics.trim()
        require(trimmed.isNotEmpty()) {
            if (language == AppLanguage.SINHALA) {
                "කරුණාකර පළමුව lyrics ඇතුළත් කරන්න."
            } else {
                "Please enter or paste lyrics first."
            }
        }

        val responseLanguageInstruction = if (language == AppLanguage.SINHALA) {
            "Write 'summary' and every 'reason' in natural Sinhala (සිංහල). Keep 'correctedLyrics', 'originalLine', and 'fixedLine' in the original language of the song lyrics (Sinhala, English, or mixed)."
        } else {
            "Write 'summary' and every 'reason' in clear English. Keep 'correctedLyrics', 'originalLine', and 'fixedLine' in the original language of the song lyrics."
        }

        val prompt = """
            You are Swara Studio's expert Song Doctor & Lyric Editor (fluent in Sinhala, English, and Singlish songwriting).
            Analyze the song lyrics below and fix all errors while preserving the song's emotion, meaning, melody, and rhythm:
            1. Fix spelling mistakes (especially Sinhala Unicode spelling like න/ණ, ල/ළ, ස/ශ/ෂ, පිල්ලම්, and English typos). If Singlish is used, clean up spelling/grammar naturally or convert unclear typos cleanly while preserving rhythm.
            2. Fix grammar, awkward phrasing, broken meter/syllable flow, and weak rhyme mismatches.
            3. Keep verse/chorus line breaks clean and readable.
            4. $responseLanguageInstruction

            Song Lyrics:
            $trimmed
        """.trimIndent()

        return runCatchingModel {
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            parseJsonResponse(response.text)
        }
    }

    suspend fun extractAndFixFromAudio(
        decodedAudio: DecodedAudio,
        optionalDraftLyrics: String,
        language: AppLanguage
    ): LyricsFixResult {
        val compactWavBytes = withContext(Dispatchers.Default) {
            encodeCompactMonoWavForAi(decodedAudio)
        }

        val responseLanguageInstruction = if (language == AppLanguage.SINHALA) {
            "Write 'summary' and every 'reason' in natural Sinhala (සිංහල). Keep 'correctedLyrics', 'originalLine', and 'fixedLine' in the song's sung language (Sinhala/English)."
        } else {
            "Write 'summary' and every 'reason' in clear English. Keep 'correctedLyrics', 'originalLine', and 'fixedLine' in the song's sung language."
        }

        val draftSection = if (optionalDraftLyrics.isNotBlank()) {
            "\nReference draft lyrics provided by user (compare against the vocals and fix mistakes in the draft):\n${optionalDraftLyrics.trim()}"
        } else {
            "\nNo draft lyrics were provided. Transcribe the sung vocals accurately, identify any grammar/pronunciation/wording/rhyme flaws, and provide polished corrected lyrics."
        }

        val promptText = """
            You are Swara Studio's expert Song Doctor & Lyric Editor (fluent in Sinhala and English vocals).
            Listen carefully to the vocals in this audio track (${decodedAudio.fileName}).
            $draftSection
            $responseLanguageInstruction
        """.trimIndent()

        return runCatchingModel {
            val response = withTimeout(60_000L) {
                model.generateContent(
                    content {
                        inlineData(compactWavBytes, "audio/wav")
                        text(promptText)
                    }
                )
            }
            parseJsonResponse(response.text)
        }
    }

    private suspend inline fun runCatchingModel(
        crossinline block: suspend () -> LyricsFixResult
    ): LyricsFixResult {
        return try {
            block()
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini lyrics request timed out", e)
            throw IllegalStateException("Request timed out after 60 seconds. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini lyrics request failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Failed to analyze lyrics with AI.")
        }
    }

    private fun parseJsonResponse(rawText: String?): LyricsFixResult {
        val jsonText = rawText?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Model returned an empty response.")
        val json = JSONObject(jsonText)
        val summary = json.optString("summary", "")
        val correctedLyrics = json.optString("correctedLyrics", "")
        val arr = json.optJSONArray("corrections")
        val corrections = buildList {
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val orig = obj.optString("originalLine", "")
                    val fixed = obj.optString("fixedLine", "")
                    val reason = obj.optString("reason", "")
                    if (orig.isNotBlank() || fixed.isNotBlank()) {
                        add(LyricCorrectionItem(orig, fixed, reason))
                    }
                }
            }
        }
        return LyricsFixResult(
            summary = summary,
            correctedLyrics = correctedLyrics,
            corrections = corrections
        )
    }

    /**
     * Downsamples to 16 kHz mono 16-bit PCM WAV capped to stay strictly below 18 MB
     * (around 5+ minutes at 16 kHz mono = 10 MB!) so Gemini inline audio always succeeds.
     */
    private fun encodeCompactMonoWavForAi(decoded: DecodedAudio): ByteArray {
        val targetRate = 16_000
        val srcRate = decoded.sampleRate.coerceAtLeast(8000)
        val srcFrames = decoded.totalFrames
        val ch0 = decoded.channelSamples[0]
        val ch1 = if (decoded.channels >= 2 && decoded.channelSamples.size >= 2) {
            decoded.channelSamples[1]
        } else {
            null
        }

        val ratio = srcRate.toDouble() / targetRate.toDouble()
        val maxOutputFrames = (MAX_INLINE_AUDIO_BYTES - 44) / 2
        val estimatedOutFrames = (srcFrames / ratio).roundToInt().coerceAtLeast(1)
        val outFrames = min(estimatedOutFrames, maxOutputFrames)

        val dataBytes = outFrames * 2
        val out = ByteArrayOutputStream(44 + dataBytes)

        fun writeIntLE(v: Int) {
            out.write(v and 0xFF)
            out.write((v ushr 8) and 0xFF)
            out.write((v ushr 16) and 0xFF)
            out.write((v ushr 24) and 0xFF)
        }

        fun writeShortLE(v: Int) {
            out.write(v and 0xFF)
            out.write((v ushr 8) and 0xFF)
        }

        out.write("RIFF".toByteArray(Charsets.US_ASCII))
        writeIntLE(36 + dataBytes)
        out.write("WAVE".toByteArray(Charsets.US_ASCII))
        out.write("fmt ".toByteArray(Charsets.US_ASCII))
        writeIntLE(16)
        writeShortLE(1) // PCM
        writeShortLE(1) // Mono
        writeIntLE(targetRate)
        writeIntLE(targetRate * 2)
        writeShortLE(2)
        writeShortLE(16)
        out.write("data".toByteArray(Charsets.US_ASCII))
        writeIntLE(dataBytes)

        for (i in 0 until outFrames) {
            val srcIdx = (i * ratio).toInt().coerceIn(0, srcFrames - 1)
            val mono = if (ch1 != null) {
                0.5f * (ch0[srcIdx] + ch1[srcIdx])
            } else {
                ch0[srcIdx]
            }.coerceIn(-1f, 1f)
            val pcm = (if (mono < 0f) mono * 32768f else mono * 32767f)
                .toInt().coerceIn(-32768, 32767)
            writeShortLE(pcm)
        }

        return out.toByteArray()
    }
}
