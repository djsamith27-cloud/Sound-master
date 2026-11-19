package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ai.LyricsDoctorRepository
import com.example.ai.configureAppCheck
import com.example.audio.AppLanguage
import com.example.data.SongRepository
import com.example.ui.StudioViewModel
import com.example.ui.SwaraStudioScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOut
import com.example.ui.theme.SwaraStudioTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        // Configure App Check with launch intent before setContent or ViewModels
        configureAppCheck(this, intent)

        setContent {
            SwaraStudioApp()
        }
    }
}

@Composable
private fun SwaraStudioApp() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        SwaraStudioTheme(isSinhala = true) {
            AuthScreen(onAuthSuccess = {})
        }
    } else {
        key(user.uid) {
            AuthenticatedStudioHost(currentUserId = user.uid)
        }
    }
}

@Composable
private fun AuthenticatedStudioHost(currentUserId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: StudioViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                StudioViewModel(
                    songRepository = SongRepository(db),
                    lyricsRepository = LyricsDoctorRepository(),
                    appContext = app.applicationContext,
                    currentUserId = currentUserId
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedSongsState by viewModel.savedSongsState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.loadAudioUri(uri)
        }
    }

    val wavExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/wav")
    ) { uri ->
        if (uri != null) {
            viewModel.exportWavToUri(uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    SwaraStudioTheme(isSinhala = uiState.language == AppLanguage.SINHALA) {
        SwaraStudioScreen(
            uiState = uiState,
            savedSongsState = savedSongsState,
            snackbarHostState = snackbarHostState,
            onToggleLanguage = viewModel::toggleLanguage,
            onPickAudioClick = {
                audioPickerLauncher.launch(arrayOf("audio/*"))
            },
            onSignOutClick = {
                signOut(credentialManager = credentialManager, scope = scope)
            },
            onPlayPauseClick = viewModel::togglePlayPause,
            onSelectBypassed = viewModel::setBypassed,
            onExportWavClick = {
                wavExportLauncher.launch("swara_studio_fixed.wav")
            },
            onSeekFraction = viewModel::seekFraction,
            onSelectPreset = viewModel::selectPreset,
            onUpdateParams = viewModel::updateParams,
            onLyricsDraftChange = viewModel::onLyricsDraftChange,
            onFixLyricsClick = viewModel::fixLyricsText,
            onExtractAndFixFromAudioClick = viewModel::extractAndFixLyricsFromAudio,
            onSaveLyricsToSongClick = viewModel::saveModifiedLyricsToCurrentSong,
            onApplyCorrectedLyrics = viewModel::applyCorrectedLyrics,
            onSelectSavedSong = viewModel::selectSavedSong,
            onClearLyrics = viewModel::clearLyrics,
            onCopiedNotice = viewModel::notifyLyricsCopied
        )
    }
}
