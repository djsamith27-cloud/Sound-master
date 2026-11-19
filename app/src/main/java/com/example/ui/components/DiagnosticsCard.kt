package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.AppLanguage
import com.example.audio.DecodedAudio
import com.example.audio.StudioStrings
import com.example.ui.theme.StudioAmberSoft
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary

@Composable
fun DiagnosticsCard(
    language: AppLanguage,
    decodedAudio: DecodedAudio?,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StudioSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, StudioBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("diagnostics_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = StudioStrings.auditHeading(language),
                style = MaterialTheme.typography.titleMedium,
                color = StudioTextPrimary
            )
            HorizontalDivider(color = StudioBorder, thickness = 1.dp)

            if (decodedAudio == null) {
                Text(
                    text = StudioStrings.auditEmptyPrompt(language),
                    style = MaterialTheme.typography.bodyMedium,
                    color = StudioTextSecondary
                )
            } else {
                Text(
                    text = StudioStrings.auditSummary(language, decodedAudio.peakDb, decodedAudio.rmsDb),
                    style = MaterialTheme.typography.bodyMedium,
                    color = StudioTextSecondary,
                    modifier = Modifier.testTag("diagnostics_summary_text")
                )

                if (decodedAudio.issues.isEmpty()) {
                    Text(
                        text = StudioStrings.auditHealthy(language),
                        style = MaterialTheme.typography.bodyMedium,
                        color = StudioEmerald,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        decodedAudio.issues.forEachIndexed { index, issue ->
                            if (index > 0) {
                                HorizontalDivider(color = StudioBorder, thickness = 1.dp)
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (language == AppLanguage.SINHALA) issue.titleSi else issue.titleEn,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = StudioAmberSoft
                                )
                                Text(
                                    text = if (language == AppLanguage.SINHALA) issue.detailSi else issue.detailEn,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StudioTextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
