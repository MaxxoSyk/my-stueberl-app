package de.landstueberl.mystueberlapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = SageGreen,
    onPrimary = Color.White,
    primaryContainer = SageGreenLight,
    onPrimaryContainer = SageGreenDark,
    secondary = SageGreenDark,
    onSecondary = Color.White,
    secondaryContainer = SageGreenLight,
    onSecondaryContainer = SageGreenDark,
    tertiary = AccentGold,
    background = FloralWhite,
    onBackground = TextPrimary,
    surface = WarmBeigeCard,
    onSurface = TextPrimary,
)

private val DarkColorScheme = darkColorScheme(
    primary = SageGreenDark,
    onPrimary = Color.White,
    primaryContainer = SageGreen,
    onPrimaryContainer = Color.White,
    secondary = SageGreen,
    onSecondary = Color.White,
    secondaryContainer = SageGreenDark,
    onSecondaryContainer = Color.White,
    tertiary = AccentGold,
    background = Color(0xFF1C1C1C),
    onBackground = Color.White,
    surface = Color(0xFF2D2D2D),
    onSurface = Color.White,
)

@Composable
fun MyStueberlAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}