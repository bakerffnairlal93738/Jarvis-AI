package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PhoneCallback
import androidx.compose.material.icons.automirrored.filled.PhoneMissed
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableIntStateOf
import com.example.ui.theme.JarvisCardBg
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisDarkNavy
import com.example.ui.theme.JarvisObsidian
import com.example.ui.theme.JarvisPurple
import com.example.ui.theme.JarvisStatusReady
import com.example.ui.theme.JarvisTextDim
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    apiKey: String,
    selectedVoice: String,
    selectedVoiceGender: String = "MALE",
    selectedLanguage: String = "auto",
    selectedModelPersona: String,
    isVoiceOutputEnabled: Boolean,
    speechVolume: Float = 1.0f,
    speechRate: Float,
    speechPitch: Float,
    isNoiseSuppressionEnabled: Boolean = true,
    noiseSuppressionLevel: Float = 0.70f,
    selectedAiModel: String = "Gemini 2.5 Flash",
    isRunInBackgroundEnabled: Boolean = true,
    isMissedCallAlertEnabled: Boolean = true,
    isContinuousListening: Boolean,
    bargeInSensitivity: Float,
    isWakeWordEnabled: Boolean,
    wakeWordSensitivity: Float,
    onSaveApiKey: (String) -> Unit,
    onVoiceSelected: (String) -> Unit,
    onVoiceGenderSelected: (String) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onModelPersonaChanged: (String) -> Unit,
    onAiModelSelected: (String) -> Unit = {},
    onRunInBackgroundToggled: (Boolean) -> Unit = {},
    onMissedCallAlertToggled: (Boolean) -> Unit = {},
    onTestMissedCall: () -> Unit = {},
    onRequestBatteryOptimization: () -> Unit = {},
    onVoiceOutputToggled: (Boolean) -> Unit,
    onSpeechVolumeChanged: (Float) -> Unit = {},
    onSpeechRateChanged: (Float) -> Unit,
    onSpeechPitchChanged: (Float) -> Unit,
    onNoiseSuppressionToggled: (Boolean) -> Unit = {},
    onNoiseSuppressionLevelChanged: (Float) -> Unit = {},
    onContinuousListeningToggled: (Boolean) -> Unit,
    onBargeInSensitivityChanged: (Float) -> Unit,
    onWakeWordToggled: (Boolean) -> Unit,
    onWakeWordSensitivityChanged: (Float) -> Unit,
    onClearConversationHistory: () -> Unit = {},
    onTestVoice: () -> Unit,
    onSubmitFeedback: (rating: Int, category: String, comment: String) -> Unit = { _, _, _ -> },
    onOpenFeedback: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var customKey by remember { mutableStateOf(apiKey) }
    var currentVolume by remember { mutableFloatStateOf(speechVolume) }
    var currentSensitivity by remember { mutableFloatStateOf(bargeInSensitivity) }
    var currentWakeSensitivity by remember { mutableFloatStateOf(wakeWordSensitivity) }
    var currentNoiseLevel by remember { mutableFloatStateOf(noiseSuppressionLevel) }
    var currentRate by remember { mutableFloatStateOf(speechRate) }
    var currentPitch by remember { mutableFloatStateOf(speechPitch) }
    var keySavedMessage by remember { mutableStateOf<String?>(null) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    var feedbackRating by remember { mutableIntStateOf(5) }
    var feedbackCategory by remember { mutableStateOf("Voice & Language") }
    var feedbackComment by remember { mutableStateOf("") }
    var feedbackSubmitted by remember { mutableStateOf(false) }

    var showVoiceGenderDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    BackHandler {
        onSaveApiKey(customKey.trim())
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = {
            onSaveApiKey(customKey.trim())
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = JarvisDarkNavy
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Back Button + Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = {
                            onSaveApiKey(customKey.trim())
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(JarvisCyan.copy(alpha = 0.15f))
                            .border(1.dp, JarvisCyan.copy(alpha = 0.45f), CircleShape)
                            .testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back / वापस",
                            tint = JarvisCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "SETTINGS (सेटिंग्स)",
                            color = JarvisCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Voice engine, background service & missed call alerts",
                            color = JarvisTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(JarvisPurple.copy(alpha = 0.2f))
                        .border(1.dp, JarvisPurple.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .clickable { onOpenFeedback() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("settings_top_feedback_pill")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Feedback,
                            contentDescription = null,
                            tint = JarvisPurple,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "FEEDBACK (رائے)",
                            color = JarvisPurple,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Spoken Language Preference (Auto-Detect / Specific Language)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisCardBg)
                    .border(1.dp, JarvisCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = JarvisCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SPOKEN LANGUAGE PREFERENCE",
                            color = JarvisCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Button(
                        onClick = { showLanguageDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JarvisCyan.copy(alpha = 0.15f),
                            contentColor = JarvisCyan
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("open_language_dialog_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Choose", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Auto-Detect will match whichever language you speak (English, Urdu, Punjabi, Hindi, or multiple languages), or lock to a preferred language:",
                    color = JarvisTextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Language Selection Chips
                val languages = listOf(
                    Triple("auto", "Auto-Detect 🌐", "Mirrors your spoken language"),
                    Triple("en", "English", "Speaks in English"),
                    Triple("ur", "Urdu (اردو)", "Speaks in Urdu"),
                    Triple("pa", "Punjabi (پنجابی)", "Speaks in Punjabi"),
                    Triple("hi", "Hindi (हिन्दी)", "Speaks in Hindi")
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for ((code, title, desc) in languages) {
                        val isSelected = selectedLanguage.equals(code, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) JarvisCyan.copy(alpha = 0.18f) else JarvisDarkNavy)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) JarvisCyan else JarvisCardBorder,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onLanguageSelected(code) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    color = if (isSelected) JarvisCyan else JarvisTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = desc,
                                    color = JarvisTextDim,
                                    fontSize = 10.sp
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = JarvisCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Voice Gender Selection Section (Male Voice vs Female Voice)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisCardBg)
                    .border(1.dp, JarvisPurple.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = JarvisPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "VOICE GENDER SELECTION",
                            color = JarvisPurple,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Button(
                        onClick = { showVoiceGenderDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JarvisPurple.copy(alpha = 0.2f),
                            contentColor = JarvisPurple
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("open_voice_gender_dialog_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Choose", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Select between Male Voice or Female Voice for spoken playback:",
                    color = JarvisTextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Direct Option Cards: Male vs Female
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isMale = selectedVoiceGender.equals("MALE", ignoreCase = true)
                    val isFemale = selectedVoiceGender.equals("FEMALE", ignoreCase = true)

                    // 1. Male Voice Card
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isMale) JarvisCyan.copy(alpha = 0.18f) else JarvisDarkNavy)
                            .border(
                                width = if (isMale) 2.dp else 1.dp,
                                color = if (isMale) JarvisCyan else JarvisCardBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onVoiceGenderSelected("MALE") }
                            .padding(12.dp)
                            .testTag("select_male_voice_option"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "👨", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Male Voice",
                                color = if (isMale) JarvisCyan else JarvisTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = "میل وائس",
                            color = if (isMale) JarvisCyan else JarvisTextDim,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Deep, commanding & authoritative tone\n(Kore / Deep)",
                            color = JarvisTextSecondary,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 13.sp
                        )

                        if (isMale) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(JarvisCyan)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ACTIVE ✓",
                                    color = JarvisObsidian,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    // 2. Female Voice Card
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isFemale) JarvisPurple.copy(alpha = 0.22f) else JarvisDarkNavy)
                            .border(
                                width = if (isFemale) 2.dp else 1.dp,
                                color = if (isFemale) JarvisPurple else JarvisCardBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onVoiceGenderSelected("FEMALE") }
                            .padding(12.dp)
                            .testTag("select_female_voice_option"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "👩", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Female Voice",
                                color = if (isFemale) JarvisPurple else JarvisTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = "فی میل وائس",
                            color = if (isFemale) JarvisPurple else JarvisTextDim,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Warm, natural & melodious tone\n(Aoede / Melodious)",
                            color = JarvisTextSecondary,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 13.sp
                        )

                        if (isFemale) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(JarvisPurple)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ACTIVE ✓",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Voice Output & Acoustic Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisCardBg)
                    .border(1.dp, JarvisCardBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = JarvisCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "VOICE OUTPUT",
                            color = JarvisCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Switch(
                        checked = isVoiceOutputEnabled,
                        onCheckedChange = onVoiceOutputToggled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = JarvisObsidian,
                            checkedTrackColor = JarvisCyan
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "When enabled, JARVIS speaks responses aloud through device speakers with live waveform animation",
                    color = JarvisTextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Speech Volume Slider (آواز کی بلندی / वॉल्यूम)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Speech Volume (آواز کی بلندی / वॉल्यूम)",
                        color = JarvisTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${(currentVolume * 100).toInt()}%",
                        color = JarvisCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = currentVolume,
                    onValueChange = {
                        currentVolume = it
                        onSpeechVolumeChanged(it)
                    },
                    valueRange = 0.10f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = JarvisCyan,
                        activeTrackColor = JarvisCyan,
                        inactiveTrackColor = JarvisCardBorder
                    ),
                    modifier = Modifier.testTag("speech_volume_slider")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Speech Speed Slider
                Text(
                    text = "Speech Speed (Rate): ${String.format(java.util.Locale.US, "%.1fx", currentRate)}",
                    color = JarvisTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = currentRate,
                    onValueChange = {
                        currentRate = it
                        onSpeechRateChanged(it)
                    },
                    valueRange = 0.7f..1.4f,
                    colors = SliderDefaults.colors(
                        thumbColor = JarvisCyan,
                        activeTrackColor = JarvisCyan,
                        inactiveTrackColor = JarvisCardBorder
                    )
                )

                // Speech Pitch Slider
                Text(
                    text = "Voice Pitch: ${String.format(java.util.Locale.US, "%.2f", currentPitch)}",
                    color = JarvisTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = currentPitch,
                    onValueChange = {
                        currentPitch = it
                        onSpeechPitchChanged(it)
                    },
                    valueRange = 0.75f..1.35f,
                    colors = SliderDefaults.colors(
                        thumbColor = JarvisCyan,
                        activeTrackColor = JarvisCyan,
                        inactiveTrackColor = JarvisCardBorder
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onTestVoice,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JarvisCyan,
                        contentColor = JarvisObsidian
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("test_voice_button")
                ) {
                    Text("TEST VOICE OUTPUT", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. AI Model Persona
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisCardBg)
                    .border(1.dp, JarvisCardBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = JarvisCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI MODEL PERSONA",
                        color = JarvisCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val personas = listOf(
                    "JARVIS" to "Stark Industries neural AI persona with multilingual fluency",
                    "Gemini Live" to "Ultra-low-latency 24kHz native audio stream"
                )

                for ((pName, pDesc) in personas) {
                    val isSelected = selectedModelPersona == pName
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) JarvisCyan.copy(alpha = 0.18f) else Color.Transparent)
                            .border(1.dp, if (isSelected) JarvisCyan else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { onModelPersonaChanged(pName) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pName,
                                color = if (isSelected) JarvisCyan else JarvisTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = pDesc,
                                color = JarvisTextDim,
                                fontSize = 11.sp
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = JarvisCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Live Synthesizer Voice for Gemini Live
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisCardBg)
                    .border(1.dp, JarvisCardBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = JarvisCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIVE SYNTHESIZER VOICE",
                        color = JarvisCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                val voices = listOf(
                    "Kore" to "Firm, confident, classic JARVIS (Male)",
                    "Aoede" to "Warm, conversational, lively (Female)",
                    "Puck" to "Playful, spirited, witty",
                    "Fenrir" to "Deep, resonant, commanding (Male)",
                    "Charon" to "Calm, analytical, measured (Male)"
                )

                for ((vName, vDesc) in voices) {
                    val isSelected = selectedVoice.equals(vName, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) JarvisCyan.copy(alpha = 0.15f) else Color.Transparent)
                            .border(1.dp, if (isSelected) JarvisCyan else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { onVoiceSelected(vName) }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = vName,
                                color = if (isSelected) JarvisCyan else JarvisTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = vDesc,
                                color = JarvisTextDim,
                                fontSize = 11.sp
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = JarvisCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Conversational & Barge-in Settings
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisCardBg)
                    .border(1.dp, JarvisCardBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Continuous Listening",
                            color = JarvisTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Microphone automatically reopens after assistant finishes speaking",
                            color = JarvisTextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = isContinuousListening,
                        onCheckedChange = onContinuousListeningToggled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = JarvisObsidian,
                            checkedTrackColor = JarvisCyan
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Barge-in / Interruption Sensitivity",
                    color = JarvisTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Text(
                    text = "Voice volume threshold required to interrupt JARVIS while speaking",
                    color = JarvisTextSecondary,
                    fontSize = 11.sp
                )
                Slider(
                    value = currentSensitivity,
                    onValueChange = {
                        currentSensitivity = it
                        onBargeInSensitivityChanged(it)
                    },
                    valueRange = 0.15f..0.75f,
                    colors = SliderDefaults.colors(
                        thumbColor = JarvisCyan,
                        activeTrackColor = JarvisCyan,
                        inactiveTrackColor = JarvisCardBorder
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Background Noise Suppression (شور اور نائز منسوخی)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisCardBg)
                    .border(1.dp, JarvisCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = JarvisCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "BACKGROUND NOISE SUPPRESSION",
                                color = JarvisCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "بیک گراؤنڈ شور اور نائز فلٹر (Noise Cancellation)",
                            color = JarvisTextPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = isNoiseSuppressionEnabled,
                        onCheckedChange = onNoiseSuppressionToggled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = JarvisObsidian,
                            checkedTrackColor = JarvisCyan
                        ),
                        modifier = Modifier.testTag("noise_suppression_switch")
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "When enabled, background fan noise, AC hum, room chatter, and static are actively eliminated so the mic only captures your speech.",
                    color = JarvisTextSecondary,
                    fontSize = 11.sp
                )

                if (isNoiseSuppressionEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cancellation Strength (شدت / क्षमता):",
                            color = JarvisTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${(currentNoiseLevel * 100).toInt()}%",
                            color = JarvisCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Slider(
                        value = currentNoiseLevel,
                        onValueChange = {
                            currentNoiseLevel = it
                            onNoiseSuppressionLevelChanged(it)
                        },
                        valueRange = 0.20f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = JarvisCyan,
                            activeTrackColor = JarvisCyan,
                            inactiveTrackColor = JarvisCardBorder
                        ),
                        modifier = Modifier.testTag("noise_suppression_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Mild (30%)", color = JarvisTextDim, fontSize = 10.sp)
                        Text("Balanced (70%)", color = JarvisCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("Maximum (100%)", color = JarvisTextDim, fontSize = 10.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(JarvisDarkNavy)
                            .border(1.dp, JarvisCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = JarvisStatusReady,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Hardware DSP & 80Hz High-Pass Noise Gate Active",
                                color = JarvisStatusReady,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7. Hands-Free Wake-Word Detection
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisCardBg)
                    .border(1.dp, JarvisCardBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = JarvisCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Hands-Free Wake Word",
                                color = JarvisTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Say 'Hey Jarvis' or 'Jarvis' anytime to activate the assistant hands-free",
                            color = JarvisTextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = isWakeWordEnabled,
                        onCheckedChange = onWakeWordToggled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = JarvisObsidian,
                            checkedTrackColor = JarvisCyan
                        ),
                        modifier = Modifier.testTag("wake_word_switch")
                    )
                }

                if (isWakeWordEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Supported Wake Words:",
                        color = JarvisCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Hey Jarvis", "Jarvis", "OK Jarvis", "جاروس", "जार्विस").forEach { word ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(JarvisPurple.copy(alpha = 0.2f))
                                    .border(1.dp, JarvisPurple.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = word,
                                    color = JarvisTextPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Spotter Sensitivity",
                            color = JarvisTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${(currentWakeSensitivity * 100).toInt()}%",
                            color = JarvisCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Slider(
                        value = currentWakeSensitivity,
                        onValueChange = {
                            currentWakeSensitivity = it
                            onWakeWordSensitivityChanged(it)
                        },
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = JarvisCyan,
                            activeTrackColor = JarvisCyan,
                            inactiveTrackColor = JarvisCardBorder
                        ),
                        modifier = Modifier.testTag("wake_word_sensitivity_slider")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 8. Gemini / AI API Key
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisCardBg)
                    .border(1.dp, JarvisCardBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = JarvisCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GEMINI / AI API KEY",
                        color = JarvisCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Loaded from Secrets panel/BuildConfig. Override below if needed.",
                    color = JarvisTextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = customKey,
                    onValueChange = {
                        customKey = it
                        onSaveApiKey(it.trim())
                    },
                    placeholder = { Text("Enter Gemini API key", color = JarvisTextDim) },
                    modifier = Modifier.fillMaxWidth().testTag("api_key_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisCardBorder,
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary
                    )
                )

                if (!keySavedMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = JarvisStatusReady,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = keySavedMessage ?: "",
                            color = JarvisStatusReady,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val trimmed = customKey.trim()
                        onSaveApiKey(trimmed)
                        keySavedMessage = "API Key saved permanently! (مستقل طور پر محفوظ ہو گئی)"
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JarvisCyan,
                        contentColor = JarvisObsidian
                    ),
                    modifier = Modifier.align(Alignment.End).testTag("save_api_key_button")
                ) {
                    Text("SAVE KEY (مستقل محفوظ کریں)", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 8.5. Missed Call Voice Alert & Telephony (مسڈ کال الرٹ)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisCardBg)
                    .border(
                        1.dp,
                        if (isMissedCallAlertEnabled) JarvisCyan.copy(alpha = 0.5f) else JarvisCardBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.PhoneMissed,
                                contentDescription = null,
                                tint = if (isMissedCallAlertEnabled) JarvisCyan else JarvisTextDim,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "MISSED CALL ALERT (مسڈ کال الرٹ)",
                                color = if (isMissedCallAlertEnabled) JarvisCyan else JarvisTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isMissedCallAlertEnabled) {
                                "آن: ایپ بیک گراؤنڈ میں رن ہو کر مسڈ کال کا الرٹ آواز میں دے گی۔"
                            } else {
                                "بند: ایپ بیک گراؤنڈ میں بالکل رن نہیں ہو رہی۔"
                            },
                            color = if (isMissedCallAlertEnabled) JarvisStatusReady else JarvisTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Alerts caller by voice (Urdu/Hindi/English) & notification. When OFF, background execution completely stops.",
                            color = JarvisTextSecondary,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Switch(
                        checked = isMissedCallAlertEnabled,
                        onCheckedChange = onMissedCallAlertToggled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = JarvisObsidian,
                            checkedTrackColor = JarvisCyan
                        ),
                        modifier = Modifier.testTag("missed_call_alert_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onTestMissedCall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JarvisCyan.copy(alpha = 0.15f),
                        contentColor = JarvisCyan
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_missed_call_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.PhoneCallback,
                            contentDescription = null,
                            tint = JarvisCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Test Missed Call Alert (مسڈ کال الرٹ ٹیسٹ کریں)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 9. Background Execution & Battery Optimization Permission (بیک گراؤنڈ میں رن کرنے کی پرمیشن)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisCardBg)
                    .border(1.dp, JarvisCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = JarvisCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BACKGROUND SERVICE (بیک گراؤنڈ پرمیشن)",
                                color = JarvisCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Keep JARVIS, voice spotter, and Termux Python active in the background when app is closed.",
                            color = JarvisTextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = isRunInBackgroundEnabled,
                        onCheckedChange = onRunInBackgroundToggled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = JarvisObsidian,
                            checkedTrackColor = JarvisCyan
                        ),
                        modifier = Modifier.testTag("background_execution_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onRequestBatteryOptimization,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JarvisPurple.copy(alpha = 0.25f),
                        contentColor = JarvisPurple
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("grant_battery_optimization_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = JarvisPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Grant Background Battery Exemption (بیک گراؤنڈ اجازت دیں)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 10. AI Model & Architecture Selector (ماڈل کا انتخاب)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisCardBg)
                    .border(1.dp, JarvisCardBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = JarvisCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI MODEL SELECTOR (ماڈل کا انتخاب)",
                        color = JarvisCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Select model for app design, Termux code execution, and voice conversation:",
                    color = JarvisTextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                val availableModels = listOf(
                    "Gemini 2.5 Flash" to "Google AI Studio • Fast, Responsive & Multilingual",
                    "Gemini Live" to "Google AI Studio • Bidirectional Real-Time Voice Link",
                    "ChatGPT-6 Persona" to "Agentic Model • Autonomous Reasoning & Vision",
                    "Claude 3.5 Sonnet" to "Architecture • Deep UI/UX & Code Synthesis",
                    "Termux Python Local" to "On-Device Engine • Offline Server Scripts"
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableModels.forEach { (modelId, desc) ->
                        val isSelected = selectedAiModel == modelId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) JarvisCyan.copy(alpha = 0.15f) else JarvisDarkNavy.copy(alpha = 0.6f))
                                .border(
                                    1.dp,
                                    if (isSelected) JarvisCyan else JarvisCardBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onAiModelSelected(modelId) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onAiModelSelected(modelId) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = JarvisCyan,
                                    unselectedColor = JarvisTextDim
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = modelId,
                                    color = if (isSelected) JarvisCyan else JarvisTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = desc,
                                    color = JarvisTextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Conversation History & Memory Persistence Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisCardBg)
                    .border(1.dp, JarvisPurple.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = JarvisPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CONVERSATION MEMORY (محفوظ گفتگو)",
                        color = JarvisPurple,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "All your queries, voice interactions, and assistant context are automatically preserved in device storage and stay saved across app restarts.",
                    color = JarvisTextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { showClearHistoryDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935).copy(alpha = 0.2f),
                        contentColor = Color(0xFFFF5252)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("clear_history_button")
                ) {
                    Text("CLEAR CONVERSATION HISTORY (ہسٹری صاف کریں)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 9. User Feedback & Rating Section (رائے اور فیڈ بیک)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisCardBg)
                    .border(1.dp, JarvisCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Feedback,
                            contentDescription = null,
                            tint = JarvisCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "FEEDBACK & SUGGESTIONS",
                            color = JarvisCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(JarvisPurple.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "رائے اور تجاویز",
                            color = JarvisPurple,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Help improve JARVIS by sharing your rating, reporting issues, or suggesting new features directly here:",
                    color = JarvisTextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (feedbackSubmitted) {
                    // Success Banner
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(JarvisStatusReady.copy(alpha = 0.15f))
                            .border(1.dp, JarvisStatusReady.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = JarvisStatusReady,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Feedback Received! (آپ کی رائے موصول ہو گئی ہے)",
                                color = JarvisStatusReady,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Thank you for helping us improve JARVIS. / آپ کی قیمتی رائے کا بہت شکریہ!",
                            color = JarvisTextPrimary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { feedbackSubmitted = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisCyan.copy(alpha = 0.2f),
                                contentColor = JarvisCyan
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Submit Another (مزید رائے دیں)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // 1. Star Rating Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..5) {
                            val isFilled = i <= feedbackRating
                            IconButton(
                                onClick = { feedbackRating = i },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("settings_star_$i")
                            ) {
                                Icon(
                                    imageVector = if (isFilled) Icons.Default.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "$i Stars",
                                    tint = if (isFilled) Color(0xFFFFD700) else JarvisTextDim,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    val ratingLabel = when (feedbackRating) {
                        1 -> "1 Star ⚠️ Needs Improvement (بہتری درکار ہے)"
                        2 -> "2 Stars 😐 Fair (مناسب ہے)"
                        3 -> "3 Stars 👍 Good (اچھا ہے)"
                        4 -> "4 Stars ⭐ Very Good (بہت اچھا ہے)"
                        else -> "5 Stars 🚀 Outstanding Experience! (شاندار اور لاجواب!)"
                    }
                    Text(
                        text = ratingLabel,
                        color = JarvisCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Category Selector
                    val categories = listOf(
                        "Voice & Audio" to "آواز (Voice)",
                        "Languages" to "زبانیں (Languages)",
                        "Speed & Latency" to "رفتار (Speed)",
                        "Features & Tools" to "فیچرز (Features)"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for ((catKey, catLabel) in categories) {
                            val isSel = catKey == feedbackCategory
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) JarvisCyan.copy(alpha = 0.2f) else JarvisDarkNavy)
                                    .border(1.dp, if (isSel) JarvisCyan else JarvisCardBorder, RoundedCornerShape(8.dp))
                                    .clickable { feedbackCategory = catKey }
                                    .padding(vertical = 6.dp, horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = catLabel,
                                    color = if (isSel) JarvisCyan else JarvisTextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. Comment Input
                    OutlinedTextField(
                        value = feedbackComment,
                        onValueChange = { feedbackComment = it },
                        placeholder = {
                            Text(
                                text = "Your comments or suggestions (اردو / English / हिन्दी)...",
                                color = JarvisTextDim,
                                fontSize = 11.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .testTag("settings_feedback_comment_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisCardBorder,
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary,
                            focusedContainerColor = JarvisDarkNavy,
                            unfocusedContainerColor = JarvisDarkNavy
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Preset Chips
                    val presets = listOf(
                        "⭐⭐⭐⭐⭐ Excellent (شاندار)",
                        "🎙️ Voice is great (آواز اچھی ہے)",
                        "🌐 Multilingual works well",
                        "⚡ Response is fast"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (p in presets) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(JarvisPurple.copy(alpha = 0.15f))
                                    .border(1.dp, JarvisPurple.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .clickable { feedbackComment = p }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(text = p, color = JarvisPurple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Submit Feedback Button
                    Button(
                        onClick = {
                            onSubmitFeedback(feedbackRating, feedbackCategory, feedbackComment)
                            feedbackSubmitted = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JarvisCyan,
                            contentColor = JarvisObsidian
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("settings_submit_feedback_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SUBMIT FEEDBACK (رائے جمع کروائیں)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Detailed Full Feedback Center Button
                Button(
                    onClick = onOpenFeedback,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JarvisCyan.copy(alpha = 0.12f),
                        contentColor = JarvisCyan
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("settings_open_feedback_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Feedback,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("OPEN FEEDBACK CENTER & HISTORY (مکمل فیڈ بیک اور سابقہ آراء)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Dialog for Voice Gender Selection
        if (showVoiceGenderDialog) {
            AlertDialog(
                onDismissRequest = { showVoiceGenderDialog = false },
                containerColor = JarvisDarkNavy,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = JarvisCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Select Voice Gender",
                            color = JarvisCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Choose between Male Voice or Female Voice:",
                            color = JarvisTextSecondary,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Option 1: Male Voice
                        val isMale = selectedVoiceGender.equals("MALE", ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isMale) JarvisCyan.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable {
                                    onVoiceGenderSelected("MALE")
                                    showVoiceGenderDialog = false
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isMale,
                                onClick = {
                                    onVoiceGenderSelected("MALE")
                                    showVoiceGenderDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = JarvisCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "👨 Male Voice (میل وائس)",
                                    color = if (isMale) JarvisCyan else JarvisTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Classic JARVIS deep & authoritative tone",
                                    color = JarvisTextDim,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Option 2: Female Voice
                        val isFemale = selectedVoiceGender.equals("FEMALE", ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isFemale) JarvisPurple.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable {
                                    onVoiceGenderSelected("FEMALE")
                                    showVoiceGenderDialog = false
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isFemale,
                                onClick = {
                                    onVoiceGenderSelected("FEMALE")
                                    showVoiceGenderDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = JarvisPurple)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "👩 Female Voice (فی میل وائس)",
                                    color = if (isFemale) JarvisPurple else JarvisTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Warm, natural & melodious tone",
                                    color = JarvisTextDim,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showVoiceGenderDialog = false }) {
                        Text("Done", color = JarvisCyan, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Dialog for Language Selection
        if (showLanguageDialog) {
            AlertDialog(
                onDismissRequest = { showLanguageDialog = false },
                containerColor = JarvisDarkNavy,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = JarvisCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Select Spoken Language",
                            color = JarvisCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Choose Auto-Detect (Dynamic Mirroring) or pick a preferred language:",
                            color = JarvisTextSecondary,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val allLangs = listOf(
                            Triple("auto", "Auto-Detect 🌐 (Recommended)", "Mirrors whatever language you speak: English, Urdu, Punjabi, Hindi"),
                            Triple("en", "English", "Always replies in fluent English"),
                            Triple("ur", "Urdu (اردو)", "Always replies in polite Urdu"),
                            Triple("pa", "Punjabi (پنجابی / ਪੰਜਾਬੀ)", "Always replies in natural Punjabi"),
                            Triple("hi", "Hindi (हिन्दी)", "Always replies in fluent Hindi"),
                            Triple("ar", "Arabic (العربية)", "Always replies in Arabic"),
                            Triple("es", "Spanish (Español)", "Always replies in Spanish"),
                            Triple("fr", "French (Français)", "Always replies in French"),
                            Triple("de", "German (Deutsch)", "Always replies in German")
                        )

                        for ((code, title, desc) in allLangs) {
                            val isSel = selectedLanguage.equals(code, ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) JarvisCyan.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable {
                                        onLanguageSelected(code)
                                        showLanguageDialog = false
                                    }
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSel,
                                    onClick = {
                                        onLanguageSelected(code)
                                        showLanguageDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = JarvisCyan)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = title,
                                        color = if (isSel) JarvisCyan else JarvisTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = desc,
                                        color = JarvisTextDim,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLanguageDialog = false }) {
                        Text("Done", color = JarvisCyan, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        if (showClearHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showClearHistoryDialog = false },
                containerColor = JarvisDarkNavy,
                title = {
                    Text(
                        text = "Clear History? (ہسٹری صاف کریں؟)",
                        color = Color(0xFFFF5252),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to clear your conversation history? All saved messages will be erased from persistent storage.",
                        color = JarvisTextSecondary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onClearConversationHistory()
                            showClearHistoryDialog = false
                        }
                    ) {
                        Text("Clear All (صاف کریں)", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearHistoryDialog = false }) {
                        Text("Cancel", color = JarvisTextDim)
                    }
                }
            )
        }
    }
}
