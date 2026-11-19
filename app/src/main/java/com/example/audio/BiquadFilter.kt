package com.example.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class BiquadType {
    HIGHPASS,
    LOWSHELF,
    PEAKING,
    HIGHSHELF,
    BANDPASS
}

/**
 * Double-precision Direct Form I biquad filter with independent stereo channel states.
 */
class StereoBiquadFilter(private val type: BiquadType) {
    private var b0 = 1.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var a1 = 0.0
    private var a2 = 0.0

    // Left channel state
    private var lx1 = 0.0
    private var lx2 = 0.0
    private var ly1 = 0.0
    private var ly2 = 0.0

    // Right channel state
    private var rx1 = 0.0
    private var rx2 = 0.0
    private var ry1 = 0.0
    private var ry2 = 0.0

    fun resetState() {
        lx1 = 0.0; lx2 = 0.0; ly1 = 0.0; ly2 = 0.0
        rx1 = 0.0; rx2 = 0.0; ry1 = 0.0; ry2 = 0.0
    }

    fun configure(sampleRate: Int, freqHz: Double, q: Double = 0.70710678, gainDb: Double = 0.0) {
        val sr = sampleRate.toDouble().coerceAtLeast(8000.0)
        val clampedFreq = freqHz.coerceIn(10.0, sr * 0.45)
        val w0 = (2.0 * PI * clampedFreq) / sr
        val cosW = cos(w0)
        val sinW = sin(w0)

        var rawB0 = 1.0
        var rawB1 = 0.0
        var rawB2 = 0.0
        var rawA0 = 1.0
        var rawA1 = 0.0
        var rawA2 = 0.0

        when (type) {
            BiquadType.HIGHPASS -> {
                val alpha = sinW / (2.0 * q)
                rawB0 = (1.0 + cosW) / 2.0
                rawB1 = -(1.0 + cosW)
                rawB2 = (1.0 + cosW) / 2.0
                rawA0 = 1.0 + alpha
                rawA1 = -2.0 * cosW
                rawA2 = 1.0 - alpha
            }

            BiquadType.PEAKING -> {
                val a = 10.0.pow(gainDb / 40.0)
                val alpha = sinW / (2.0 * q)
                rawB0 = 1.0 + alpha * a
                rawB1 = -2.0 * cosW
                rawB2 = 1.0 - alpha * a
                rawA0 = 1.0 + alpha / a
                rawA1 = -2.0 * cosW
                rawA2 = 1.0 - alpha / a
            }

            BiquadType.LOWSHELF -> {
                val a = 10.0.pow(gainDb / 40.0)
                val alpha = sinW / sqrt(2.0) // S = 1.0 (WebAudio default)
                val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha
                rawB0 = a * ((a + 1.0) - (a - 1.0) * cosW + twoSqrtAAlpha)
                rawB1 = 2.0 * a * ((a - 1.0) - (a + 1.0) * cosW)
                rawB2 = a * ((a + 1.0) - (a - 1.0) * cosW - twoSqrtAAlpha)
                rawA0 = (a + 1.0) + (a - 1.0) * cosW + twoSqrtAAlpha
                rawA1 = -2.0 * ((a - 1.0) + (a + 1.0) * cosW)
                rawA2 = (a + 1.0) + (a - 1.0) * cosW - twoSqrtAAlpha
            }

            BiquadType.HIGHSHELF -> {
                val a = 10.0.pow(gainDb / 40.0)
                val alpha = sinW / sqrt(2.0) // S = 1.0
                val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha
                rawB0 = a * ((a + 1.0) + (a - 1.0) * cosW + twoSqrtAAlpha)
                rawB1 = -2.0 * a * ((a - 1.0) + (a + 1.0) * cosW)
                rawB2 = a * ((a + 1.0) + (a - 1.0) * cosW - twoSqrtAAlpha)
                rawA0 = (a + 1.0) - (a - 1.0) * cosW + twoSqrtAAlpha
                rawA1 = 2.0 * ((a - 1.0) - (a + 1.0) * cosW)
                rawA2 = (a + 1.0) - (a - 1.0) * cosW - twoSqrtAAlpha
            }

            BiquadType.BANDPASS -> {
                val alpha = sinW / (2.0 * q)
                rawB0 = alpha
                rawB1 = 0.0
                rawB2 = -alpha
                rawA0 = 1.0 + alpha
                rawA1 = -2.0 * cosW
                rawA2 = 1.0 - alpha
            }
        }

        b0 = rawB0 / rawA0
        b1 = rawB1 / rawA0
        b2 = rawB2 / rawA0
        a1 = rawA1 / rawA0
        a2 = rawA2 / rawA0
    }

    fun processLeft(x0: Double): Double {
        val y0 = b0 * x0 + b1 * lx1 + b2 * lx2 - a1 * ly1 - a2 * ly2
        lx2 = lx1; lx1 = x0
        ly2 = ly1; ly1 = y0
        return y0
    }

    fun processRight(x0: Double): Double {
        val y0 = b0 * x0 + b1 * rx1 + b2 * rx2 - a1 * ry1 - a2 * ry2
        rx2 = rx1; rx1 = x0
        ry2 = ry1; ry1 = y0
        return y0
    }
}
