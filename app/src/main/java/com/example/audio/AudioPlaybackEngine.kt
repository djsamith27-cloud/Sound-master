package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min

data class PlaybackTelemetry(
    val isPlaying: Boolean = false,
    val currentSec: Float = 0f,
    val gainReductionDb: Float = 0f,
    val spectrumBars: List<Float> = List(40) { 0f }
)

class AudioPlaybackEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var playJob: Job? = null

    private var audio: DecodedAudio? = null
    private var dspChain: MasteringDspChain? = null
    private val fftAnalyzer = FftAnalyzer()

    @Volatile
    private var currentFrame: Int = 0

    @Volatile
    private var isBypassed: Boolean = false

    @Volatile
    private var currentParams: MasteringParams = MasteringParams()

    private val _telemetry = MutableStateFlow(PlaybackTelemetry())
    val telemetry: StateFlow<PlaybackTelemetry> = _telemetry.asStateFlow()

    fun loadTrack(decoded: DecodedAudio, params: MasteringParams, bypassed: Boolean) {
        stopInternal(resetPosition = true)
        audio = decoded
        currentParams = params
        isBypassed = bypassed
        dspChain = MasteringDspChain(decoded.sampleRate, decoded.channels, params)
        fftAnalyzer.reset()
        _telemetry.value = PlaybackTelemetry(
            isPlaying = false,
            currentSec = 0f,
            gainReductionDb = 0f,
            spectrumBars = List(40) { 0f }
        )
    }

    fun updateParams(params: MasteringParams) {
        currentParams = params
        dspChain?.updateParams(params)
    }

    fun setBypassed(bypassed: Boolean) {
        isBypassed = bypassed
        if (bypassed) {
            _telemetry.value = _telemetry.value.copy(gainReductionDb = 0f)
        }
    }

    fun togglePlayPause() {
        if (_telemetry.value.isPlaying) {
            pause()
        } else {
            playFrom(currentFrame)
        }
    }

    fun seekFraction(fraction: Float) {
        val track = audio ?: return
        val clamped = fraction.coerceIn(0f, 1f)
        val targetFrame = (clamped * track.totalFrames).toInt().coerceIn(0, (track.totalFrames - 1).coerceAtLeast(0))
        val wasPlaying = _telemetry.value.isPlaying
        currentFrame = targetFrame
        val sec = targetFrame.toFloat() / track.sampleRate.toFloat()
        if (wasPlaying) {
            playFrom(targetFrame)
        } else {
            _telemetry.value = _telemetry.value.copy(currentSec = sec)
        }
    }

    fun pause() {
        stopInternal(resetPosition = false)
    }

    fun release() {
        stopInternal(resetPosition = true)
        scope.cancel()
    }

    private fun stopInternal(resetPosition: Boolean) {
        playJob?.cancel()
        playJob = null
        if (resetPosition) {
            currentFrame = 0
        }
        val track = audio
        val sec = if (resetPosition || track == null) 0f else currentFrame.toFloat() / track.sampleRate.toFloat()
        _telemetry.value = _telemetry.value.copy(
            isPlaying = false,
            currentSec = sec,
            gainReductionDb = if (!_telemetry.value.isPlaying) 0f else _telemetry.value.gainReductionDb,
            spectrumBars = List(40) { 0f }
        )
    }

    private fun playFrom(startFrame: Int) {
        val track = audio ?: return
        playJob?.cancel()

        val safeStart = if (startFrame >= track.totalFrames) 0 else startFrame.coerceAtLeast(0)
        currentFrame = safeStart

        val chain = dspChain ?: MasteringDspChain(track.sampleRate, track.channels, currentParams).also {
            dspChain = it
        }
        chain.reset()
        chain.updateParams(currentParams)
        fftAnalyzer.reset()

        playJob = scope.launch {
            val channelMask = if (track.channels >= 2) {
                AudioFormat.CHANNEL_OUT_STEREO
            } else {
                AudioFormat.CHANNEL_OUT_MONO
            }
            val minBufBytes = AudioTrack.getMinBufferSize(
                track.sampleRate,
                channelMask,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(8192)
            val bufferBytes = maxOf(minBufBytes, 16384)

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(track.sampleRate)
                        .setChannelMask(channelMask)
                        .build()
                )
                .setBufferSizeInBytes(bufferBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            val blockFrames = 2048
            val leftOut = FloatArray(blockFrames)
            val rightOut = FloatArray(blockFrames)
            val outChannels = track.channels.coerceIn(1, 2)
            val pcmShorts = ShortArray(blockFrames * outChannels)

            try {
                audioTrack.play()
                _telemetry.value = _telemetry.value.copy(isPlaying = true)

                var pos = safeStart
                while (isActive && pos < track.totalFrames) {
                    val count = min(blockFrames, track.totalFrames - pos)
                    chain.processBlock(
                        srcChannels = track.channelSamples,
                        offset = pos,
                        frameCount = count,
                        leftOut = leftOut,
                        rightOut = rightOut,
                        isBypassed = isBypassed
                    )

                    val bars = fftAnalyzer.computeSpectrumBars(leftOut, rightOut, count)
                    val sec = pos.toFloat() / track.sampleRate.toFloat()
                    _telemetry.value = PlaybackTelemetry(
                        isPlaying = true,
                        currentSec = sec,
                        gainReductionDb = chain.currentGainReductionDb,
                        spectrumBars = bars
                    )

                    var sIdx = 0
                    for (i in 0 until count) {
                        val sL = leftOut[i].coerceIn(-1f, 1f)
                        pcmShorts[sIdx++] = (if (sL < 0f) sL * 32768f else sL * 32767f)
                            .toInt().coerceIn(-32768, 32767).toShort()
                        if (outChannels >= 2) {
                            val sR = rightOut[i].coerceIn(-1f, 1f)
                            pcmShorts[sIdx++] = (if (sR < 0f) sR * 32768f else sR * 32767f)
                                .toInt().coerceIn(-32768, 32767).toShort()
                        }
                    }

                    val written = audioTrack.write(pcmShorts, 0, count * outChannels, AudioTrack.WRITE_BLOCKING)
                    if (written < 0) break

                    pos += count
                    currentFrame = pos
                }

                // Natural end of track
                if (isActive && currentFrame >= track.totalFrames) {
                    currentFrame = 0
                    _telemetry.value = PlaybackTelemetry(
                        isPlaying = false,
                        currentSec = 0f,
                        gainReductionDb = 0f,
                        spectrumBars = List(40) { 0f }
                    )
                }
            } finally {
                try {
                    audioTrack.pause()
                    audioTrack.flush()
                    audioTrack.stop()
                } catch (_: Throwable) {
                }
                audioTrack.release()
            }
        }
    }
}
