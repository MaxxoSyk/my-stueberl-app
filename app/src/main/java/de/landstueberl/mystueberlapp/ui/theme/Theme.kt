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
    onTertiary = Color.White,
    background = WarmBeige,
    onBackground = TextPrimary,
    surface = FloralWhite,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    surfaceVariant = Color(0xFFECE3D6),
    outline = Color(0xFFB4B2AE),            // ≈ TextSecondary at 50 % on FloralWhite, as in your fields
    outlineVariant = Color(0xFFE0D8CC),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = FloralWhite,
    surfaceContainer = FloralWhite,
    surfaceContainerHigh = FloralWhite,
    surfaceContainerHighest = Color(0xFFF7EFE4),
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
    tertiary = AccentGoldDark,
    onTertiary = Color.Black,
    background = Color(0xFF1C1C1C),
    onBackground = Color.White,
    surface = Color(0xFF2D2D2D),
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFBDBDBD),
    surfaceVariant = Color(0xFF3A3A3A),
    outline = Color(0xFF6B6B6B),
    outlineVariant = Color(0xFF444444),
    surfaceContainerLowest = Color(0xFF161616),
    surfaceContainerLow = Color(0xFF222222),
    surfaceContainer = Color(0xFF262626),
    surfaceContainerHigh = Color(0xFF2D2D2D),
    surfaceContainerHighest = Color(0xFF363636),
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