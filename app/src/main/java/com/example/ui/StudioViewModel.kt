package com.example.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.LyricsDoctorRepository
import com.example.ai.LyricsDoctorUiState
import com.example.audio.AppLanguage
import com.example.audio.AudioDecoder
import com.example.audio.AudioPlaybackEngine
import com.example.audio.DecodedAudio
import com.example.audio.MASTERING_PRESETS
import com.example.audio.MasteringParams
import com.example.audio.PlaybackTelemetry
import com.example.audio.StudioStrings
import com.example.audio.WavExporter
import com.example.data.SongDocument
import com.example.data.SongRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

sealed interface SavedSongsUiState {
    data object Loading : SavedSongsUiState
    data class Success(val songs: List<SongDocument>) : SavedSongsUiState
    data class Error(val message: String) : SavedSongsUiState
}

data class StudioUiState(
    val language: AppLanguage = AppLanguage.SINHALA,
    val params: MasteringParams = MASTERING_PRESETS.first().params,
    val activePresetId: String? = MASTERING_PRESETS.first().id,
    val isBypassed: Boolean = false,
    val decodedAudio: DecodedAudio? = null,
    val telemetry: PlaybackTelemetry = PlaybackTelemetry(),
    val isDecoding: Boolean = false,
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val currentSongId: String = "untitled_song",
    val currentSongTitle: String = "Untitled Song",
    val lyricsDraft: String = "",
    val lyricsDoctorState: LyricsDoctorUiState = LyricsDoctorUiState.Idle,
    val isSavingLyrics: Boolean = false,
    val lastSavedSongId: String? = null,
    val saveLyricsError: String? = null
) {
    val hasAudio: Boolean get() = decodedAudio != null

    val trackStatsText: String
        get() {
            val track = decodedAudio
                ?: return "--:-- · -- kHz · Peak -- dBFS · RMS -- dBFS"
            val dur = formatTime(track.durationSec)
            val khz = String.format(Locale.US, "%.1f", track.sampleRate / 1000f)
            val peak = String.format(Locale.US, "%.1f", track.peakDb)
            val rms = String.format(Locale.US, "%.1f", track.rmsDb)
            return "$dur · $khz kHz · Peak $peak dBFS · RMS $rms dBFS"
        }
}

fun formatTime(seconds: Float): String {
    val totalSec = seconds.toInt().coerceAtLeast(0)
    val mins = totalSec / 60
    val secs = totalSec % 60
    return "$mins:${secs.toString().padStart(2, '0')}"
}

