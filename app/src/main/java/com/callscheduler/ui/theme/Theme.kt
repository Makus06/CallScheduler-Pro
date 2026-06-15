package com.callscheduler.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────
// PALETTE : Deep Space Gold
// ─────────────────────────────────────────────

object AppColors {
    // Backgrounds
    val SpaceBlack      = Color(0xFF090910)
    val SurfaceDark     = Color(0xFF10101C)
    val CardDark        = Color(0xFF16162A)
    val CardDarkAlt     = Color(0xFF1C1C30)
    val Divider         = Color(0xFF252540)

    // Gold principale
    val Gold            = Color(0xFFFFB800)
    val GoldDeep        = Color(0xFFCC9400)
    val GoldPale        = Color(0xFFFFD966)
    val GoldAlpha20     = Color(0x33FFB800)
    val GoldAlpha10     = Color(0x1AFFB800)

    // Accents
    val CyanAccent      = Color(0xFF00D4FF)
    val CyanAlpha30     = Color(0x4D00D4FF)
    val GreenSuccess    = Color(0xFF00E676)
    val GreenAlpha20    = Color(0x3300E676)
    val RedError        = Color(0xFFFF4444)
    val RedAlpha20      = Color(0x33FF4444)
    val OrangeWarning   = Color(0xFFFF8C00)
    val PurpleAccent    = Color(0xFFBB86FC)

    // Texte
    val TextPrimary     = Color(0xFFEEEEFF)
    val TextSecondary   = Color(0xFF8888AA)
    val TextMuted       = Color(0xFF555577)
    val TextOnGold      = Color(0xFF0A0A0F)

    // États
    val StatusActive    = GreenSuccess
    val StatusInactive  = Color(0xFF444466)
    val StatusCalling   = CyanAccent
    val StatusBusy      = OrangeWarning
    val StatusFailed    = RedError
}

// ─────────────────────────────────────────────
// DARK COLOR SCHEME
// ─────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary            = AppColors.Gold,
    onPrimary          = AppColors.TextOnGold,
    primaryContainer   = AppColors.GoldAlpha20,
    onPrimaryContainer = AppColors.GoldPale,
    secondary          = AppColors.CyanAccent,
    onSecondary        = AppColors.SpaceBlack,
    tertiary           = AppColors.PurpleAccent,
    background         = AppColors.SpaceBlack,
    onBackground       = AppColors.TextPrimary,
    surface            = AppColors.SurfaceDark,
    onSurface          = AppColors.TextPrimary,
    surfaceVariant     = AppColors.CardDark,
    onSurfaceVariant   = AppColors.TextSecondary,
    error              = AppColors.RedError,
    onError            = AppColors.TextPrimary,
    outline            = AppColors.Divider,
    outlineVariant     = AppColors.GoldAlpha10,
)

// ─────────────────────────────────────────────
// TYPOGRAPHY
// ─────────────────────────────────────────────

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 48.sp,
        letterSpacing = (-2).sp,
        color = AppColors.TextPrimary
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        letterSpacing = (-1).sp,
        color = AppColors.TextPrimary
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        letterSpacing = 0.sp,
        color = AppColors.TextPrimary
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        letterSpacing = 0.sp,
        color = AppColors.TextPrimary
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.3.sp,
        color = AppColors.TextPrimary
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp,
        color = AppColors.TextPrimary
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.15.sp,
        color = AppColors.TextSecondary
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.2.sp,
        color = AppColors.TextMuted
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 1.sp,
        color = AppColors.Gold
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
        color = AppColors.TextSecondary
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.8.sp,
        color = AppColors.TextMuted
    ),
)

// ─────────────────────────────────────────────
// THEME COMPOSABLE
// ─────────────────────────────────────────────

@Composable
fun CallSchedulerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
