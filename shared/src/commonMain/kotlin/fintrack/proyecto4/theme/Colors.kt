package fintrack.proyecto4.theme

import androidx.compose.ui.graphics.Color

object FinTrackColors {

    // Verdes
    val GreenPrimary  = Color(0xFF22C55E)
    val GreenDark     = Color(0xFF15803D)
    val GreenLight    = Color(0xFF4ADE80)

    // Blancos
    val White         = Color(0xFFFFFFFF)
    val WhiteSoft     = Color(0xFFF0FDF4)
    val WhiteAlpha70  = Color(0xB3FFFFFF)
    val WhiteAlpha40  = Color(0x66FFFFFF)
    val WhiteAlpha10  = Color(0x1AFFFFFF)

    // Fondos
    val CardBackground      = Color(0x80000000)   // negro 50% opaco
    val OverlayDark         = Color(0xCC000000)
    val OverlayGreenMid     = Color(0xB3001A0A)
    val OverlayDarkBottom   = Color(0xF2000000)

    // Estados
    val ErrorColor    = Color(0xFFFF6B6B)
    val SuccessColor  = GreenPrimary

    // Bordes
    val BorderDefault = Color(0x33FFFFFF)         // blanco 20%
    val BorderFocused = GreenPrimary
}
