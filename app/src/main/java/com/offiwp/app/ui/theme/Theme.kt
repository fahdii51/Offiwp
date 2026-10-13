package com.offiwp.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OffiWpDarkColorScheme = darkColorScheme(
    primary = PrimaryLightTeal,
    onPrimary = Color.Black,
    primaryContainer = PrimaryTeal,
    onPrimaryContainer = Color.White,
    secondary = AccentGreen,
    onSecondary = Color.Black,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = TextPrimaryDark,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = DividerDark
)

private val OffiWpLightColorScheme = lightColorScheme(
    primary = PrimaryTeal,
    onPrimary = Color.White,
    primaryContainer = PrimaryLightTeal,
    onPrimaryContainer = Color.Black,
    secondary = AccentGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2F1),
    onSecondaryContainer = Color(0xFF004D40),
    background = Color(0xFFF0F2F5),
    onBackground = Color(0xFF111B21),
    surface = Color.White,
    onSurface = Color(0xFF111B21),
    surfaceVariant = Color(0xFFE9EDEF),
    onSurfaceVariant = Color(0xFF667781),
    outline = Color(0xFFD1D7DB)
)

@Composable
fun OffiWpTheme(
    darkTheme: Boolean = true, // Mandatory dark theme by default as requested
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) OffiWpDarkColorScheme else OffiWpLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    OffiWpTheme(darkTheme = darkTheme, content = content)
}