class StudioViewModel(
    private val songRepository: SongRepository,
    private val lyricsRepository: LyricsDoctorRepository,
    private val appContext: Context,
    private val currentUserId: String
) : ViewModel() {
    private val playbackEngine = AudioPlaybackEngine()

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    val savedSongsState: StateFlow<SavedSongsUiState> = songRepository.observeSongs(currentUserId)
        .map<List<SongDocument>, SavedSongsUiState> { SavedSongsUiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing saved songs", rootError)
            emit(SavedSongsUiState.Error(rootError.message ?: "Failed to load saved songs"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = SavedSongsUiState.Loading
        )

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        viewModelScope.launch {
            playbackEngine.telemetry.collect { telemetry ->
                _uiState.update { it.copy(telemetry = telemetry) }
            }
        }
    }

    fun toggleLanguage() {
        _uiState.update { state ->
            val next = if (state.language == AppLanguage.SINHALA) {
                AppLanguage.ENGLISH
            } else {
                AppLanguage.SINHALA
            }
            state.copy(language = next)
        }
    }

    fun selectPreset(presetId: String) {
        val preset = MASTERING_PRESETS.find { it.id == presetId } ?: return
        playbackEngine.updateParams(preset.params)
        _uiState.update {
            it.copy(params = preset.params, activePresetId = preset.id)
        }
    }

    fun updateParams(transform: (MasteringParams) -> MasteringParams) {
        val updated = transform(_uiState.value.params)
        val matchedPreset = MASTERING_PRESETS.find { it.params == updated }?.id
        playbackEngine.updateParams(updated)
        _uiState.update {
            it.copy(params = updated, activePresetId = matchedPreset)
        }
    }

    fun setBypassed(bypassed: Boolean) {
        playbackEngine.setBypassed(bypassed)
        _uiState.update { it.copy(isBypassed = bypassed) }
    }

    fun togglePlayPause() {
        if (!_uiState.value.hasAudio) return
        playbackEngine.togglePlayPause()
    }

    fun seekFraction(fraction: Float) {
        if (!_uiState.value.hasAudio) return
        playbackEngine.seekFraction(fraction)
    }

    fun onLyricsDraftChange(text: String) {
        _uiState.update { it.copy(lyricsDraft = text, saveLyricsError = null) }
    }

    fun clearLyrics() {
        _uiState.update {
            it.copy(
                lyricsDraft = "",
                lyricsDoctorState = LyricsDoctorUiState.Idle,
                saveLyricsError = null
            )
        }
    }

    fun applyCorrectedLyrics(corrected: String) {
        _uiState.update { it.copy(lyricsDraft = corrected, saveLyricsError = null) }
    }

    fun selectSavedSong(song: SongDocument) {
        _uiState.update {
            it.copy(
                currentSongId = song.id.ifBlank { "untitled_song" },
                currentSongTitle = song.title.ifBlank { "Untitled Song" },
                lyricsDraft = song.lyrics,
                lyricsDoctorState = LyricsDoctorUiState.Idle,
                lastSavedSongId = song.id,
                saveLyricsError = null
            )
        }
    }

    fun notifyLyricsCopied() {
        val msg = if (_uiState.value.language == AppLanguage.SINHALA) {
            "නිවැරදි කළ Lyrics Copy කරන ලදී!"
        } else {
            "Corrected lyrics copied to clipboard!"
        }
        _messages.tryEmit(msg)
    }

    fun fixLyricsText() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(lyricsDoctorState = LyricsDoctorUiState.Loading) }
            try {
                val result = lyricsRepository.fixLyricsText(state.lyricsDraft, state.language)
                _uiState.update { it.copy(lyricsDoctorState = LyricsDoctorUiState.Success(result)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        lyricsDoctorState = LyricsDoctorUiState.Error(
                            e.localizedMessage ?: "Failed to fix lyrics."
                        )
                    )
                }
            }
        }
    }

    fun extractAndFixLyricsFromAudio() {
        val state = _uiState.value
        val track = state.decodedAudio ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(lyricsDoctorState = LyricsDoctorUiState.Loading) }
            try {
                val result = lyricsRepository.extractAndFixFromAudio(
                    decodedAudio = track,
                    optionalDraftLyrics = state.lyricsDraft,
                    language = state.language
                )
                _uiState.update {
                    it.copy(
                        lyricsDoctorState = LyricsDoctorUiState.Success(result),
                        lyricsDraft = it.lyricsDraft.ifBlank { result.correctedLyrics }
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        lyricsDoctorState = LyricsDoctorUiState.Error(
                            e.localizedMessage ?: "Failed to analyze song audio for lyrics."
                        )
                    )
                }
            }
        }
    }

    fun saveModifiedLyricsToCurrentSong(lyricsOverride: String? = null) {
        val state = _uiState.value
        val lyricsToSave = (lyricsOverride ?: state.lyricsDraft).trim()
        if (lyricsToSave.isEmpty()) {
            val msg = if (state.language == AppLanguage.SINHALA) {
                "Save කිරීමට පෙර Lyrics ඇතුළත් කරන්න."
            } else {
                "Please enter or generate lyrics before saving."
            }
            _uiState.update { it.copy(saveLyricsError = msg) }
            return
        }

        val summary = (state.lyricsDoctorState as? LyricsDoctorUiState.Success)?.result?.summary.orEmpty()
        val track = state.decodedAudio
        val songId = state.currentSongId
        val title = track?.fileName?.substringBeforeLast('.')?.ifBlank { state.currentSongTitle }
            ?: state.currentSongTitle
        val fileName = track?.fileName ?: ""
        val durationSec = track?.durationSec?.toDouble() ?: 0.0
        val sampleRate = track?.sampleRate?.toLong() ?: 0L

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingLyrics = true, saveLyricsError = null) }
            val result = songRepository.saveModifiedLyricsToCurrentSong(
                userId = currentUserId,
                songId = songId,
                title = title,
                fileName = fileName,
                modifiedLyrics = lyricsToSave,
                lyricsSummary = summary,
                durationSec = durationSec,
                sampleRate = sampleRate
            )
            result.fold(
                onSuccess = { savedId ->
                    _uiState.update {
                        it.copy(
                            isSavingLyrics = false,
                            lyricsDraft = lyricsToSave,
                            currentSongId = savedId,
                            currentSongTitle = title,
                            lastSavedSongId = savedId,
                            saveLyricsError = null
                        )
                    }
                    val okMsg = if (state.language == AppLanguage.SINHALA) {
                        "Lyrics ගීතයට Firestore හි Save කරන ලදී!"
                    } else {
                        "Modified lyrics saved to current song document!"
                    }
                    _messages.tryEmit(okMsg)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSavingLyrics = false,
                            saveLyricsError = error.localizedMessage ?: "Failed to save lyrics to Firestore."
                        )
                    }
                }
            )
        }
    }

    fun loadAudioUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDecoding = true) }
            try {
                val decoded = AudioDecoder.decodeUri(appContext, uri)
                val songId = SongRepository.buildSongDocumentId(
                    fileName = decoded.fileName,
                    durationSec = decoded.durationSec,
                    sampleRate = decoded.sampleRate
                )
                val title = decoded.fileName.substringBeforeLast('.').ifBlank { decoded.fileName }
                val current = _uiState.value
                playbackEngine.loadTrack(decoded, current.params, current.isBypassed)

                val existingSong = songRepository.getSong(currentUserId, songId).getOrNull()
                _uiState.update {
                    it.copy(
                        decodedAudio = decoded,
                        isDecoding = false,
                        currentSongId = songId,
                        currentSongTitle = existingSong?.title?.ifBlank { title } ?: title,
                        lyricsDraft = existingSong?.lyrics ?: it.lyricsDraft,
                        lastSavedSongId = existingSong?.id,
                        saveLyricsError = null
                    )
                }
            } catch (t: Throwable) {
                _uiState.update { it.copy(isDecoding = false) }
                _messages.tryEmit(t.localizedMessage ?: "Failed to decode audio file.")
            }
        }
    }

    fun exportWavToUri(uri: Uri) {
        val track = _uiState.value.decodedAudio ?: return
        val params = _uiState.value.params
        val bypassed = _uiState.value.isBypassed
        val lang = _uiState.value.language

        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportProgress = 0f) }
            try {
                withContext(Dispatchers.IO) {
                    val out = appContext.contentResolver.openOutputStream(uri)
                        ?: error("Unable to open output stream for WAV export.")
                    out.use { stream ->
                        WavExporter.exportToWav(
                            decoded = track,
                            params = params,
                            isBypassed = bypassed,
                            outputStream = stream,
                            onProgress = { progress ->
                                _uiState.update { it.copy(exportProgress = progress) }
                            }
                        )
                    }
                }
                _uiState.update { it.copy(isExporting = false, exportProgress = 1f) }
                _messages.tryEmit(StudioStrings.exportSuccess(lang))
            } catch (t: Throwable) {
                _uiState.update { it.copy(isExporting = false) }
                _messages.tryEmit(t.localizedMessage ?: "Failed to export WAV.")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackEngine.release()
    }

    private companion object {
        const val TAG = "StudioViewModel"
    }
}
