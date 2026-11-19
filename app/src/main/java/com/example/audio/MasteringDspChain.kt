package com.example.audio

import java.io.BufferedOutputStream
import java.io.OutputStream
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Stereo / Mono Mastering DSP Chain:
 * 6-Band Parametric Repair EQ -> Soft-Knee Dynamics Compressor -> Mid-Side Stereo Width ->
 * Output Makeup Gain -> -0.6 dBFS Safety Limiter.
 */
class MasteringDspChain(
    private val sampleRate: Int,
    private val channels: Int,
    initialParams: MasteringParams = MasteringParams()
) {
    private val hpfFilter = StereoBiquadFilter(BiquadType.HIGHPASS)
    private val lowFilter = StereoBiquadFilter(BiquadType.LOWSHELF)
    private val mudFilter = StereoBiquadFilter(BiquadType.PEAKING)
    private val presFilter = StereoBiquadFilter(BiquadType.PEAKING)
    private val deessFilter = StereoBiquadFilter(BiquadType.PEAKING)
    private val airFilter = StereoBiquadFilter(BiquadType.HIGHSHELF)

    private val compAttackCoeff = exp(-1.0 / (sampleRate * 0.008))
    private val compReleaseCoeff = exp(-1.0 / (sampleRate * 0.160))
    private val limAttackCoeff = exp(-1.0 / (sampleRate * 0.001))
    private val limReleaseCoeff = exp(-1.0 / (sampleRate * 0.060))
    private val brickwallCeiling = 10.0.pow(-0.6 / 20.0) // -0.6 dBFS

    private var compEnv = 0.0
    private var limEnv = 0.0

    @Volatile
    private var pendingParams: MasteringParams = initialParams
    private var appliedParams: MasteringParams? = null

    @Volatile
    var currentGainReductionDb: Float = 0f
        private set

    init {
        applyParameters(initialParams)
    }

    fun updateParams(params: MasteringParams) {
        pendingParams = params
    }

    fun reset() {
        hpfFilter.resetState()
        lowFilter.resetState()
        mudFilter.resetState()
        presFilter.resetState()
        deessFilter.resetState()
        airFilter.resetState()
        compEnv = 0.0
        limEnv = 0.0
        currentGainReductionDb = 0f
    }

    private fun applyParameters(p: MasteringParams) {
        hpfFilter.configure(sampleRate, p.hpf.toDouble(), q = 0.70710678)
        lowFilter.configure(sampleRate, 90.0, gainDb = p.low.toDouble())
        mudFilter.configure(sampleRate, 250.0, q = 1.4, gainDb = p.mud.toDouble())
        presFilter.configure(sampleRate, 3200.0, q = 1.2, gainDb = p.pres.toDouble())
        deessFilter.configure(sampleRate, 6800.0, q = 2.2, gainDb = p.deess.toDouble())
        airFilter.configure(sampleRate, 11000.0, gainDb = p.air.toDouble())
        appliedParams = p
    }

    private fun computeCompressorGainDb(
        levelLinear: Double,
        thresholdDb: Double,
        ratio: Double,
        kneeDb: Double
    ): Double {
        if (ratio <= 1.001) return 0.0
        val inDb = 20.0 * log10(max(1e-6, levelLinear))
        val diff = inDb - thresholdDb
        val outDb = when {
            2.0 * diff < -kneeDb -> inDb
            2.0 * abs(diff) <= kneeDb -> {
                val x = diff + kneeDb / 2.0
                inDb + (1.0 / ratio - 1.0) * (x * x) / (2.0 * kneeDb)
            }
            else -> thresholdDb + diff / ratio
        }
        return outDb - inDb
    }

    /**
     * Processes [frameCount] samples starting at [offset] from source channel arrays into
     * [leftOut] / [rightOut] (mono copies leftOut to rightOut if channels == 1).
     */
    fun processBlock(
        srcChannels: Array<FloatArray>,
        offset: Int,
        frameCount: Int,
        leftOut: FloatArray,
        rightOut: FloatArray,
        isBypassed: Boolean
    ) {
        val target = pendingParams
        if (target != appliedParams) {
            applyParameters(target)
        }

        val ch0 = srcChannels[0]
        val ch1 = if (channels >= 2 && srcChannels.size >= 2) srcChannels[1] else ch0

        val threshDb = target.thresh.toDouble()
        val ratio = target.ratio.toDouble().coerceAtLeast(1.0)
        val width = target.width.toDouble()
        val makeupLinear = 10.0.pow(target.gain.toDouble() / 20.0)

        var lastGrDb = 0.0

        for (i in 0 until frameCount) {
            val idx = offset + i
            val dryL = ch0[idx].toDouble()
            val dryR = ch1[idx].toDouble()

            // Always run EQ & dynamics state so A/B switching stays seamless and click-free
            var wetL = hpfFilter.processLeft(dryL)
            wetL = lowFilter.processLeft(wetL)
            wetL = mudFilter.processLeft(wetL)
            wetL = presFilter.processLeft(wetL)
            wetL = deessFilter.processLeft(wetL)
            wetL = airFilter.processLeft(wetL)

            var wetR = if (channels >= 2) {
                var r = hpfFilter.processRight(dryR)
                r = lowFilter.processRight(r)
                r = mudFilter.processRight(r)
                r = presFilter.processRight(r)
                r = deessFilter.processRight(r)
                airFilter.processRight(r)
            } else {
                wetL
            }

            // Stereo-linked envelope detection for main compressor
            val peakIn = max(abs(wetL), abs(wetR))
            compEnv = if (peakIn > compEnv) {
                compAttackCoeff * compEnv + (1.0 - compAttackCoeff) * peakIn
            } else {
                compReleaseCoeff * compEnv + (1.0 - compReleaseCoeff) * peakIn
            }

            val grDb = computeCompressorGainDb(compEnv, threshDb, ratio, kneeDb = 6.0)
            lastGrDb = grDb
            val compGain = 10.0.pow(grDb / 20.0)
            wetL *= compGain
            wetR *= compGain

            // Mid-Side Stereo Width (for stereo tracks)
            if (channels >= 2) {
                val mid = 0.5 * (wetL + wetR)
                val side = 0.5 * (wetL - wetR) * width
                wetL = mid + side
                wetR = mid - side
            }

            // Output Makeup Gain
            wetL *= makeupLinear
            wetR *= makeupLinear

            // -0.6 dBFS Safety Limiter (20:1 soft-knee + brickwall ceiling)
            val limPeak = max(abs(wetL), abs(wetR))
            limEnv = if (limPeak > limEnv) {
                limAttackCoeff * limEnv + (1.0 - limAttackCoeff) * limPeak
            } else {
                limReleaseCoeff * limEnv + (1.0 - limReleaseCoeff) * limPeak
            }
            val limGrDb = computeCompressorGainDb(limEnv, -0.6, 20.0, kneeDb = 0.5)
            val limGain = 10.0.pow(limGrDb / 20.0)
            wetL = (wetL * limGain).coerceIn(-brickwallCeiling, brickwallCeiling)
            wetR = (wetR * limGain).coerceIn(-brickwallCeiling, brickwallCeiling)

            if (isBypassed) {
                leftOut[i] = dryL.toFloat().coerceIn(-1f, 1f)
                rightOut[i] = dryR.toFloat().coerceIn(-1f, 1f)
            } else {
                leftOut[i] = wetL.toFloat()
                rightOut[i] = wetR.toFloat()
            }
        }

        currentGainReductionDb = if (isBypassed) 0f else lastGrDb.toFloat()
    }
}

