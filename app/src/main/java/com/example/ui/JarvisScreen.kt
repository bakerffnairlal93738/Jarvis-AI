package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
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
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartDisplay
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
import androidx.compose.material3.Surface
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
import com.example.ui.components.VideoScrollSheet
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

    val phonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.toggleMissedCallAlert(true)
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

    val activity = context as? Activity
    val anySheetOpen = uiState.isSettingsOpen || uiState.isCommandsOpen || uiState.isNotesOpen || uiState.isTermuxSheetOpen || uiState.isFeedbackOpen || uiState.isVideoHubOpen

    BackHandler(enabled = anySheetOpen) {
        if (uiState.isSettingsOpen) viewModel.openSettings(false)
        if (uiState.isCommandsOpen) viewModel.openCommands(false)
        if (uiState.isNotesOpen) viewModel.openNotes(false)
        if (uiState.isTermuxSheetOpen) viewModel.openTermuxSheet(false)
        if (uiState.isFeedbackOpen) viewModel.openFeedback(false)
        if (uiState.isVideoHubOpen) viewModel.openVideoHub(false)
    }

    val handleBackNavigation: () -> Unit = {
        if (anySheetOpen) {
            viewModel.openSettings(false)
            viewModel.openCommands(false)
            viewModel.openNotes(false)
            viewModel.openTermuxSheet(false)
            viewModel.openFeedback(false)
            viewModel.openVideoHub(false)
        } else {
            activity?.moveTaskToBack(true)
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
            // Top HUD Bar with Back Button on Left and Settings on Right
            TopHudBar(
                status = uiState.status,
                voiceName = uiState.selectedVoice,
                modelPersona = uiState.selectedModelPersona,
                onBack = handleBackNavigation,
                onOpenSettings = { viewModel.openSettings(true) }
            )

            // Quick Action Strip (Termux, Video Hub, Notes, Commands, Feedback)
            QuickHudStrip(
                isTermuxRunning = uiState.termuxState.isRunning,
                onOpenTermux = { viewModel.openTermuxSheet(true) },
                onOpenVideoHub = { viewModel.openVideoHub(true) },
                onOpenNotes = { viewModel.openNotes(true) },
                onOpenCommands = { viewModel.openCommands(true) },
                onOpenFeedback = { viewModel.openFeedback(true) }
            )

            Spacer(modifier = Modifier.height(2.dp))

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
                speechVolume = uiState.speechVolume,
                speechRate = uiState.speechRate,
                speechPitch = uiState.speechPitch,
                isNoiseSuppressionEnabled = uiState.isNoiseSuppressionEnabled,
                noiseSuppressionLevel = uiState.noiseSuppressionLevel,
                selectedAiModel = uiState.selectedAiModel,
                isRunInBackgroundEnabled = uiState.isRunInBackgroundEnabled,
                isMissedCallAlertEnabled = uiState.isMissedCallAlertEnabled,
                isContinuousListening = uiState.isContinuousListening,
                bargeInSensitivity = uiState.bargeInSensitivity,
                isWakeWordEnabled = uiState.isWakeWordEnabled,
                wakeWordSensitivity = uiState.wakeWordSensitivity,
                onSaveApiKey = { key -> viewModel.updateApiKey(key) },
                onVoiceSelected = { v -> viewModel.setVoice(v) },
                onVoiceGenderSelected = { g -> viewModel.setVoiceGender(g) },
                onLanguageSelected = { l -> viewModel.setLanguage(l) },
                onModelPersonaChanged = { p -> viewModel.setModelPersona(p) },
                onAiModelSelected = { m -> viewModel.setSelectedAiModel(m) },
                onRunInBackgroundToggled = { enabled -> viewModel.toggleRunInBackground(enabled) },
                onMissedCallAlertToggled = { enabled ->
                    if (enabled) {
                        val permissionsToRequest = mutableListOf(Manifest.permission.READ_PHONE_STATE)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        val needsPermission = permissionsToRequest.any {
                            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                        }
                        if (needsPermission) {
                            phonePermissionLauncher.launch(permissionsToRequest.toTypedArray())
                        } else {
                            viewModel.toggleMissedCallAlert(true)
                        }
                    } else {
                        viewModel.toggleMissedCallAlert(false)
                    }
                },
                onTestMissedCall = { viewModel.testMissedCallAlert() },
                onRequestBatteryOptimization = { viewModel.requestBatteryOptimization(context) },
                onVoiceOutputToggled = { enabled -> viewModel.toggleVoiceOutput() },
                onSpeechVolumeChanged = { vol -> viewModel.setSpeechVolume(vol) },
                onSpeechRateChanged = { r -> viewModel.setSpeechRate(r) },
                onSpeechPitchChanged = { p -> viewModel.setSpeechPitch(p) },
                onNoiseSuppressionToggled = { enabled -> viewModel.toggleNoiseSuppression(enabled) },
                onNoiseSuppressionLevelChanged = { lvl -> viewModel.setNoiseSuppressionLevel(lvl) },
                onContinuousListeningToggled = { c -> viewModel.setContinuousListening(c) },
                onBargeInSensitivityChanged = { s -> viewModel.setBargeInSensitivity(s) },
                onWakeWordToggled = { enabled -> viewModel.toggleWakeWord(enabled) },
                onWakeWordSensitivityChanged = { s -> viewModel.setWakeWordSensitivity(s) },
                onClearConversationHistory = { viewModel.clearConversationHistory() },
                onTestVoice = {
                    val testMsg = when (uiState.selectedLanguage) {
                        "hi" -> "नमस्ते! मैं जार्विस हूँ। वॉयस आउटपुट और वॉल्यूम बिल्कुल ठीक काम कर रहा है।"
                        "ur" -> "السلام علیکم! میں جاروس ہوں۔ آواز اور والیوم بہترین کام کر رہا ہے۔"
                        "pa" -> "ست شری اکال! میں جاروس آں، آواز تے والیوم بالکل ٹھیک چل ریا اے۔"
                        else -> "Hello! JARVIS voice output and volume are functioning smoothly."
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

        if (uiState.isVideoHubOpen) {
            VideoScrollSheet(
                isOpen = true,
                onDismiss = { viewModel.openVideoHub(false) },
                scrollTrigger = uiState.activeVideoScrollTrigger,
                scrollDirection = uiState.activeVideoDirection,
                onScrollNext = { viewModel.scrollVideo("down") },
                onScrollPrevious = { viewModel.scrollVideo("up") },
                onSendVideo = { title, url, recipient ->
                    viewModel.sendCurrentVideo(title, url, recipient)
                },
                onSearchYouTube = { query ->
                    viewModel.searchInApp("youtube", query)
                },
                onCallContact = { name ->
                    viewModel.callContact(name)
                },
                onOpenApp = { appName ->
                    viewModel.openAnyApp(appName)
                }
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
    onBack: () -> Unit = {},
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LEFT SIDE: Back Button + Jarvis Identity (App icon & title)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(JarvisCyan.copy(alpha = 0.15f))
                    .border(1.dp, JarvisCyan.copy(alpha = 0.45f), CircleShape)
                    .testTag("top_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back / वापस",
                    tint = JarvisCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, JarvisCyan.copy(alpha = 0.85f), CircleShape)
                    .background(JarvisObsidian)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_jarvis_logo),
                    contentDescription = "JARVIS AI Logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "JARVIS",
                        color = JarvisCyan,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.6.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                when (status) {
                                    AssistantStatus.LISTENING -> Color(0xFF00E676)
                                    AssistantStatus.SPEAKING -> JarvisPurple
                                    AssistantStatus.THINKING -> JarvisGold
                                    AssistantStatus.ERROR -> JarvisCrimson
                                    else -> JarvisCyan
                                }
                            )
                    )
                }
                Text(
                    text = "A.I. SYSTEM • MARK VII",
                    color = JarvisTextDim,
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.8.sp
                )
            }
        }

        // RIGHT SIDE: Dedicated, High-Affordance Settings Button (Never blocked, easy to tap)
        Surface(
            onClick = onOpenSettings,
            shape = RoundedCornerShape(12.dp),
            color = JarvisCyan.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.6f)),
            modifier = Modifier
                .height(40.dp)
                .testTag("top_settings_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = JarvisCyan,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SETTINGS",
                    color = JarvisCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun QuickHudStrip(
    isTermuxRunning: Boolean,
    onOpenTermux: () -> Unit,
    onOpenVideoHub: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenCommands: () -> Unit,
    onOpenFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuickPillButton(
            icon = Icons.Default.Terminal,
            label = "Termux",
            tint = if (isTermuxRunning) JarvisStatusReady else JarvisCyanDim,
            testTag = "top_termux_button",
            onClick = onOpenTermux
        )
        QuickPillButton(
            icon = Icons.Default.SmartDisplay,
            label = "Video Hub",
            tint = JarvisCyan,
            testTag = "top_video_hub_button",
            onClick = onOpenVideoHub
        )
        QuickPillButton(
            icon = Icons.Default.Bookmark,
            label = "Notes",
            tint = JarvisCyanDim,
            testTag = "top_notes_button",
            onClick = onOpenNotes
        )
        QuickPillButton(
            icon = Icons.Default.Code,
            label = "Commands",
            tint = JarvisCyanDim,
            testTag = "top_commands_button",
            onClick = onOpenCommands
        )
        QuickPillButton(
            icon = Icons.Default.Feedback,
            label = "Feedback",
            tint = JarvisPurple,
            testTag = "top_feedback_button",
            onClick = onOpenFeedback
        )
    }
}

@Composable
private fun QuickPillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = JarvisCardBg,
        border = BorderStroke(1.dp, tint.copy(alpha = 0.35f)),
        modifier = Modifier
            .height(32.dp)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                color = JarvisTextSecondary,
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
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
