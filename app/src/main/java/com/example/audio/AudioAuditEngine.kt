package com.example.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Port of Swara Studio's DSP Flaw Analyzer and 140-bar waveform peak extractor.
 */
object AudioAuditEngine {

    data class AuditResult(
        val peaks: List<Float>,
        val peakDb: Float,
        val rmsDb: Float,
        val issues: List<AuditIssue>
    )

    fun analyze(ch0: FloatArray, sampleRate: Int): AuditResult {
        val peaks = extractWaveformPeaks(ch0, bars = 140)

        var peak = 0.0f
        var sqSum = 0.0
        var count = 0
        var i = 0
        while (i < ch0.size) {
            val sample = ch0[i]
            val absVal = abs(sample)
            if (absVal > peak) peak = absVal
            sqSum += sample.toDouble() * sample.toDouble()
            count++
            i += 2
        }

        val rms = sqrt(sqSum / max(1, count))
        val rmsDb = (20.0 * log10(max(1e-6, rms))).toFloat()
        val peakDb = (20.0 * log10(max(1e-6, peak.toDouble()))).toFloat()

        val mudDb = measureBand(ch0, sampleRate, freq = 250.0, q = 1.2)
        val vocDb = measureBand(ch0, sampleRate, freq = 3200.0, q = 1.0)
        val harshDb = measureBand(ch0, sampleRate, freq = 6800.0, q = 1.5)

        val issues = buildList {
            if (mudDb - vocDb > 4.0) {
                add(
                    AuditIssue(
                        titleSi = "250Hz Low-Mid Mud ගතිය වැඩියි",
                        detailSi = "808/Bass කලාපය Vocal හඬට බාධා කරයි. Mud Cut (-2.6 dB) නිර්දේශිතයි.",
                        titleEn = "Excessive 250Hz Low-Mid Mud",
                        detailEn = "Low-mid buildup is masking vocal clarity. Mud Cut (-2.6 dB) is recommended."
                    )
                )
            }
            if (vocDb < rmsDb - 11.0) {
                add(
                    AuditIssue(
                        titleSi = "Vocal Presence (3.2kHz) අඩුයි",
                        detailSi = "ගායන හඬ සංගීතයට යට වී ඇත. 3.2kHz (+2.8 dB) ඉහළ දමන්න.",
                        titleEn = "Low Vocal Presence (3.2kHz)",
                        detailEn = "Vocals sit behind the instrumental mix. Boost 3.2kHz (+2.8 dB) for clarity."
                    )
                )
            }
            if (harshDb > vocDb - 3.5) {
                add(
                    AuditIssue(
                        titleSi = "6.8kHz Hi-Hat / Sibilance තියුණුයි",
                        detailSi = "වැඩි ශබ්දයකදී කනට තියුණු ගතියක් දැනේ. De-Esser (-2.4 dB) යොදන්න.",
                        titleEn = "Harsh 6.8kHz Hi-Hat / Sibilance",
                        detailEn = "High-frequency transients sound sharp at louder playback. Apply De-Esser (-2.4 dB)."
                    )
                )
            }
            if (peakDb > -1.0f) {
                add(
                    AuditIssue(
                        titleSi = "Streaming Headroom අඩුයි (Clipping අවදානම)",
                        detailSi = "Spotify/YouTube සඳහා -0.6 dBFS Safety Limiter සක්‍රීය කර ඇත.",
                        titleEn = "Low Streaming Headroom (Clipping Risk)",
                        detailEn = "-0.6 dBFS Safety Limiter is active to protect Spotify/YouTube playback."
                    )
                )
            }
        }

        return AuditResult(
            peaks = peaks,
            peakDb = peakDb,
            rmsDb = rmsDb,
            issues = issues
        )
    }

    private fun extractWaveformPeaks(ch0: FloatArray, bars: Int): List<Float> {
        if (ch0.isEmpty()) return List(bars) { 0.05f }
        val step = max(1, ch0.size / bars)
        val peaks = ArrayList<Float>(bars)
        for (i in 0 until bars) {
            var maxAmp = 0f
            val start = i * step
            val end = min(ch0.size, (i + 1) * step)
            var j = start
            while (j < end) {
                val a = abs(ch0[j])
                if (a > maxAmp) maxAmp = a
                j += 8
            }
            peaks.add(maxAmp.coerceIn(0f, 1f))
        }
        return peaks
    }

    private fun measureBand(samples: FloatArray, sr: Int, freq: Double, q: Double): Double {
        val w0 = (2.0 * PI * freq) / sr.toDouble()
        val cosW = cos(w0)
        val alpha = sin(w0) / (2.0 * q)
        val a0 = 1.0 + alpha
        val a1 = -2.0 * cosW
        val a2 = 1.0 - alpha
        val b0 = alpha
        val b1 = 0.0
        val b2 = -alpha

        var x1 = 0.0
        var x2 = 0.0
        var y1 = 0.0
        var y2 = 0.0
        var sq = 0.0
        val n = min(samples.size, sr * 60)
        for (i in 0 until n) {
            val x0 = samples[i].toDouble()
            val y0 = (b0 / a0) * x0 + (b1 / a0) * x1 + (b2 / a0) * x2 - (a1 / a0) * y1 - (a2 / a0) * y2
            x2 = x1; x1 = x0
            y2 = y1; y1 = y0
            sq += y0 * y0
        }
        val rms = sqrt(sq / max(1, n))
        return if (rms > 1e-6) 20.0 * log10(rms) else -96.0
    }
}
