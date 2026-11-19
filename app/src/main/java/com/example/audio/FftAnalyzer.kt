package com.example.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure Kotlin 2048-point Radix-2 Cooley-Tukey FFT producing 40 logarithmic spectrum bars
 * matching Swara Studio's Live Spectrum analyzer.
 */
class FftAnalyzer(
    private val fftSize: Int = 2048,
    private val numBars: Int = 40
) {
    private val real = FloatArray(fftSize)
    private val imag = FloatArray(fftSize)
    private val window = FloatArray(fftSize) { i ->
        (0.5 * (1.0 - cos(2.0 * PI * i / (fftSize - 1)))).toFloat()
    }
    private val smoothedBars = FloatArray(numBars)

    fun computeSpectrumBars(leftSamples: FloatArray, rightSamples: FloatArray, validFrames: Int): List<Float> {
        if (validFrames <= 0) {
            for (i in 0 until numBars) smoothedBars[i] *= 0.8f
            return smoothedBars.toList()
        }

        val count = minOf(fftSize, validFrames)
        for (i in 0 until fftSize) {
            if (i < count) {
                val mono = 0.5f * (leftSamples[i] + rightSamples[i])
                real[i] = mono * window[i]
            } else {
                real[i] = 0f
            }
            imag[i] = 0f
        }

        fft(real, imag)

        val halfSize = fftSize / 2
        val result = FloatArray(numBars)
        for (i in 0 until numBars) {
            val idx = ((i.toDouble() / numBars).pow(1.8) * halfSize * 0.7)
                .toInt()
                .coerceIn(1, halfSize - 1)
            val re = real[idx]
            val im = imag[idx]
            val mag = sqrt(re * re + im * im) / (fftSize * 0.25f)
            val db = 20f * log10(max(1e-5f, mag))
            // Map roughly -68 dB..0 dB to 0..1
            val normalized = ((db + 68f) / 68f).coerceIn(0f, 1f)
            // Fast attack, smooth decay
            smoothedBars[i] = if (normalized > smoothedBars[i]) {
                smoothedBars[i] * 0.35f + normalized * 0.65f
            } else {
                smoothedBars[i] * 0.72f + normalized * 0.28f
            }
            result[i] = smoothedBars[i]
        }
        return result.toList()
    }

    fun reset() {
        smoothedBars.fill(0f)
    }

    private fun fft(re: FloatArray, im: FloatArray) {
        val n = re.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tmpR = re[i]; re[i] = re[j]; re[j] = tmpR
                val tmpI = im[i]; im[i] = im[j]; im[j] = tmpI
            }
        }

        var len = 2
        while (len <= n) {
            val halfLen = len shr 1
            val angle = -2.0 * PI / len
            val wLenR = cos(angle).toFloat()
            val wLenI = sin(angle).toFloat()
            var i = 0
            while (i < n) {
                var wR = 1f
                var wI = 0f
                for (k in 0 until halfLen) {
                    val uR = re[i + k]
                    val uI = im[i + k]
                    val tR = re[i + k + halfLen] * wR - im[i + k + halfLen] * wI
                    val tI = re[i + k + halfLen] * wI + im[i + k + halfLen] * wR
                    re[i + k] = uR + tR
                    im[i + k] = uI + tI
                    re[i + k + halfLen] = uR - tR
                    im[i + k + halfLen] = uI - tI
                    val nextWR = wR * wLenR - wI * wLenI
                    wI = wR * wLenI + wI * wLenR
                    wR = nextWR
                }
                i += len
            }
            len = len shl 1
        }
    }
}
