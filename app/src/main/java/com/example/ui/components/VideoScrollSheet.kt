package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.JarvisAccessibilityService
import com.example.ui.theme.JarvisCardBg
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanDim
import com.example.ui.theme.JarvisDarkNavy
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisObsidian
import com.example.ui.theme.JarvisPurple
import com.example.ui.theme.JarvisStatusReady
import com.example.ui.theme.JarvisTextDim
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import kotlinx.coroutines.launch

data class VideoItem(
    val id: String,
    val title: String,
    val creator: String,
    val category: String,
    val views: String,
    val duration: String,
    val youtubeQuery: String,
    val gradientColors: List<Color>
)

val sampleVideos = listOf(
    VideoItem(
        id = "v1",
        title = "Iron Man Arc Reactor & Holographic JARVIS Core HUD in Action",
        creator = "@StarkIndustries",
        category = "AI & Technology",
        views = "2.4M",
        duration = "0:58",
        youtubeQuery = "iron man jarvis hud arc reactor shorts",
        gradientColors = listOf(Color(0xFF0D47A1), Color(0xFF00E5FF))
    ),
    VideoItem(
        id = "v2",
        title = "Gemini 2.5 Live Real-Time Voice AI and Autonomous Assistants",
        creator = "@GoogleDeepMind",
        category = "Next-Gen AI",
        views = "1.8M",
        duration = "0:45",
        youtubeQuery = "gemini live voice demo shorts",
        gradientColors = listOf(Color(0xFF4A148C), Color(0xFF7C4DFF))
    ),
    VideoItem(
        id = "v3",
        title = "Ultra Fast FPV Drone Dive through Mountain Peaks in 4K HDR",
        creator = "@SkyVisions",
        category = "Adventure / Reel",
        views = "3.2M",
        duration = "0:30",
        youtubeQuery = "fpv drone mountain dive 4k shorts",
        gradientColors = listOf(Color(0xFF004D40), Color(0xFF00BFA5))
    ),
    VideoItem(
        id = "v4",
        title = "Top 5 Android Tricks & Hidden Features You Never Knew Existed",
        creator = "@TechGuru",
        category = "Smartphones & Tips",
        views = "950K",
        duration = "0:52",
        youtubeQuery = "hidden android features and tricks shorts",
        gradientColors = listOf(Color(0xFFB71C1C), Color(0xFFFF5252))
    ),
    VideoItem(
        id = "v5",
        title = "SpaceX Starship Super Heavy Rocket Booster Catch Reel",
        creator = "@SpaceXHQ",
        category = "Space Exploration",
        views = "4.6M",
        duration = "1:00",
        youtubeQuery = "spacex starship booster catch shorts",
        gradientColors = listOf(Color(0xFF212121), Color(0xFF00E5FF))
    ),
    VideoItem(
        id = "v6",
        title = "AI Smart Glasses & Gesture Computing Hands-Free Demonstration",
        creator = "@FutureWearables",
        category = "Wearables & Robotics",
        views = "1.1M",
        duration = "0:42",
        youtubeQuery = "smart glasses gesture control demo reel",
        gradientColors = listOf(Color(0xFF1A237E), Color(0xFF2979FF))
    )
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VideoScrollSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    scrollTrigger: Long,
    scrollDirection: String,
    onScrollNext: () -> Unit,
    onScrollPrevious: () -> Unit,
    onSendVideo: (title: String, url: String, recipient: String) -> Unit,
    onSearchYouTube: (query: String) -> Unit,
    onCallContact: (name: String) -> Unit,
    onOpenApp: (appName: String) -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var currentPlayingIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchRow by remember { mutableStateOf(false) }

    val isAccessibilityActive = remember(isOpen) {
        JarvisAccessibilityService.isServiceEnabled()
    }

    // Auto-scroll response when voice command "स्क्रॉल द वीडियो" or "next video" is spoken!
    LaunchedEffect(scrollTrigger) {
        if (scrollTrigger > 0L) {
            val isUp = scrollDirection.equals("up", ignoreCase = true)
            if (isUp) {
                val prev = (currentPlayingIndex - 1).coerceAtLeast(0)
                currentPlayingIndex = prev
                listState.animateScrollToItem(prev)
            } else {
                val next = (currentPlayingIndex + 1).coerceAtMost(sampleVideos.size - 1)
                currentPlayingIndex = next
                listState.animateScrollToItem(next)
            }
        }
    }

    BackHandler { onDismiss() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = JarvisObsidian,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(JarvisObsidian)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(JarvisCyan.copy(alpha = 0.15f))
                            .border(1.dp, JarvisCyan.copy(alpha = 0.45f), CircleShape)
                            .testTag("video_hub_back_button")
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
                            text = "JARVIS REELS & VIDEO HUB",
                            color = JarvisCyan,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "वॉयस कंट्रोल्ड वीडियो स्क्रोलिंग & शेयरिंग",
                            color = JarvisTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showSearchRow = !showSearchRow },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (showSearchRow) JarvisCyan else JarvisTextSecondary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = JarvisTextDim
                        )
                    }
                }
            }

            // System Accessibility Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (isAccessibilityActive) JarvisStatusReady.copy(alpha = 0.12f) else JarvisGold.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isAccessibilityActive) JarvisStatusReady.copy(alpha = 0.4f) else JarvisGold.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Accessibility,
                            contentDescription = null,
                            tint = if (isAccessibilityActive) JarvisStatusReady else JarvisGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAccessibilityActive)
                                "सिस्टम-वाइड स्क्रॉल सक्रिय: YouTube Shorts और Instagram Reels में भी काम करेगा!"
                            else
                                "YouTube Shorts व Reels में सिस्टम-वाइड स्क्रॉल के लिए एक्सेसिबिलिटी चालू करें",
                            color = JarvisTextPrimary,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }

                    if (!isAccessibilityActive) {
                        OutlinedButton(
                            onClick = { JarvisAccessibilityService.openAccessibilitySettings(context) },
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisGold),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisGold)
                        ) {
                            Text("चालू करें", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Expandable in-app search field
            AnimatedVisibility(visible = showSearchRow) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("video_search_input"),
                        placeholder = { Text("यूट्यूब पर जो चाहें सर्च करें...", fontSize = 12.sp, color = JarvisTextDim) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisCardBorder,
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                onSearchYouTube(searchQuery)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("सर्च", color = JarvisObsidian, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Quick Voice Action Hints
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                VideoQuickChip(
                    text = "🎬 'स्क्रॉल द वीडियो'",
                    onClick = {
                        coroutineScope.launch {
                            val next = (currentPlayingIndex + 1).coerceAtMost(sampleVideos.size - 1)
                            currentPlayingIndex = next
                            listState.animateScrollToItem(next)
                            onScrollNext()
                        }
                    }
                )
                VideoQuickChip(
                    text = "📤 'यह वीडियो सेंड करो'",
                    onClick = {
                        val v = sampleVideos[currentPlayingIndex]
                        onSendVideo(v.title, "https://www.youtube.com/results?search_query=${Uri.encode(v.youtubeQuery)}", "")
                    }
                )
                VideoQuickChip(
                    text = "🔍 'यूट्यूब पर सर्च करो'",
                    onClick = { onSearchYouTube("AI robot automation") }
                )
                VideoQuickChip(
                    text = "📞 'कांटेक्ट को कॉल लगाओ'",
                    onClick = { onCallContact("Rahul") }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Vertical Feed of Video Cards
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(sampleVideos) { index, video ->
                    VideoCardItem(
                        video = video,
                        isCurrent = index == currentPlayingIndex,
                        isPlaying = isPlaying && index == currentPlayingIndex,
                        onTogglePlay = {
                            if (currentPlayingIndex == index) {
                                isPlaying = !isPlaying
                            } else {
                                currentPlayingIndex = index
                                isPlaying = true
                            }
                        },
                        onScrollNext = {
                            coroutineScope.launch {
                                val next = (index + 1).coerceAtMost(sampleVideos.size - 1)
                                currentPlayingIndex = next
                                listState.animateScrollToItem(next)
                                onScrollNext()
                            }
                        },
                        onScrollPrevious = {
                            coroutineScope.launch {
                                val prev = (index - 1).coerceAtLeast(0)
                                currentPlayingIndex = prev
                                listState.animateScrollToItem(prev)
                                onScrollPrevious()
                            }
                        },
                        onSend = {
                            onSendVideo(
                                video.title,
                                "https://www.youtube.com/results?search_query=${Uri.encode(video.youtubeQuery)}",
                                ""
                            )
                        },
                        onOpenYouTube = {
                            val encoded = Uri.encode(video.youtubeQuery)
                            val ytIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded")).apply {
                                setPackage("com.google.android.youtube")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                context.startActivity(ytIntent)
                            } catch (e: Exception) {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded")).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(browserIntent)
                            }
                        }
                    )
                }
            }

            // Bottom Sticky Control Deck for Fast Scrolling and Sending
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = JarvisDarkNavy,
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val prev = (currentPlayingIndex - 1).coerceAtLeast(0)
                                    currentPlayingIndex = prev
                                    listState.animateScrollToItem(prev)
                                    onScrollPrevious()
                                }
                            },
                            modifier = Modifier.size(44.dp),
                            contentPadding = PaddingValues(0.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCardBorder),
                            shape = CircleShape
                        ) {
                            Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "Previous Video", tint = JarvisTextSecondary)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val next = (currentPlayingIndex + 1).coerceAtMost(sampleVideos.size - 1)
                                    currentPlayingIndex = next
                                    listState.animateScrollToItem(next)
                                    onScrollNext()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = null, tint = JarvisObsidian)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "स्क्रॉल द वीडियो (Next)",
                                color = JarvisObsidian,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val v = sampleVideos[currentPlayingIndex]
                            onSendVideo(v.title, "https://www.youtube.com/results?search_query=${Uri.encode(v.youtubeQuery)}", "")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisPurple),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "सेंड करो",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoCardItem(
    video: VideoItem,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onScrollNext: () -> Unit,
    onScrollPrevious: () -> Unit,
    onSend: () -> Unit,
    onOpenYouTube: () -> Unit
) {
    val borderColor = if (isCurrent) JarvisCyan else JarvisCardBorder

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(JarvisCardBg)
            .border(if (isCurrent) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Video Preview Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = video.gradientColors
                        )
                    )
                    .clickable { onTogglePlay() },
                contentAlignment = Alignment.Center
            ) {
                // Dim overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                )

                // Play / Pause Circle
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(2.dp, if (isCurrent) JarvisCyan else Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = if (isCurrent) JarvisCyan else Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Top Badge: Category & Duration
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = video.category,
                            color = JarvisCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = video.duration,
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                // Active Indicator
                if (isCurrent) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = JarvisCyan
                    ) {
                        Text(
                            text = "NOW PLAYING",
                            color = JarvisObsidian,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Video Details & Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = video.title,
                    color = JarvisTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${video.creator} • ${video.views} views",
                        color = JarvisTextSecondary,
                        fontSize = 12.sp
                    )

                    Text(
                        text = "बोलें: 'स्क्रॉल द वीडियो'",
                        color = JarvisCyanDim,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onScrollNext,
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("स्क्रॉल करें", color = JarvisCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onSend,
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisPurple.copy(alpha = 0.2f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisPurple),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = JarvisPurple, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("सेंड کریں", color = JarvisPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onOpenYouTube,
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisDarkNavy),
                        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCardBorder),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "YouTube", tint = JarvisTextPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoQuickChip(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = JarvisDarkNavy,
        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCardBorder)
    ) {
        Text(
            text = text,
            color = JarvisTextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
