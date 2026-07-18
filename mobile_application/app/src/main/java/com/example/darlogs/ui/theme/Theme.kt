package com.example.darlogs.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val BrandGreen = Color(0xFF1E5F35)
val BrandDeep = Color(0xFF144326)
val BrandGreenDark = Color(0xFF0E3D1E)
val AccentGold = Color(0xFFD3AC36)
val Background = Color(0xFF081A11)
val Surface = Color(0xFF142F1F)
val SurfaceSecondary = Color(0xFF173B28)
val TextDark = Color(0xFFE8F7E6)
val TextMuted = Color(0xFFB9D8B6)
val LineColor = Color(0xFF2C5F3E)
val DangerRed = Color(0xFFC93030)
val SuccessGreen = Color(0xFF2CC56A)

// Softer functional colors for Light Mode
val SuccessGreenSoft = Color(0xFF4DB6AC)
val DangerRedSoft = Color(0xFFE57373)

val BackgroundDark = Color(0xFF04110D)
val SurfaceDark = Color(0xFF0A2E1B)
val SurfaceSecondaryDark = Color(0xFF0D3A24)
val TextLight = Color(0xFFE8F7E6)
val TextMutedDark = Color(0xFF9BC8A4)
val LineColorDark = Color(0xFF1C472D)

val PrimaryHeaderGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF113522), Color(0xFF06220E))
)

val PrimaryHeaderDarkGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF0C4622), Color(0xFF04110D))
)

val DarDarkColorScheme = darkColorScheme(
    primary = Color(0xFF1E6F36),
    onPrimary = Surface,
    primaryContainer = Color(0xFF104B24),
    onPrimaryContainer = Surface,
    secondary = AccentGold,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceSecondaryDark,
    onSurface = TextLight,
    onSurfaceVariant = TextMutedDark,
    outline = LineColorDark,
    error = DangerRed
)

val BackgroundLight = Color(0xFFF2F5F2) // Very light minty background
val SurfaceLight = Color(0xFFFFFFFF) // White for cards
val SurfaceSecondaryLight = Color(0xFFE8EDE9) // Light mint for secondary surfaces
val TextDarkLight = Color(0xFF1E2621) // Soft forest charcoal for text
val TextMutedLight = Color(0xFF637368) // Muted sage for labels
val LineColorLight = Color(0xFFDCE3DE) // Subtle divider

val PrimaryHeaderLightGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF2F7A4D), Color(0xFF1A5A35))
)

val PrimaryFooterLightGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF2F7A4D), Color(0xFF1A5A35))
)

val NavigationBarUnderlineLight = Color(0xFFF2F4F7)

val DarLightColorScheme = lightColorScheme(
    primary = Color(0xFF2D6A4F), // Professional emerald
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E2DC),
    onPrimaryContainer = Color(0xFF1B4332),
    secondary = Color(0xFFB79631), // Muted gold for accents
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceSecondaryLight,
    onSurface = TextDarkLight,
    onSurfaceVariant = TextMutedLight,
    outline = LineColorLight,
    error = DangerRedSoft
)
