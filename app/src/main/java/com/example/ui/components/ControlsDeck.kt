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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AssistantStatus
import com.example.ui.theme.JarvisCardBg
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisDarkNavy
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisObsidian
import com.example.ui.theme.JarvisPurple
import com.example.ui.theme.JarvisTextDim
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@Composable
fun ControlsDeck(
    status: AssistantStatus,
    isSpeaking: Boolean,
    isMuted: Boolean,
    isVoiceOutputEnabled: Boolean,
    selectedLanguage: String,
    selectedMode: String, // "LIVE" or "VOICE"
    selectedModelPersona: String, // "ChatGPT-6", "JARVIS", "Gemini Live"
    onToggleLive: () -> Unit,
    onStopSpeaking: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleVoiceOutput: () -> Unit,
    onLanguageSelected: (String) -> Unit,
    onModeChanged: (String) -> Unit,
    onModelPersonaChanged: (String) -> Unit,
    onQuickCommand: (String) -> Unit,
    onOpenFeedback: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // AI Model Voice Selector Pills (ChatGPT-6 highlighted)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // AI Assistant Core Badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(JarvisCardBg)
                    .border(1.dp, JarvisCyan.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = JarvisCyan,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "JARVIS NEURAL CORE",
                    color = JarvisCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Language Selector: Auto, English, Urdu, Punjabi, Hindi
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(JarvisCardBg)
                    .border(1.dp, JarvisCardBorder, RoundedCornerShape(20.dp))
                    .padding(3.dp)
            ) {
                LanguageButton(code = "auto", label = "AUTO 🌐", selected = selectedLanguage == "auto", onClick = { onLanguageSelected("auto") })
                LanguageButton(code = "en", label = "EN", selected = selectedLanguage == "en", onClick = { onLanguageSelected("en") })
                LanguageButton(code = "ur", label = "اردو", selected = selectedLanguage == "ur", onClick = { onLanguageSelected("ur") })
                LanguageButton(code = "pa", label = "پنجابی", selected = selectedLanguage == "pa", onClick = { onLanguageSelected("pa") })
                LanguageButton(code = "hi", label = "हिंदी", selected = selectedLanguage == "hi", onClick = { onLanguageSelected("hi") })
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Action Command Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val quickCommands = when (selectedLanguage) {
                "pa" -> listOf(
                    "⭐ فیڈ بیک دیو (Feedback)",
                    "کی حال اے جاروس",
                    "ٹرمکس وچ ویب سائٹ بناؤ",
                    "پائتھن بند کر دیو",
                    "ویب ایپ کھولو",
                    "کی ٹائم ہویا اے؟",
                    "اج کیہڑی تاریخ اے؟",
                    "یوٹیوب کھولو",
                    "گوگل کھولو",
                    "نوٹس پڑھو",
                    "حساب: 50 * 5"
                )
                "ur" -> listOf(
                    "⭐ فیڈ بیک دو (Feedback)",
                    "ٹرمکس میں ویب سائٹ بناؤ",
                    "بیک گراؤنڈ سے پائتھن بند کر دو",
                    "ٹرمکس ایپ کھولو",
                    "سلام جاروس",
                    "کیا وقت ہوا ہے؟",
                    "آج کی تاریخ کیا ہے؟",
                    "یوٹیوب کھولو",
                    "گوگل کھولو",
                    "نوٹس پڑھو",
                    "حساب: 50 * 4"
                )
                "hi" -> listOf(
                    "⭐ फीडबैक दें (Feedback)",
                    "नमस्ते जार्विस",
                    "टर्मक्स में वेबसाइट बनाओ",
                    "बैकग्राउंड पायथन बंद करो",
                    "तुम कौन हो?",
                    "समय क्या हुआ है?",
                    "आज का मौसम कैसा है?",
                    "यूट्यूब खोलो",
                    "गूगल खोलो",
                    "नोट्स पढ़ो",
                    "हिसाब: 25 * 4"
                )
                else -> listOf(
                    "⭐ Give Feedback (رائے)",
                    "What's the weather today?",
                    "کی حال اے جاروس",
                    "کیا وقت ہوا ہے؟",
                    "नमस्ते जार्विस",
                    "Make Python app in Termux",
                    "Open YouTube",
                    "Calculate 45 * 12",
                    "Read my notes"
                )
            }
            for (cmd in quickCommands) {
                QuickCommandChip(
                    text = cmd,
                    onClick = {
                        if (cmd.contains("Feedback", ignoreCase = true) || cmd.contains("فیڈ بیک") || cmd.contains("फीडबैक")) {
                            onOpenFeedback()
                        } else {
                            onQuickCommand(cmd)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Mode switch row: LIVE STREAM vs CONVERSATIONAL VOICE
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(JarvisDarkNavy)
                    .border(1.dp, JarvisCardBorder, RoundedCornerShape(18.dp))
                    .padding(2.dp)
            ) {
                ModeButton(
                    title = "वॉयस इनपुट / आउटपुट (VOICE)",
                    selected = selectedMode == "VOICE",
                    onClick = { onModeChanged("VOICE") }
                )
                ModeButton(
                    title = "लाइव ऑडियो (LIVE 24kHz)",
                    selected = selectedMode == "LIVE",
                    onClick = { onModeChanged("LIVE") }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Main Action Bar: Mic Input | Central Start/Stop Mic | Voice Output Speaker | Feedback
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Voice Input Mute/Unmute Toggle Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (isMuted) JarvisCrimson.copy(alpha = 0.2f) else JarvisCardBg)
                        .border(1.dp, if (isMuted) JarvisCrimson else JarvisCardBorder, CircleShape)
                        .testTag("mute_button")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (isMuted) "Unmute Mic" else "Mute Mic",
                        tint = if (isMuted) JarvisCrimson else JarvisCyan
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (isMuted) "माइक बंद" else "माइक चालू",
                    fontSize = 9.sp,
                    color = if (isMuted) JarvisCrimson else JarvisTextDim,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Main Primary Live Mic Button
            val isLiveActive = status == AssistantStatus.LISTENING || status == AssistantStatus.CONNECTING || status == AssistantStatus.THINKING
            Button(
                onClick = onToggleLive,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLiveActive) JarvisCrimson else JarvisCyan,
                    contentColor = JarvisObsidian
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .height(50.dp)
                    .testTag("main_action_button")
            ) {
                Icon(
                    imageVector = if (isLiveActive) Icons.Default.Stop else Icons.Default.GraphicEq,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when {
                        status == AssistantStatus.LISTENING -> if (selectedLanguage == "hi") "सुन रहा हूँ… (रोकें)" else "LISTENING… (STOP)"
                        status == AssistantStatus.SPEAKING -> if (selectedLanguage == "hi") "बोलना रोकें" else "INTERRUPT"
                        status == AssistantStatus.THINKING -> if (selectedLanguage == "hi") "सोच रहा हूँ…" else "THINKING…"
                        status == AssistantStatus.CONNECTING -> if (selectedLanguage == "hi") "कनेक्ट ہو رہا ہے…" else "CONNECTING…"
                        else -> if (selectedLanguage == "hi") "बोलें (वॉयस)" else if (selectedLanguage == "ur") "بولیں (مائیک)" else "TAP TO SPEAK"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }

            // Voice Output Speaker Toggle Button (آواز آن / میوٹ)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onToggleVoiceOutput,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (isVoiceOutputEnabled) JarvisPurple.copy(alpha = 0.25f) else JarvisCardBg)
                        .border(1.dp, if (isVoiceOutputEnabled) JarvisPurple else JarvisCardBorder, CircleShape)
                        .testTag("voice_output_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isVoiceOutputEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = if (isVoiceOutputEnabled) "Mute Voice Output" else "Enable Voice Output",
                        tint = if (isVoiceOutputEnabled) JarvisPurple else JarvisTextDim
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (isVoiceOutputEnabled) "آواز آن" else "آواز میوٹ",
                    fontSize = 9.sp,
                    color = if (isVoiceOutputEnabled) JarvisPurple else JarvisTextDim,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Dedicated Quick Feedback Button (رائے / فیڈ بیک)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onOpenFeedback,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(JarvisPurple.copy(alpha = 0.2f))
                        .border(1.dp, JarvisPurple.copy(alpha = 0.5f), CircleShape)
                        .testTag("controls_deck_feedback_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Feedback,
                        contentDescription = "Give Feedback",
                        tint = JarvisPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (selectedLanguage == "ur" || selectedLanguage == "pa") "فیڈ بیک" else if (selectedLanguage == "hi") "फीडबैक" else "FEEDBACK",
                    fontSize = 9.sp,
                    color = JarvisPurple,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ModelPill(title: String, selected: Boolean, isSpecial: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) {
                    if (isSpecial) JarvisCyan else JarvisPurple
                } else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isSpecial) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = if (selected) JarvisObsidian else JarvisCyan,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = title,
                color = if (selected) JarvisObsidian else JarvisTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun QuickCommandChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(JarvisCardBg)
            .border(1.dp, JarvisCardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = JarvisTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ModeButton(title: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) JarvisCyan else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = title,
            color = if (selected) JarvisObsidian else JarvisTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun LanguageButton(code: String, label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) JarvisCyan.copy(alpha = 0.2f) else Color.Transparent)
            .border(
                1.dp,
                if (selected) JarvisCyan else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            color = if (selected) JarvisCyan else JarvisTextSecondary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
