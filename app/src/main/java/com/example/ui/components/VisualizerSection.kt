package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.AppLanguage
import com.example.audio.StudioStrings
import com.example.ui.formatTime
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioBorderMuted
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import java.util.Locale
import kotlin.math.max

@Composable
fun WaveformCard(
    language: AppLanguage,
    peaks: List<Float>,
    currentSec: Float,
    durationSec: Float,
    isBypassed: Boolean,
    hasAudio: Boolean,
    onSeekFraction: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeText = "${formatTime(currentSec)} / ${formatTime(durationSec)}"
    val waveformDesc = androidx.compose.ui.res.stringResource(R.string.cd_waveform)

    Surface(
        color = StudioSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, StudioBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = StudioStrings.waveformLabel(language),
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioTextMuted
                )
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelMedium,
                    color = StudioTextPrimary,
                    modifier = Modifier.testTag("time_display_text")
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioBg)
                    .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                    .semantics { contentDescription = waveformDesc }
                    .testTag("waveform_canvas")
                    .pointerInput(hasAudio) {
                        if (!hasAudio) return@pointerInput
                        detectTapGestures { offset ->
                            val frac = (offset.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                            onSeekFraction(frac)
                        }
                    }
                    .pointerInput(hasAudio) {
                        if (!hasAudio) return@pointerInput
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            val frac = (change.position.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                            onSeekFraction(frac)
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (peaks.isEmpty() || durationSec <= 0f) return@Canvas
                    val pct = (currentSec / durationSec).coerceIn(0f, 1f)
                    val bw = size.width / peaks.size.toFloat()
                    val playedColor = if (isBypassed) StudioTextMuted else StudioAmber
                    val unplayedColor = StudioBorderMuted

                    peaks.forEachIndexed { idx, amp ->
                        val barH = max(4f, amp * (size.height - 14f))
                        val barColor = if (idx.toFloat() / peaks.size.toFloat() <= pct) {
                            playedColor
                        } else {
                            unplayedColor
                        }
                        drawRect(
                            color = barColor,
                            topLeft = Offset(idx * bw + 1f, (size.height - barH) / 2f),
                            size = Size(max(1f, bw - 2f), barH)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpectrumCard(
    spectrumBars: List<Float>,
    gainReductionDb: Float,
    isPlaying: Boolean,
    isBypassed: Boolean,
    modifier: Modifier = Modifier
) {
    val grDisplay = if (isBypassed || !isPlaying) 0.0f else gainReductionDb
    val grText = "GR: ${String.format(Locale.US, "%.1f", grDisplay)} dB"
    val spectrumDesc = androidx.compose.ui.res.stringResource(R.string.cd_spectrum)

    Surface(
        color = StudioSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, StudioBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = StudioStrings.spectrumLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioTextMuted
                )
                Text(
                    text = grText,
                    style = MaterialTheme.typography.labelMedium,
                    color = StudioAmber,
                    modifier = Modifier.testTag("gr_display_text")
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioBg)
                    .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                    .semantics { contentDescription = spectrumDesc }
                    .testTag("spectrum_canvas")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (!isPlaying || spectrumBars.isEmpty()) return@Canvas
                    val bins = spectrumBars.size
                    val bw = size.width / bins.toFloat()
                    val barColor = if (isBypassed) {
                        StudioTextSecondary.copy(alpha = 0.6f)
                    } else {
                        StudioAmber.copy(alpha = 0.85f)
                    }

                    spectrumBars.forEachIndexed { idx, level ->
                        val h = max(2f, level.coerceIn(0f, 1f) * (size.height - 8f))
                        drawRect(
                            color = barColor,
                            topLeft = Offset(idx * bw + 1f, size.height - h),
                            size = Size(max(2f, bw - 2f), h)
                        )
                    }
                }
            }
        }
    }
}
