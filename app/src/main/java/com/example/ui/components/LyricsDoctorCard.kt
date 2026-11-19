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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ai.LyricsDoctorUiState
import com.example.audio.AppLanguage
import com.example.data.SongDocument
import com.example.ui.SavedSongsUiState
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAmberSoft
import com.example.ui.theme.StudioBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioBorderMuted
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioEmeraldDark
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LyricsDoctorCard(
    language: AppLanguage,
    currentSongId: String,
    currentSongTitle: String,
    lyricsDraft: String,
    hasAudio: Boolean,
    doctorState: LyricsDoctorUiState,
    isSavingLyrics: Boolean,
    lastSavedSongId: String?,
    saveLyricsError: String?,
    savedSongsState: SavedSongsUiState,
    onLyricsDraftChange: (String) -> Unit,
    onFixLyricsClick: () -> Unit,
    onExtractAndFixFromAudioClick: () -> Unit,
    onSaveLyricsToSongClick: (String?) -> Unit,
    onApplyCorrectedLyrics: (String) -> Unit,
    onSelectSavedSong: (SongDocument) -> Unit,
    onClearLyrics: () -> Unit,
    onCopiedNotice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val isLoading = doctorState is LyricsDoctorUiState.Loading
    val isSi = language == AppLanguage.SINHALA

    Surface(
        color = StudioSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, StudioBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("lyrics_doctor_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isSi) "Lyrics Doctor (වැරදි හදන කොටස)" else "Lyrics Doctor (Lyric Error Fixer)",
                        style = MaterialTheme.typography.titleMedium,
                        color = StudioTextPrimary
                    )
                    Text(
                        text = if (isSi) {
                            "වත්මන් ගීතය: $currentSongTitle"
                        } else {
                            "Song Document: $currentSongTitle"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = StudioTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .background(StudioAmber.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
                        .border(1.dp, StudioAmber.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (lastSavedSongId == currentSongId) "Saved" else "AI + Cloud",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (lastSavedSongId == currentSongId) StudioEmerald else StudioAmber
                    )
                }
            }

            HorizontalDivider(color = StudioBorder, thickness = 1.dp)

            Text(
                text = if (isSi) {
                    "ගීතයේ පද රචනය (Sinhala / English) මෙහි Paste හෝ Type කර වැරදි හදන්න — නැතහොත් ගීතයෙන්ම Lyrics ගෙන නිවැරදි කර Firestore ගීත ගොනුවේ Save කරන්න."
                } else {
                    "Paste or type lyrics to fix spelling, grammar, and rhythm — or extract from the loaded song and save modified lyrics directly to the current Firestore song document."
                },
                style = MaterialTheme.typography.bodySmall,
                color = StudioTextMuted
            )

            OutlinedTextField(
                value = lyricsDraft,
                onValueChange = onLyricsDraftChange,
                enabled = !isLoading && !isSavingLyrics,
                minLines = 4,
                maxLines = 10,
                placeholder = {
                    Text(
                        text = if (isSi) {
                            "මෙහි ගීතයේ lyrics type හෝ paste කරන්න..."
                        } else {
                            "Type or paste your song lyrics here..."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = StudioTextMuted
                    )
                },
                trailingIcon = {
                    if (lyricsDraft.isNotEmpty() && !isLoading && !isSavingLyrics) {
                        IconButton(
                            onClick = onClearLyrics,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("clear_lyrics_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = if (isSi) "Lyrics මකන්න" else "Clear lyrics",
                                tint = StudioTextMuted
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = StudioBg,
                    unfocusedContainerColor = StudioBg,
                    disabledContainerColor = StudioBg.copy(alpha = 0.7f),
                    focusedBorderColor = StudioAmber,
                    unfocusedBorderColor = StudioBorderMuted,
                    focusedTextColor = StudioTextPrimary,
                    unfocusedTextColor = StudioTextPrimary,
                    cursorColor = StudioAmber
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lyrics_input_field")
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onFixLyricsClick,
                    enabled = !isLoading && !isSavingLyrics && lyricsDraft.isNotBlank(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioAmber,
                        contentColor = StudioBg,
                        disabledContainerColor = StudioAmber.copy(alpha = 0.25f),
                        disabledContentColor = StudioBg.copy(alpha = 0.6f)
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .height(42.dp)
                        .testTag("fix_lyrics_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoFixHigh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSi) "Lyrics වැරදි හදන්න" else "Fix Lyrics Mistakes",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                OutlinedButton(
                    onClick = onExtractAndFixFromAudioClick,
                    enabled = !isLoading && !isSavingLyrics && hasAudio,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (!isLoading && !isSavingLyrics && hasAudio) StudioAmber else StudioBorder
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = StudioBg,
                        contentColor = StudioAmber,
                        disabledContentColor = StudioTextMuted.copy(alpha = 0.5f)
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .height(42.dp)
                        .testTag("fix_lyrics_from_audio_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSi) "ගීතයෙන් Lyrics ගෙන හදන්න" else "Extract & Fix from Song",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Button(
                    onClick = { onSaveLyricsToSongClick(null) },
                    enabled = !isLoading && !isSavingLyrics && lyricsDraft.isNotBlank(),
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
                        .testTag("save_lyrics_firestore_button")
                ) {
                    if (isSavingLyrics) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (lastSavedSongId == currentSongId) {
                                Icons.Filled.CloudDone
                            } else {
                                Icons.Filled.CloudUpload
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSavingLyrics) {
                            if (isSi) "සුරකිමින්..." else "Saving..."
                        } else {
                            if (isSi) "ගීතයට Save කරන්න" else "Save Lyrics to Song"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            if (!saveLyricsError.isNullOrBlank()) {
                Surface(
                    color = Color(0xFF3F1D1D),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF991B1B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_lyrics_error_banner")
                ) {
                    Text(
                        text = saveLyricsError,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFCA5A5),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (isLoading) {
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
                        text = if (isSi) {
                            "Lyrics පරීක්ෂා කර වැරදි නිවැරදි කරමින් පවතී..."
                        } else {
                            "Checking lyrics and fixing mistakes..."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioAmber
                    )
                }
            }

            when (doctorState) {
                is LyricsDoctorUiState.Error -> {
                    Surface(
                        color = Color(0xFF3F1D1D),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF991B1B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lyrics_error_banner")
                    ) {
                        Text(
                            text = doctorState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFCA5A5),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                is LyricsDoctorUiState.Success -> {
                    val result = doctorState.result

                    if (result.summary.isNotBlank()) {
                        Surface(
                            color = StudioBg,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, StudioBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = result.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = StudioTextSecondary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    if (result.corrections.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isSi) {
                                    "නිවැරදි කළ වැරදි (${result.corrections.size})"
                                } else {
                                    "Corrections Made (${result.corrections.size})"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = StudioAmberSoft
                            )

                            result.corrections.forEach { item ->
                                Surface(
                                    color = StudioSurfaceElevated,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, StudioBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (item.originalLine.isNotBlank()) {
                                            Text(
                                                text = item.originalLine,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    textDecoration = TextDecoration.LineThrough
                                                ),
                                                color = Color(0xFFFCA5A5)
                                            )
                                        }
                                        if (item.fixedLine.isNotBlank()) {
                                            Text(
                                                text = item.fixedLine,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                color = StudioEmerald
                                            )
                                        }
                                        if (item.reason.isNotBlank()) {
                                            Text(
                                                text = item.reason,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = StudioTextMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (result.correctedLyrics.isNotBlank()) {
                        Surface(
                            color = StudioBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, StudioEmeraldDark.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("corrected_lyrics_box")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = if (isSi) "නිවැරදි කළ සම්පූර්ණ Lyrics" else "Corrected Lyrics",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = StudioEmerald
                                )

                                Text(
                                    text = result.correctedLyrics,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = StudioTextPrimary
                                )

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(result.correctedLyrics))
                                            onCopiedNotice()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, StudioBorderMuted),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .height(38.dp)
                                            .testTag("copy_lyrics_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ContentCopy,
                                            contentDescription = null,
                                            tint = StudioTextPrimary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isSi) "Copy කරන්න" else "Copy Lyrics",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = StudioTextPrimary
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { onApplyCorrectedLyrics(result.correctedLyrics) },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.6f)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .height(38.dp)
                                            .testTag("apply_lyrics_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = StudioAmber,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isSi) "Editor එකට දාන්න" else "Apply to Editor",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = StudioAmber
                                        )
                                    }

                                    Button(
                                        onClick = { onSaveLyricsToSongClick(result.correctedLyrics) },
                                        enabled = !isSavingLyrics,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = StudioEmeraldDark,
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .height(38.dp)
                                            .testTag("save_corrected_lyrics_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CloudUpload,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isSi) "නිවැරදි Lyrics Save කරන්න" else "Save Corrected to Song",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                else -> Unit
            }

            // Saved Cloud Song Documents
            when (savedSongsState) {
                is SavedSongsUiState.Error -> {
                    Text(
                        text = savedSongsState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFCA5A5)
                    )
                }

                is SavedSongsUiState.Success -> {
                    if (savedSongsState.songs.isNotEmpty()) {
                        HorizontalDivider(color = StudioBorder, thickness = 1.dp)
                        Text(
                            text = if (isSi) {
                                "Cloud හි Save කළ ගීත (${savedSongsState.songs.size})"
                            } else {
                                "Saved Cloud Song Documents (${savedSongsState.songs.size})"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = StudioTextSecondary
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            savedSongsState.songs.take(5).forEach { song ->
                                val isCurrent = song.id == currentSongId
                                Surface(
                                    color = if (isCurrent) StudioSurfaceElevated else StudioBg,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isCurrent) StudioAmber.copy(alpha = 0.7f) else StudioBorder
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onSelectSavedSong(song) }
                                        .testTag("saved_song_item_${song.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.MusicNote,
                                            contentDescription = null,
                                            tint = if (isCurrent) StudioAmber else StudioTextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = song.title.ifBlank { song.id },
                                                style = MaterialTheme.typography.labelLarge,
                                                color = StudioTextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = song.lyrics.replace('\n', ' ').take(90),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = StudioTextMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                SavedSongsUiState.Loading -> Unit
            }
        }
    }
}
