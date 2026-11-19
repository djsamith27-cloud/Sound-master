package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.audio.MasteringParams
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAmberLight
import com.example.ui.theme.StudioBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ParametricEqCard(
    params: MasteringParams,
    onUpdateParams: ((MasteringParams) -> MasteringParams) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StudioSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, StudioBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Parametric Repair EQ",
                style = MaterialTheme.typography.titleMedium,
                color = StudioTextPrimary
            )
            HorizontalDivider(color = StudioBorder, thickness = 1.dp)

            StudioSliderRow(
                label = "Sub-Tight High-Pass",
                valueText = "${params.hpf.roundToInt()} Hz",
                value = params.hpf,
                range = 20f..90f,
                step = 2f,
                testTag = "slider_hpf",
                onValueChange = { v -> onUpdateParams { it.copy(hpf = v) } }
            )
            StudioSliderRow(
                label = "808 / Bass Warmth (90 Hz)",
                valueText = formatSignedDb(params.low),
                value = params.low,
                range = -6f..6f,
                step = 0.2f,
                testTag = "slider_low",
                onValueChange = { v -> onUpdateParams { it.copy(low = v) } }
            )
            StudioSliderRow(
                label = "Mud Cut (250 Hz)",
                valueText = formatSignedDb(params.mud),
                value = params.mud,
                range = -6f..4f,
                step = 0.2f,
                testTag = "slider_mud",
                onValueChange = { v -> onUpdateParams { it.copy(mud = v) } }
            )
            StudioSliderRow(
                label = "Vocal Clarity (3.2 kHz)",
                valueText = formatSignedDb(params.pres),
                value = params.pres,
                range = -4f..6f,
                step = 0.2f,
                testTag = "slider_pres",
                onValueChange = { v -> onUpdateParams { it.copy(pres = v) } }
            )
            StudioSliderRow(
                label = "De-Esser / Hi-Hat (6.8 kHz)",
                valueText = formatSignedDb(params.deess),
                value = params.deess,
                range = -6f..3f,
                step = 0.2f,
                testTag = "slider_deess",
                onValueChange = { v -> onUpdateParams { it.copy(deess = v) } }
            )
            StudioSliderRow(
                label = "Studio Air (11 kHz)",
                valueText = formatSignedDb(params.air),
                value = params.air,
                range = -4f..6f,
                step = 0.2f,
                testTag = "slider_air",
                onValueChange = { v -> onUpdateParams { it.copy(air = v) } }
            )
        }
    }
}

@Composable
fun DynamicsAndLimiterCard(
    params: MasteringParams,
    onUpdateParams: ((MasteringParams) -> MasteringParams) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StudioSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, StudioBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Dynamics, Width & Limiter",
                style = MaterialTheme.typography.titleMedium,
                color = StudioTextPrimary
            )
            HorizontalDivider(color = StudioBorder, thickness = 1.dp)

            StudioSliderRow(
                label = "Compressor Threshold",
                valueText = "${params.thresh.roundToInt()} dB",
                value = params.thresh,
                range = -30f..0f,
                step = 1f,
                testTag = "slider_thresh",
                onValueChange = { v -> onUpdateParams { it.copy(thresh = v) } }
            )
            StudioSliderRow(
                label = "Compression Ratio",
                valueText = "${String.format(Locale.US, "%.1f", params.ratio)} : 1",
                value = params.ratio,
                range = 1f..8f,
                step = 0.1f,
                testTag = "slider_ratio",
                onValueChange = { v -> onUpdateParams { it.copy(ratio = v) } }
            )
            StudioSliderRow(
                label = "Stereo Width (Mid-Side)",
                valueText = "${String.format(Locale.US, "%.2f", params.width)} x",
                value = params.width,
                range = 0.6f..1.45f,
                step = 0.02f,
                testTag = "slider_width",
                onValueChange = { v -> onUpdateParams { it.copy(width = v) } }
            )
            StudioSliderRow(
                label = "Output Makeup Gain",
                valueText = formatSignedDb(params.gain),
                value = params.gain,
                range = -6f..6f,
                step = 0.2f,
                testTag = "slider_gain",
                onValueChange = { v -> onUpdateParams { it.copy(gain = v) } }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StudioBg, RoundedCornerShape(8.dp))
                    .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Safety Limiter Ceiling",
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioTextMuted
                )
                Text(
                    text = "-0.6 dBFS",
                    style = MaterialTheme.typography.labelMedium,
                    color = StudioEmerald
                )
            }
        }
    }
}

@Composable
private fun StudioSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    testTag: String,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = StudioTextSecondary
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                color = StudioAmberLight
            )
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = { raw ->
                val snapped = ((raw / step).roundToInt() * step)
                    .coerceIn(range.start, range.endInclusive)
                onValueChange(snapped)
            },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = StudioAmber,
                activeTrackColor = StudioAmber,
                inactiveTrackColor = StudioBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}

private fun formatSignedDb(value: Float): String {
    val prefix = if (value >= 0f) "+" else ""
    return "$prefix${String.format(Locale.US, "%.1f", value)} dB"
}
