package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.AssistantStatus
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisObsidian
import com.example.ui.theme.JarvisPurple
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorOrb(
    status: AssistantStatus,
    amplitude: Float,
    isSpeaking: Boolean,
    size: Dp = 220.dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "reactor_anim")

    // Slow or fast rotation based on state
    val rotationSpeed = when (status) {
        AssistantStatus.THINKING -> 2000
        AssistantStatus.SPEAKING -> 4000
        AssistantStatus.LISTENING -> 6000
        else -> 12000
    }

    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = rotationSpeed, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rot"
    )

    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (rotationSpeed * 1.5).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_rot"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Dynamic amplitude reaction
    val animatedAmp = remember { Animatable(0f) }
    LaunchedEffect(amplitude) {
        animatedAmp.animateTo(
            targetValue = amplitude,
            animationSpec = tween(durationMillis = 80, easing = LinearEasing)
        )
    }

    val primaryColor = when (status) {
        AssistantStatus.READY -> JarvisCyan
        AssistantStatus.CONNECTING -> JarvisCyan
        AssistantStatus.LISTENING -> Color(0xFF00E676)
        AssistantStatus.THINKING -> JarvisGold
        AssistantStatus.SPEAKING -> JarvisPurple
        AssistantStatus.INTERRUPTED -> JarvisCrimson
        AssistantStatus.ERROR -> JarvisCrimson
        AssistantStatus.DISCONNECTED -> Color(0xFF64748B)
        AssistantStatus.RECONNECTING -> JarvisGold
    }

    val glowAlpha = (0.25f + (animatedAmp.value * 0.5f)).coerceIn(0.2f, 0.95f)

    Box(
        modifier = modifier
            .size(size)
            .testTag("arc_reactor_orb")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val maxRadius = (this.size.minDimension / 2f) * pulseScale

            // 1. Ambient Radial Glow
            val glowRadius = maxRadius * (1.1f + animatedAmp.value * 0.4f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = glowAlpha),
                        primaryColor.copy(alpha = glowAlpha * 0.4f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = center
            )

            // 2. Outer Technical Ring
            drawCircle(
                color = primaryColor.copy(alpha = 0.35f),
                radius = maxRadius,
                center = center,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), outerRotation)
                )
            )

            // 3. Segmented Arcs
            val arcRadius = maxRadius * 0.85f
            val strokeWidth = 4.dp.toPx()
            for (i in 0 until 4) {
                val startAngle = outerRotation + (i * 90f) + 15f
                drawArc(
                    color = primaryColor.copy(alpha = 0.85f),
                    startAngle = startAngle,
                    sweepAngle = 60f,
                    useCenter = false,
                    topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                    size = androidx.compose.ui.geometry.Size(arcRadius * 2, arcRadius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // 4. Middle Ring with Ticks
            val tickRadius = maxRadius * 0.68f
            for (i in 0 until 16) {
                val angleRad = Math.toRadians((innerRotation + (i * 22.5)).toDouble())
                val innerPoint = Offset(
                    center.x + (tickRadius - 6.dp.toPx()) * cos(angleRad).toFloat(),
                    center.y + (tickRadius - 6.dp.toPx()) * sin(angleRad).toFloat()
                )
                val outerPoint = Offset(
                    center.x + tickRadius * cos(angleRad).toFloat(),
                    center.y + tickRadius * sin(angleRad).toFloat()
                )
                drawLine(
                    color = primaryColor.copy(alpha = 0.6f),
                    start = innerPoint,
                    end = outerPoint,
                    strokeWidth = 2.dp.toPx()
                )
            }

            // 5. Inner Core Ring
            val coreRingRadius = maxRadius * 0.45f
            drawCircle(
                color = primaryColor,
                radius = coreRingRadius,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            // 6. Subtle inner core glow
            val solidCoreRadius = maxRadius * 0.36f * (1f + animatedAmp.value * 0.2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.45f),
                        primaryColor.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = solidCoreRadius * 1.3f
                ),
                radius = solidCoreRadius * 1.3f,
                center = center
            )
        }

        // Center High-Tech JARVIS AI Core Logo
        val coreSize = size * 0.58f
        Box(
            modifier = Modifier
                .size(coreSize)
                .clip(CircleShape)
                .border(2.dp, primaryColor.copy(alpha = 0.85f), CircleShape)
                .background(JarvisObsidian),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_jarvis_logo),
                contentDescription = "JARVIS Core Reactor Logo",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            // Dynamic holographic state tint overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                primaryColor.copy(alpha = if (status == AssistantStatus.READY) 0.15f else 0.35f)
                            )
                        )
                    )
            )

            // Center Interactive Action Glyphs
            val icon = if (isSpeaking) {
                Icons.Default.Stop
            } else {
                Icons.Default.Mic
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(JarvisObsidian.copy(alpha = 0.82f))
                    .border(1.5.dp, primaryColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = if (isSpeaking) "Stop Speaking" else "Microphone",
                    tint = primaryColor,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}
