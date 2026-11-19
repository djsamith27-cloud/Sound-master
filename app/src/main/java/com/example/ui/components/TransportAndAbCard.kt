package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.AppLanguage
import com.example.audio.StudioStrings
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioBorderMuted
import com.example.ui.theme.StudioEmeraldDark
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransportAndAbCard(
    trackName: String?,
    trackStats: String,
    language: AppLanguage,
    hasAudio: Boolean,
    isPlaying: Boolean,
    isBypassed: Boolean,
    isDecoding: Boolean,
    isExporting: Boolean,
    exportProgress: Float,
    onPlayPause: () -> Unit,
    onSelectBypassed: (Boolean) -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StudioSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, StudioBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = trackName ?: StudioStrings.selectTrackPlaceholder(language),
                    style = MaterialTheme.typography.titleMedium,
                    color = StudioTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("track_name_text")
                )
                Text(
                    text = trackStats,
                    style = MaterialTheme.typography.labelMedium,
                    color = StudioTextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("track_stats_text")
                )
            }

            if (isDecoding) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(
                        color = StudioAmber,
                        trackColor = StudioBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                    Text(
                        text = StudioStrings.decodingTrack(language),
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioAmber
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onPlayPause,
                    enabled = hasAudio && !isDecoding,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioTextPrimary,
                        contentColor = StudioBg,
                        disabledContainerColor = StudioTextPrimary.copy(alpha = 0.25f),
                        disabledContentColor = StudioBg.copy(alpha = 0.6f)
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .height(42.dp)
                        .testTag("play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play_pause),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPlaying) "Pause" else "Play",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Segmented A/B Comparison Switch
                Row(
                    modifier = Modifier
                        .background(StudioBg, RoundedCornerShape(8.dp))
                        .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AbSegmentButton(
                        label = StudioStrings.btnOriginal(language),
                        selected = isBypassed,
                        activeContainer = StudioBorderMuted,
                        activeContent = Color.White,
                        onClick = { onSelectBypassed(true) },
                        testTag = "ab_original_button"
                    )
                    AbSegmentButton(
                        label = StudioStrings.btnFixed(language),
                        selected = !isBypassed,
                        activeContainer = StudioAmber,
                        activeContent = StudioBg,
                        onClick = { onSelectBypassed(false) },
                        testTag = "ab_fixed_button"
                    )
                }

                Button(
                    onClick = onExportClick,
                    enabled = hasAudio && !isDecoding && !isExporting,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioEmeraldDark,
                        contentColor = Color.White,
                        disabledContainerColor = StudioEmeraldDark.copy(alpha = 0.35f),
                        disabledContentColor = Color.White.copy(alpha = 0.55f)
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .height(42.dp)
                        .testTag("export_wav_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = stringResource(R.string.cd_export_wav),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = StudioStrings.exportBtn(language, isExporting),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            if (isExporting) {
                LinearProgressIndicator(
                    progress = { exportProgress },
                    color = StudioEmeraldDark,
                    trackColor = StudioBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Composable
private fun AbSegmentButton(
    label: String,
    selected: Boolean,
    activeContainer: Color,
    activeContent: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) activeContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (selected) activeContent else StudioTextMuted,
            maxLines = 1
        )
    }
}

@Composable
fun UploadDropBanner(
    language: AppLanguage,
    isDecoding: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StudioSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, StudioBorderMuted),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isDecoding, onClick = onClick)
            .testTag("drop_zone_banner")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.AudioFile,
                contentDescription = null,
                tint = StudioAmber,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = StudioStrings.dropTitle(language),
                style = MaterialTheme.typography.titleSmall,
                color = StudioTextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = StudioStrings.dropSubtitle(language),
                style = MaterialTheme.typography.bodySmall,
                color = StudioTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
