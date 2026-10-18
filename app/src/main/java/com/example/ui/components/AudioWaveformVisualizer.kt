package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisPurple
import kotlin.random.Random

@Composable
fun AudioWaveformVisualizer(
    amplitude: Float,
    isActive: Boolean,
    isSpeaking: Boolean,
    height: Dp = 44.dp,
    barCount: Int = 28,
    modifier: Modifier = Modifier
) {
    val barHeights = remember { List(barCount) { Animatable(0.08f) } }

    LaunchedEffect(amplitude, isActive) {
        val targetColor = if (isSpeaking) JarvisPurple else JarvisCyan
        for (i in 0 until barCount) {
            val distFromCenter = kotlin.math.abs(i - (barCount / 2f)) / (barCount / 2f)
            val bellFactor = (1f - distFromCenter * 0.6f).coerceIn(0.2f, 1f)

            val baseTarget = if (isActive) {
                val noise = Random.nextFloat() * 0.25f
                val computed = ((amplitude * 1.8f) * bellFactor) + noise
                computed.coerceIn(0.1f, 1.0f)
            } else {
                0.08f
            }

            barHeights[i].animateTo(
                targetValue = baseTarget,
                animationSpec = tween(durationMillis = 90, easing = LinearEasing)
            )
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val totalWidth = size.width
        val canvasHeight = size.height
        val barWidth = (totalWidth / (barCount * 1.8f)).coerceIn(3.dp.toPx(), 8.dp.toPx())
        val spacing = (totalWidth - (barCount * barWidth)) / (barCount - 1).coerceAtLeast(1)

        val activeBrush = Brush.verticalGradient(
            colors = if (isSpeaking) {
                listOf(JarvisPurple, JarvisCyan)
            } else {
                listOf(JarvisCyan, Color(0xFF00E676))
            }
        )

        val inactiveColor = Color(0xFF1E2D4A)

        for (i in 0 until barCount) {
            val h = barHeights[i].value * canvasHeight
            val x = i * (barWidth + spacing)
            val y = (canvasHeight - h) / 2f

            drawRoundRect(
                brush = if (isActive && barHeights[i].value > 0.12f) activeBrush else Brush.linearGradient(listOf(inactiveColor, inactiveColor)),
                topLeft = Offset(x, y),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
