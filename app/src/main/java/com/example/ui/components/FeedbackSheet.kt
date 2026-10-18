package com.example.ui.components

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FeedbackEntity
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackSheet(
    feedbackList: List<FeedbackEntity>,
    onSubmitFeedback: (rating: Int, category: String, comment: String) -> Unit,
    onDeleteFeedback: (id: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var rating by remember { mutableIntStateOf(5) }
    var selectedCategory by remember { mutableStateOf("Voice Recognition") }
    var commentText by remember { mutableStateOf("") }
    var showPastFeedback by remember { mutableStateOf(false) }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                commentText = if (commentText.isBlank()) spoken else "$commentText $spoken"
            }
        }
    }

    val categories = listOf(
        "Voice Recognition" to "آواز کی شناخت (Voice)",
        "Speed & Latency" to "رفتار اور رسپانس (Speed)",
        "Multilingual Accuracy" to "کثیر لسانی (Languages)",
        "Feature Request" to "نئی سہولت (Feature)",
        "Bug Report" to "خرابی یا بگ (Bug)",
        "General Appreciation" to "عمومی رائے (General)"
    )

    val ratingLabels = mapOf(
        1 to "1 Star - Needs Improvement ⚠️ (بہتری درکار ہے)",
        2 to "2 Stars - Fair 😐 (مناسب ہے)",
        3 to "3 Stars - Good 👍 (اچھا ہے)",
        4 to "4 Stars - Very Good ⭐ (بہت اچھا ہے)",
        5 to "5 Stars - Outstanding Experience! 🚀 (شاندار اور لاجواب!)"
    )

    val quickPresets = listOf(
        "⭐⭐⭐⭐⭐ Excellent performance! (بہترین کارکردگی)",
        "🎙️ Voice quality is great (آواز بہت اچھی ہے)",
        "🌐 Multilingual works smoothly (کثیر لسانی زبردست ہے)",
        "⚡ Response is fast and helpful (رسپانس تیز ہے)",
        "💡 Please add more voice commands (مزید وائس کمانڈز شامل کریں)"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = JarvisDarkNavy
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(JarvisCyan.copy(alpha = 0.15f))
                            .border(1.dp, JarvisCyan.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Feedback,
                            contentDescription = null,
                            tint = JarvisCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "USER FEEDBACK & RATING",
                            color = JarvisCyan,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "رائے اور تجاویز درج کریں (Share Your Experience)",
                            color = JarvisTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                if (feedbackList.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (showPastFeedback) JarvisPurple.copy(alpha = 0.25f) else JarvisCardBg)
                            .border(1.dp, if (showPastFeedback) JarvisPurple else JarvisCardBorder, RoundedCornerShape(8.dp))
                            .clickable { showPastFeedback = !showPastFeedback }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("toggle_history_button")
                    ) {
                        Text(
                            text = if (showPastFeedback) "New Form (نیا فارم)" else "History (${feedbackList.size}) (سابقہ ریکارڈ)",
                            color = if (showPastFeedback) JarvisPurple else JarvisTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (showPastFeedback) {
                // Past Feedback List
                Text(
                    text = "YOUR PREVIOUS SUBMISSIONS (آپ کی سابقہ آراء)",
                    color = JarvisPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(feedbackList, key = { it.id }) { item ->
                        PastFeedbackCard(
                            feedback = item,
                            onDelete = { onDeleteFeedback(item.id) }
                        )
                    }
                }
            } else {
                // New Feedback Form
                // 1. Rating Selector (1 to 5 Stars)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(JarvisCardBg)
                        .border(1.dp, JarvisCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "HOW WOULD YOU RATE JARVIS? (ریٹنگ درج کریں)",
                        color = JarvisCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..5) {
                            val isFilled = i <= rating
                            IconButton(
                                onClick = { rating = i },
                                modifier = Modifier
                                    .size(42.dp)
                                    .testTag("star_rating_$i")
                            ) {
                                Icon(
                                    imageVector = if (isFilled) Icons.Default.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "$i Stars",
                                    tint = if (isFilled) Color(0xFFFFD700) else JarvisTextDim,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ratingLabels[rating] ?: "",
                        color = JarvisCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Feedback Category Selector
                Text(
                    text = "FEEDBACK CATEGORY (شعبہ یا کیٹیگری منتخب کریں)",
                    color = JarvisTextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val row1 = categories.take(3)
                    for ((catKey, catLabel) in row1) {
                        val isSel = catKey == selectedCategory
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) JarvisCyan.copy(alpha = 0.2f) else JarvisCardBg)
                                .border(1.dp, if (isSel) JarvisCyan else JarvisCardBorder, RoundedCornerShape(8.dp))
                                .clickable { selectedCategory = catKey }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
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

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val row2 = categories.drop(3)
                    for ((catKey, catLabel) in row2) {
                        val isSel = catKey == selectedCategory
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) JarvisCyan.copy(alpha = 0.2f) else JarvisCardBg)
                                .border(1.dp, if (isSel) JarvisCyan else JarvisCardBorder, RoundedCornerShape(8.dp))
                                .clickable { selectedCategory = catKey }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
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

                // Quick Presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (preset in quickPresets) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(JarvisPurple.copy(alpha = 0.15f))
                                .border(1.dp, JarvisPurple.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickable { commentText = preset }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = preset,
                                color = JarvisPurple,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Comment Input with Voice Dictation Button
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = {
                        Text(
                            text = "Write or speak your feedback (اردو، پنجابی، हिन्दी، English)...",
                            color = JarvisTextDim,
                            fontSize = 12.sp
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your feedback / اپنی رائے بولیں...")
                                }
                                speechLauncher.launch(intent)
                            },
                            modifier = Modifier.testTag("dictate_feedback_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Dictate Feedback",
                                tint = JarvisCyan
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("feedback_comment_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisCardBorder,
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary,
                        focusedContainerColor = JarvisDarkNavy,
                        unfocusedContainerColor = JarvisDarkNavy
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Submit Button
                Button(
                    onClick = {
                        onSubmitFeedback(rating, selectedCategory, commentText)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JarvisCyan,
                        contentColor = JarvisObsidian
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_feedback_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SUBMIT FEEDBACK (فیڈ بیک جمع کروائیں)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun PastFeedbackCard(
    feedback: FeedbackEntity,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()) }
    val formattedDate = remember(feedback.timestamp) { dateFormat.format(Date(feedback.timestamp)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(JarvisCardBg)
            .border(1.dp, JarvisCardBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Star display
                Row {
                    for (i in 1..5) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (i <= feedback.rating) Color(0xFFFFD700) else JarvisTextDim,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(JarvisPurple.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = feedback.category,
                        color = JarvisPurple,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = JarvisTextDim,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        if (feedback.comment.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = feedback.comment,
                color = JarvisTextPrimary,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formattedDate,
            color = JarvisTextDim,
            fontSize = 10.sp
        )
    }
}
