package com.cockroachcommunity.app.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    secondary = Gold,
    onSecondary = Ink,
    tertiary = Orange,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = PaperSoft,
    onSurfaceVariant = InkMuted,
    outline = Ink,
    outlineVariant = Ink.copy(alpha = 0.25f),
    error = Red,
    onError = Paper
)

private val DarkColors = darkColorScheme(
    primary = Gold,
    onPrimary = Ink,
    secondary = Gold,
    onSecondary = Ink,
    tertiary = Orange,
    background = Ink,
    onBackground = Paper,
    surface = Ink,
    onSurface = Paper,
    surfaceVariant = InkSoft,
    onSurfaceVariant = PaperMuted,
    outline = Paper,
    outlineVariant = Paper.copy(alpha = 0.25f),
    error = Color(0xFFEF4444),
    onError = Ink
)

@Composable
fun CockroachCommunityTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = CjpTypography,
        shapes = CjpShapes,
        content = content
    )
}