object WavExporter {
    fun exportToWav(
        decoded: DecodedAudio,
        params: MasteringParams,
        isBypassed: Boolean,
        outputStream: OutputStream,
        onProgress: (Float) -> Unit = {}
    ) {
        val totalFrames = decoded.totalFrames
        val numChannels = decoded.channels.coerceIn(1, 2)
        val sampleRate = decoded.sampleRate
        val dataByteSize = totalFrames.toLong() * numChannels * 2L

        val chain = MasteringDspChain(sampleRate, numChannels, params)
        val blockSize = 4096
        val leftOut = FloatArray(blockSize)
        val rightOut = FloatArray(blockSize)
        val byteBuf = ByteArray(blockSize * numChannels * 2)

        BufferedOutputStream(outputStream, 65536).use { out ->
            // Write 44-byte RIFF/WAVE header
            val header = ByteArray(44)
            fun putAscii(offset: Int, text: String) {
                for (i in text.indices) header[offset + i] = text[i].code.toByte()
            }
            fun putIntLE(offset: Int, value: Int) {
                header[offset] = (value and 0xFF).toByte()
                header[offset + 1] = ((value ushr 8) and 0xFF).toByte()
                header[offset + 2] = ((value ushr 16) and 0xFF).toByte()
                header[offset + 3] = ((value ushr 24) and 0xFF).toByte()
            }
            fun putShortLE(offset: Int, value: Int) {
                header[offset] = (value and 0xFF).toByte()
                header[offset + 1] = ((value ushr 8) and 0xFF).toByte()
            }

            val riffSize = (36L + dataByteSize).coerceAtMost(0xFFFFFFFFL).toInt()
            val dataSizeInt = dataByteSize.coerceAtMost(0xFFFFFFFFL).toInt()
            val byteRate = sampleRate * numChannels * 2
            val blockAlign = numChannels * 2

            putAscii(0, "RIFF")
            putIntLE(4, riffSize)
            putAscii(8, "WAVE")
            putAscii(12, "fmt ")
            putIntLE(16, 16) // PCM chunk size
            putShortLE(20, 1) // AudioFormat = PCM
            putShortLE(22, numChannels)
            putIntLE(24, sampleRate)
            putIntLE(28, byteRate)
            putShortLE(32, blockAlign)
            putShortLE(34, 16) // BitsPerSample
            putAscii(36, "data")
            putIntLE(40, dataSizeInt)
            out.write(header)

            var pos = 0
            while (pos < totalFrames) {
                val count = min(blockSize, totalFrames - pos)
                chain.processBlock(
                    srcChannels = decoded.channelSamples,
                    offset = pos,
                    frameCount = count,
                    leftOut = leftOut,
                    rightOut = rightOut,
                    isBypassed = isBypassed
                )

                var bIdx = 0
                for (i in 0 until count) {
                    val sL = leftOut[i].coerceIn(-1f, 1f)
                    val pcmL = (if (sL < 0f) sL * 32768f else sL * 32767f).toInt().coerceIn(-32768, 32767)
                    byteBuf[bIdx++] = (pcmL and 0xFF).toByte()
                    byteBuf[bIdx++] = ((pcmL ushr 8) and 0xFF).toByte()

                    if (numChannels >= 2) {
                        val sR = rightOut[i].coerceIn(-1f, 1f)
                        val pcmR = (if (sR < 0f) sR * 32768f else sR * 32767f).toInt().coerceIn(-32768, 32767)
                        byteBuf[bIdx++] = (pcmR and 0xFF).toByte()
                        byteBuf[bIdx++] = ((pcmR ushr 8) and 0xFF).toByte()
                    }
                }
                out.write(byteBuf, 0, bIdx)
                pos += count
                onProgress(pos.toFloat() / totalFrames.coerceAtLeast(1).toFloat())
            }
            out.flush()
        }
    }
}
