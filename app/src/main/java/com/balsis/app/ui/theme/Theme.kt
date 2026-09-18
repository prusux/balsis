package com.balsis.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BalsisPrimary = Color(0xFF10B981) // Emerald green
val BalsisPrimaryDark = Color(0xFF059669)
val BalsisSecondary = Color(0xFF0EA5E9) // Sky blue
val BalsisBackgroundDark = Color(0xFF121417)
val BalsisSurfaceDark = Color(0xFF1E2228)
val BalsisCardDark = Color(0xFF282D35)

private val DarkColorScheme = darkColorScheme(
    primary = BalsisPrimary,
    secondary = BalsisSecondary,
    background = BalsisBackgroundDark,
    surface = BalsisSurfaceDark,
    surfaceVariant = BalsisCardDark
)

private val LightColorScheme = lightColorScheme(
    primary = BalsisPrimaryDark,
    secondary = BalsisSecondary,
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F5F9)
)

@Composable
fun BalsisTheme(
    darkTheme: Boolean = true, // Default to sleek dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
