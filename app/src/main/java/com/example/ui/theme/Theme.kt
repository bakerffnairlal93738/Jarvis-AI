package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisColorScheme = darkColorScheme(
    primary = JarvisCyan,
    onPrimary = JarvisObsidian,
    primaryContainer = JarvisCardBg,
    onPrimaryContainer = JarvisCyan,
    secondary = JarvisElectricBlue,
    onSecondary = Color.White,
    tertiary = JarvisPurple,
    onTertiary = Color.White,
    background = JarvisObsidian,
    onBackground = JarvisTextPrimary,
    surface = JarvisDarkNavy,
    onSurface = JarvisTextPrimary,
    surfaceVariant = JarvisCardBg,
    onSurfaceVariant = JarvisTextSecondary,
    outline = JarvisCardBorder,
    error = JarvisCrimson,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = Typography,
        content = content
    )
}
