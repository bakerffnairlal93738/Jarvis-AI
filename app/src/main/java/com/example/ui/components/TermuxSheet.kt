package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.example.service.TermuxPythonAppState
import com.example.ui.theme.JarvisCardBg
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisCrimson
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
fun TermuxSheet(
    termuxState: TermuxPythonAppState,
    selectedLanguage: String,
    onStartApp: (String) -> Unit,
    onStopPython: () -> Unit,
    onOpenBrowser: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var showScriptCode by remember { mutableStateOf(false) }

    val isUrdu = selectedLanguage == "ur"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = JarvisDarkNavy
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = if (termuxState.isRunning) JarvisStatusReady else JarvisCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "TERMUX PYTHON STUDIO",
                            color = JarvisCyan,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (isUrdu) "اینڈرائیڈ ٹرمکس اور بیک گراؤنڈ پائتھن سرور" else "Android Termux & Background Python Server",
                            color = JarvisTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Active status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (termuxState.isRunning) JarvisStatusReady.copy(alpha = 0.2f) else JarvisCardBg)
                        .border(1.dp, if (termuxState.isRunning) JarvisStatusReady else JarvisCardBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (termuxState.isRunning) "RUNNING" else "STOPPED",
                        color = if (termuxState.isRunning) JarvisStatusReady else JarvisTextDim,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current Server State Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(JarvisCardBg)
                    .border(
                        1.dp,
                        if (termuxState.isRunning) JarvisStatusReady.copy(alpha = 0.5f) else JarvisCardBorder,
                        RoundedCornerShape(14.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (termuxState.isRunning) JarvisStatusReady else JarvisCrimson)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (termuxState.isRunning) {
                                if (isUrdu) "بیک گراؤنڈ میں چل رہا ہے" else "ACTIVE IN BACKGROUND"
                            } else {
                                if (isUrdu) "پائتھن سرور بند ہے" else "PYTHON SERVER STOPPED"
                            },
                            color = if (termuxState.isRunning) JarvisStatusReady else JarvisTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (termuxState.isRunning) {
                        Text(
                            text = "Port ${termuxState.port}",
                            color = JarvisCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (termuxState.isRunning) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = termuxState.appName.ifBlank { "Python Web App" },
                        color = JarvisTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "URL: ${termuxState.serverUrl}",
                        color = JarvisCyan,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Buttons: Open Web App & Stop Python
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onOpenBrowser,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisCyan,
                                contentColor = JarvisObsidian
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("open_web_app_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isUrdu) "ویب سائٹ کھولیں" else "Open App", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onStopPython,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisCrimson,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("stop_python_button")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isUrdu) "پائتھن بند کریں" else "Stop Python", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isUrdu)
                            "آپ وائس کمانڈ دے سکتے ہیں: 'ٹرمکس میں ویب سائٹ بناؤ اور پائتھن رن کرو'، یا نیچے دیے گئے بٹنوں سے فوری ایپ تیار کریں۔"
                        else
                            "Say 'Run website in Termux Python' or select a preset below to build and run an app in the background.",
                        color = JarvisTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick App Presets to Launch
            Text(
                text = if (isUrdu) "فوری ایپ تیار کریں (Quick Generate)" else "QUICK GENERATE & RUN IN PYTHON",
                color = JarvisCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))

            val presets = listOf(
                "پورٹ فولیو ویب سائٹ" to "Portfolio Website (پورٹ فولیو)",
                "ٹوڈو ٹاسک ایپ" to "Task Manager (ٹوڈو ایپ)",
                "سائنٹیفک کیلکولیٹر" to "Scientific Calculator (کیلکولیٹر)",
                "موسم ویدر اسٹیشن" to "Weather Station (موسم)"
            )

            for ((urduTitle, engTitle) in presets) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(JarvisCardBg)
                        .border(1.dp, JarvisCardBorder, RoundedCornerShape(10.dp))
                        .clickable { onStartApp(urduTitle) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isUrdu) urduTitle else engTitle,
                            color = JarvisTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Runs in Termux background on port 5000",
                            color = JarvisTextDim,
                            fontSize = 11.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run",
                        tint = JarvisCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Voice Command Guidance
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisPurple.copy(alpha = 0.12f))
                    .border(1.dp, JarvisPurple.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = if (isUrdu) "🎙️ آواز کے ذریعے کمانڈز (Voice Commands)" else "🎙️ VOICE COMMANDS",
                    color = JarvisPurple,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                val commandsList = if (isUrdu) {
                    listOf(
                        "• 'ٹرمکس میں پورٹ فولیو ویب سائٹ بناؤ اور پائتھن رن کرو'",
                        "• 'بیک گراؤنڈ سے پائتھن بند کر دو'",
                        "• 'ٹرمکس ایپ کھولو' یا 'ویب سائٹ دیکھو'"
                    )
                } else {
                    listOf(
                        "• 'Make a portfolio website and run in Termux Python in background'",
                        "• 'Stop Python from background'",
                        "• 'Open Python web app'"
                    )
                }
                for (cmd in commandsList) {
                    Text(text = cmd, color = JarvisTextPrimary, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // View Code & Copy Termux Script
            if (termuxState.pythonScriptCode.isNotBlank()) {
                OutlinedButton(
                    onClick = { showScriptCode = !showScriptCode },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("toggle_script_code_button")
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showScriptCode) "کوڈ چھپائیں (Hide Code)" else "پائتھن سکرپٹ دیکھیں (View Python Code)")
                }

                if (showScriptCode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(JarvisObsidian)
                            .border(1.dp, JarvisCardBorder, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = termuxState.pythonScriptCode,
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Termux Command", termuxState.termuxCommand)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Termux کمانڈ کاپی کر لی گئی!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisCardBg, contentColor = JarvisCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("copy_termux_command_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Termux Command کاپی کریں")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
