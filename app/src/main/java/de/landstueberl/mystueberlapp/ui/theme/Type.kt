package de.landstueberl.mystueberlapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import de.landstueberl.mystueberlapp.R

// ── Merriweather Font Family ───────────────────
val Merriweather = FontFamily(
    Font(
        R.font.merriweather,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        R.font.merriweather,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    ),
    Font(
        R.font.merriweather,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    ),
    Font(
        R.font.merriweather_italic,
        weight = FontWeight.Normal,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    )
)

// ── Typography ─────────────────────────────────
val Typography = Typography(
    // ── Display: large figures, e.g. statistics ──
    displayLarge = TextStyle(
        fontFamily = Merriweather,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    displayMedium = TextStyle(
        fontFamily = Merriweather,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp
    ),
    displaySmall = TextStyle(
        fontFamily = Merriweather,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),

    // ── Headline: screen titles ──
    headlineLarge = TextStyle(
        fontFamily = Merriweather,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Merriweather,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Merriweather,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    ),

    // ── Title: cards, tiles, sections ──
    titleLarge = TextStyle(
        fontFamily = Merriweather,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Merriweather,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Merriweather,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp
    ),

    // ── Body: running text, values ──
    bodyLarge = TextStyle(
        fontFamily = Merriweather,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Merriweather,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Merriweather,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),

    // ── Label: buttons, chips, badges, metadata ──
    labelLarge = TextStyle(
        fontFamily = Merriweather,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Merriweather,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Merriweather,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp
    )
)