package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.audio.MasteringParams
import com.example.data.SongDocument
import com.example.ui.components.DiagnosticsCard
import com.example.ui.components.DynamicsAndLimiterCard
import com.example.ui.components.LyricsDoctorCard
import com.example.ui.components.ParametricEqCard
import com.example.ui.components.PresetBar
import com.example.ui.components.SpectrumCard
import com.example.ui.components.StudioHeader
import com.example.ui.components.TransportAndAbCard
import com.example.ui.components.UploadDropBanner
import com.example.ui.components.WaveformCard
import com.example.ui.theme.StudioBg

@Composable
fun SwaraStudioScreen(
    uiState: StudioUiState,
    savedSongsState: SavedSongsUiState,
    snackbarHostState: SnackbarHostState,
    onToggleLanguage: () -> Unit,
    onPickAudioClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onSelectBypassed: (Boolean) -> Unit,
    onExportWavClick: () -> Unit,
    onSeekFraction: (Float) -> Unit,
    onSelectPreset: (String) -> Unit,
    onUpdateParams: ((MasteringParams) -> MasteringParams) -> Unit,
    onLyricsDraftChange: (String) -> Unit,
    onFixLyricsClick: () -> Unit,
    onExtractAndFixFromAudioClick: () -> Unit,
    onSaveLyricsToSongClick: (String?) -> Unit,
    onApplyCorrectedLyrics: (String) -> Unit,
    onSelectSavedSong: (SongDocument) -> Unit,
    onClearLyrics: () -> Unit,
    onCopiedNotice: () -> Unit
) {
    Scaffold(
        containerColor = StudioBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            StudioHeader(
                language = uiState.language,
                isDecoding = uiState.isDecoding,
                onToggleLanguage = onToggleLanguage,
                onUploadClick = onPickAudioClick,
                onSignOutClick = onSignOutClick
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth >= 840.dp
            val scrollState = rememberScrollState()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 1280.dp)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TransportAndAbCard(
                        trackName = uiState.decodedAudio?.fileName,
                        trackStats = uiState.trackStatsText,
                        language = uiState.language,
                        hasAudio = uiState.hasAudio,
                        isPlaying = uiState.telemetry.isPlaying,
                        isBypassed = uiState.isBypassed,
                        isDecoding = uiState.isDecoding,
                        isExporting = uiState.isExporting,
                        exportProgress = uiState.exportProgress,
                        onPlayPause = onPlayPauseClick,
                        onSelectBypassed = onSelectBypassed,
                        onExportClick = onExportWavClick
                    )

                    AnimatedVisibility(visible = !uiState.hasAudio) {
                        UploadDropBanner(
                            language = uiState.language,
                            isDecoding = uiState.isDecoding,
                            onClick = onPickAudioClick
                        )
                    }

                    if (isWideScreen) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            WaveformCard(
                                language = uiState.language,
                                peaks = uiState.decodedAudio?.peaks.orEmpty(),
                                currentSec = uiState.telemetry.currentSec,
                                durationSec = uiState.decodedAudio?.durationSec ?: 0f,
                                isBypassed = uiState.isBypassed,
                                hasAudio = uiState.hasAudio,
                                onSeekFraction = onSeekFraction,
                                modifier = Modifier.weight(8f)
                            )
                            SpectrumCard(
                                spectrumBars = uiState.telemetry.spectrumBars,
                                gainReductionDb = uiState.telemetry.gainReductionDb,
                                isPlaying = uiState.telemetry.isPlaying,
                                isBypassed = uiState.isBypassed,
                                modifier = Modifier.weight(4f)
                            )
                        }
                    } else {
                        WaveformCard(
                            language = uiState.language,
                            peaks = uiState.decodedAudio?.peaks.orEmpty(),
                            currentSec = uiState.telemetry.currentSec,
                            durationSec = uiState.decodedAudio?.durationSec ?: 0f,
                            isBypassed = uiState.isBypassed,
                            hasAudio = uiState.hasAudio,
                            onSeekFraction = onSeekFraction
                        )
                        SpectrumCard(
                            spectrumBars = uiState.telemetry.spectrumBars,
                            gainReductionDb = uiState.telemetry.gainReductionDb,
                            isPlaying = uiState.telemetry.isPlaying,
                            isBypassed = uiState.isBypassed
                        )
                    }

                    PresetBar(
                        activePresetId = uiState.activePresetId,
                        onSelectPreset = onSelectPreset
                    )

                    if (isWideScreen) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            ParametricEqCard(
                                params = uiState.params,
                                onUpdateParams = onUpdateParams,
                                modifier = Modifier.weight(7f)
                            )
                            DynamicsAndLimiterCard(
                                params = uiState.params,
                                onUpdateParams = onUpdateParams,
                                modifier = Modifier.weight(5f)
                            )
                        }
                    } else {
                        ParametricEqCard(
                            params = uiState.params,
                            onUpdateParams = onUpdateParams
                        )
                        DynamicsAndLimiterCard(
                            params = uiState.params,
                            onUpdateParams = onUpdateParams
                        )
                    }

                    DiagnosticsCard(
                        language = uiState.language,
                        decodedAudio = uiState.decodedAudio
                    )

                    LyricsDoctorCard(
                        language = uiState.language,
                        currentSongId = uiState.currentSongId,
                        currentSongTitle = uiState.currentSongTitle,
                        lyricsDraft = uiState.lyricsDraft,
                        hasAudio = uiState.hasAudio,
                        doctorState = uiState.lyricsDoctorState,
                        isSavingLyrics = uiState.isSavingLyrics,
                        lastSavedSongId = uiState.lastSavedSongId,
                        saveLyricsError = uiState.saveLyricsError,
                        savedSongsState = savedSongsState,
                        onLyricsDraftChange = onLyricsDraftChange,
                        onFixLyricsClick = onFixLyricsClick,
                        onExtractAndFixFromAudioClick = onExtractAndFixFromAudioClick,
                        onSaveLyricsToSongClick = onSaveLyricsToSongClick,
                        onApplyCorrectedLyrics = onApplyCorrectedLyrics,
                        onSelectSavedSong = onSelectSavedSong,
                        onClearLyrics = onClearLyrics,
                        onCopiedNotice = onCopiedNotice
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
