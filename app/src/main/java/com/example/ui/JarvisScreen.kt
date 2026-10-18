package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.ArcReactorOrb
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.ControlsDeck
import com.example.ui.components.CustomCommandsSheet
import com.example.ui.components.FeedbackSheet
import com.example.ui.components.NotesRemindersSheet
import com.example.ui.components.SettingsSheet
import com.example.ui.components.TermuxSheet
import com.example.ui.components.TranscriptsView
import com.example.ui.theme.JarvisCardBg
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanDim
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisObsidian
import com.example.ui.theme.JarvisPurple
import com.example.ui.theme.JarvisStatusError
import com.example.ui.theme.JarvisStatusListening
import com.example.ui.theme.JarvisStatusReady
import com.example.ui.theme.JarvisStatusSpeaking
import com.example.ui.theme.JarvisStatusThinking
import com.example.ui.theme.JarvisTextDim
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@Composable
fun JarvisScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var showPermissionRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            viewModel.toggleLiveSession()
        } else {
            showPermissionRationale = true
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(uiState.feedbackSubmittedMessage) {
        uiState.feedbackSubmittedMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedbackSubmittedMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = JarvisObsidian,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(JarvisObsidian),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top HUD Bar with Model Persona Badge (ChatGPT-6)
            TopHudBar(
                status = uiState.status,
                voiceName = uiState.selectedVoice,
                modelPersona = uiState.selectedModelPersona,
                isTermuxRunning = uiState.termuxState.isRunning,
                onOpenTermux = { viewModel.openTermuxSheet(true) },
                onOpenNotes = { viewModel.openNotes(true) },
                onOpenCommands = { viewModel.openCommands(true) },
                onOpenFeedback = { viewModel.openFeedback(true) },
                onOpenSettings = { viewModel.openSettings(true) }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Upper Center: Arc Reactor & Real-Time Waveform
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ArcReactorOrb(
                    status = uiState.status,
                    amplitude = uiState.amplitude,
                    isSpeaking = uiState.isSpeaking,
                    size = 185.dp,
                    onClick = {
                        if (!hasMicPermission) {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            viewModel.toggleLiveSession()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Status Label
                StatusDisplayLabel(
                    status = uiState.status,
                    isHindi = uiState.selectedLanguage == "hi"
                )

                if (uiState.isWakeWordEnabled && uiState.status == AssistantStatus.READY) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(JarvisPurple.copy(alpha = 0.15f))
                            .border(1.dp, JarvisPurple.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .testTag("wake_word_active_badge")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(JarvisStatusReady)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = when (uiState.selectedLanguage) {
                                "ur" -> "ہینڈز فری: 'جاروس' فعال ہے"
                                "hi" -> "हैंड्स-फ्री: 'जार्विस' सक्रिय"
                                else -> "WAKE WORD: 'HEY JARVIS' ACTIVE"
                            },
                            color = JarvisPurple,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Real-time audio waveform visualizer
                AudioWaveformVisualizer(
                    amplitude = uiState.amplitude,
                    isActive = uiState.status == AssistantStatus.LISTENING || uiState.status == AssistantStatus.SPEAKING,
                    isSpeaking = uiState.isSpeaking,
                    height = 34.dp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Center: Transcripts feed
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                TranscriptsView(
                    messages = uiState.messages,
                    liveUserSpeech = uiState.liveUserSpeech,
                    onReplaySpeech = { text -> viewModel.replayMessageSpeech(text) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom Controls Deck with Voice Input & Output controls
            ControlsDeck(
                status = uiState.status,
                isSpeaking = uiState.isSpeaking,
                isMuted = uiState.isMuted,
                isVoiceOutputEnabled = uiState.isVoiceOutputEnabled,
                selectedLanguage = uiState.selectedLanguage,
                selectedMode = uiState.selectedMode,
                selectedModelPersona = uiState.selectedModelPersona,
                onToggleLive = {
                    if (!hasMicPermission) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        viewModel.toggleLiveSession()
                    }
                },
                onStopSpeaking = { viewModel.stopSpeaking() },
                onToggleMute = { viewModel.toggleMute() },
                onToggleVoiceOutput = { viewModel.toggleVoiceOutput() },
                onLanguageSelected = { lang -> viewModel.setLanguage(lang) },
                onModeChanged = { mode -> viewModel.setMode(mode) },
                onModelPersonaChanged = { persona -> viewModel.setModelPersona(persona) },
                onQuickCommand = { cmd -> viewModel.processUserPrompt(cmd) },
                onOpenFeedback = { viewModel.openFeedback(true) },
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Sheets
        if (uiState.isSettingsOpen) {
            SettingsSheet(
                apiKey = uiState.apiKey,
                selectedVoice = uiState.selectedVoice,
                selectedVoiceGender = uiState.selectedVoiceGender,
                selectedLanguage = uiState.selectedLanguage,
                selectedModelPersona = uiState.selectedModelPersona,
                isVoiceOutputEnabled = uiState.isVoiceOutputEnabled,
                speechRate = uiState.speechRate,
                speechPitch = uiState.speechPitch,
                isContinuousListening = uiState.isContinuousListening,
                bargeInSensitivity = uiState.bargeInSensitivity,
                isWakeWordEnabled = uiState.isWakeWordEnabled,
                wakeWordSensitivity = uiState.wakeWordSensitivity,
                onSaveApiKey = { key -> viewModel.updateApiKey(key) },
                onVoiceSelected = { v -> viewModel.setVoice(v) },
                onVoiceGenderSelected = { g -> viewModel.setVoiceGender(g) },
                onLanguageSelected = { l -> viewModel.setLanguage(l) },
                onModelPersonaChanged = { p -> viewModel.setModelPersona(p) },
                onVoiceOutputToggled = { enabled -> viewModel.toggleVoiceOutput() },
                onSpeechRateChanged = { r -> viewModel.setSpeechRate(r) },
                onSpeechPitchChanged = { p -> viewModel.setSpeechPitch(p) },
                onContinuousListeningToggled = { c -> viewModel.setContinuousListening(c) },
                onBargeInSensitivityChanged = { s -> viewModel.setBargeInSensitivity(s) },
                onWakeWordToggled = { enabled -> viewModel.toggleWakeWord(enabled) },
                onWakeWordSensitivityChanged = { s -> viewModel.setWakeWordSensitivity(s) },
                onTestVoice = {
                    val testMsg = if (uiState.selectedLanguage == "hi") {
                        "नमस्ते! मैं जार्विस हूँ। वॉयस आउटपुट बिल्कुल ठीक काम कर रहा है।"
                    } else {
                        "Hello! JARVIS voice output is functioning smoothly."
                    }
                    viewModel.replayMessageSpeech(testMsg)
                },
                onSubmitFeedback = { rating, category, comment ->
                    viewModel.submitFeedback(rating, category, comment)
                },
                onOpenFeedback = {
                    viewModel.openSettings(false)
                    viewModel.openFeedback(true)
                },
                onDismiss = { viewModel.openSettings(false) }
            )
        }

        if (uiState.isCommandsOpen) {
            CustomCommandsSheet(
                commands = uiState.customCommands,
                onAddCommand = { t, u, d -> viewModel.addCustomCommand(t, u, d) },
                onDeleteCommand = { cmd -> viewModel.deleteCustomCommand(cmd) },
                onTestCommand = { cmd -> viewModel.testCustomCommand(cmd) },
                onDismiss = { viewModel.openCommands(false) }
            )
        }

        if (uiState.isNotesOpen) {
            NotesRemindersSheet(
                notes = uiState.notes,
                reminders = uiState.reminders,
                onDeleteNote = { note -> viewModel.deleteNote(note) },
                onDeleteReminder = { rem -> viewModel.deleteReminder(rem) },
                onReadNotesAloud = { viewModel.readNotesAloud() },
                onDismiss = { viewModel.openNotes(false) }
            )
        }

        if (uiState.isTermuxSheetOpen) {
            TermuxSheet(
                termuxState = uiState.termuxState,
                selectedLanguage = uiState.selectedLanguage,
                onStartApp = { appName -> viewModel.startTermuxPython(appName) },
                onStopPython = { viewModel.stopTermuxPython() },
                onOpenBrowser = { viewModel.openTermuxWebApp() },
                onDismiss = { viewModel.openTermuxSheet(false) }
            )
        }

        if (uiState.isFeedbackOpen) {
            FeedbackSheet(
                feedbackList = uiState.feedbackList,
                onSubmitFeedback = { rating, category, comment ->
                    viewModel.submitFeedback(rating, category, comment)
                },
                onDeleteFeedback = { id ->
                    viewModel.deleteFeedback(id)
                },
                onDismiss = { viewModel.openFeedback(false) }
            )
        }

        // Permission Rationale Dialog
        if (showPermissionRationale) {
            AlertDialog(
                onDismissRequest = { showPermissionRationale = false },
                containerColor = JarvisCardBg,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = JarvisGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (uiState.selectedLanguage == "hi") "माइक्रोफ़ोन एक्सेस की अनुमति चाहिए" else "Microphone Access Required",
                            color = JarvisTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Text(
                        if (uiState.selectedLanguage == "hi")
                            "जार्विस (JARVIS) को आपकी आवाज़ सुनने और लाइव वॉयस कमांड निष्पादित करने के लिए माइक्रोफ़ोन अनुमति आवश्यक है।"
                        else
                            "JARVIS requires microphone permission to listen to your voice and execute spoken commands.",
                        color = JarvisTextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPermissionRationale = false
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = JarvisObsidian)
                    ) {
                        Text(if (uiState.selectedLanguage == "hi") "अनुमति दें" else "Grant Permission", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showPermissionRationale = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = JarvisTextDim)
                    ) {
                        Text(if (uiState.selectedLanguage == "hi") "रद्द करें" else "Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun TopHudBar(
    status: AssistantStatus,
    voiceName: String,
    modelPersona: String,
    isTermuxRunning: Boolean = false,
    onOpenTermux: () -> Unit = {},
    onOpenNotes: () -> Unit,
    onOpenCommands: () -> Unit,
    onOpenFeedback: () -> Unit = {},
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Title & Model Persona badge
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(JarvisCyan)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "JARVIS",
                color = JarvisCyan,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisPurple.copy(alpha = 0.25f))
                    .border(1.dp, JarvisPurple.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "NEURAL CORE",
                    color = JarvisPurple,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Action Icons: Termux Studio, Notes, Commands, Feedback, Settings
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onOpenTermux,
                modifier = Modifier.size(36.dp).testTag("top_termux_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Termux Python Studio",
                    tint = if (isTermuxRunning) JarvisStatusReady else JarvisCyanDim,
                    modifier = Modifier.size(19.dp)
                )
            }

            IconButton(
                onClick = onOpenNotes,
                modifier = Modifier.size(36.dp).testTag("top_notes_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = "Notes and Reminders",
                    tint = JarvisCyanDim,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onOpenCommands,
                modifier = Modifier.size(36.dp).testTag("top_commands_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = "Custom Voice Commands",
                    tint = JarvisCyanDim,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onOpenFeedback,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(JarvisPurple.copy(alpha = 0.2f))
                    .testTag("top_feedback_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Feedback,
                    contentDescription = "Feedback & Rating",
                    tint = JarvisPurple,
                    modifier = Modifier.size(19.dp)
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(36.dp).testTag("top_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = JarvisCyanDim,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun StatusDisplayLabel(status: AssistantStatus, isHindi: Boolean = false) {
    val (statusText, statusColor) = when (status) {
        AssistantStatus.READY -> (if (isHindi) "सिस्टम तैयार (READY)" else "SYSTEM READY") to JarvisStatusReady
        AssistantStatus.CONNECTING -> (if (isHindi) "वॉयस लिंक कनेक्ट हो रहा है…" else "INITIALIZING VOICE LINK…") to JarvisCyan
        AssistantStatus.LISTENING -> (if (isHindi) "सुन रहा हूँ… बोलिए" else "LISTENING… (SPEAK NOW)") to JarvisStatusListening
        AssistantStatus.THINKING -> (if (isHindi) "प्रोसेसिंग… (NEURAL CORE)" else "PROCESSING NEURAL CORE…") to JarvisStatusThinking
        AssistantStatus.SPEAKING -> (if (isHindi) "बोल रहा हूँ… (SPEAKING)" else "SPEAKING…") to JarvisStatusSpeaking
        AssistantStatus.INTERRUPTED -> (if (isHindi) "इंटरप्टेड — सुन रहा हूँ" else "INTERRUPTED — LISTENING") to JarvisCrimson
        AssistantStatus.DISCONNECTED -> (if (isHindi) "स्टैंडबाय मोड" else "OFFLINE / STANDBY") to Color(0xFF64748B)
        AssistantStatus.RECONNECTING -> (if (isHindi) "री-कनेक्ट हो रहा है…" else "RECONNECTING…") to JarvisGold
        AssistantStatus.ERROR -> (if (isHindi) "सिस्टम एरर" else "SYSTEM ALERT") to JarvisStatusError
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(statusColor.copy(alpha = 0.15f))
            .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }
    }
}
