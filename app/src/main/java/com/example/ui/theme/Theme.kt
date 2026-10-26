package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = YardGreen,
    onPrimary = Color(0xFF003816),
    primaryContainer = YardGreenContainer,
    onPrimaryContainer = YardGreenText,

    secondary = YardAmber,
    onSecondary = Color(0xFF3E2600),
    secondaryContainer = YardAmberContainer,
    onSecondaryContainer = Color(0xFFFFE082),

    tertiary = YardCyan,
    onTertiary = Color(0xFF00363F),
    tertiaryContainer = YardCyanContainer,
    onTertiaryContainer = Color(0xFF80DEEA),

    error = YardRed,
    onError = Color.White,
    errorContainer = YardRedContainer,
    onErrorContainer = Color(0xFFFFCDD2),

    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = Color(0xFF384351)
)

private val LightColorScheme = DarkColorScheme // Court / night parking looks best in immersive dark palette

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
