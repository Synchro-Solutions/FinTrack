package fintrack.proyecto4.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object FinTrackColors {

    /* Verdes */
    val GreenPrimary = Color(0xFF22C55E)
    val GreenDark    = Color(0xFF15803D)
    val GreenLight   = Color(0xFF4ADE80)

    /* Blancos */
    val White        = Color(0xFFFFFFFF)
    val WhiteSoft    = Color(0xFFF0FDF4)
    val WhiteAlpha70 = Color(0xB3FFFFFF)
    val WhiteAlpha40 = Color(0x66FFFFFF)
    val WhiteAlpha10 = Color(0x1AFFFFFF)

    /* Fondos y superficies */
    val BgApp            = Color(0xFF080E1A)   // fondo global de la app
    val SurfacePrimary   = Color(0xFF111827)   // cards principales
    val SurfaceSecondary = Color(0xFF1A2332)   // cards secundarios

    val CardBackground   = Color(0x80000000)   // negro 50%
    val OverlayDark      = Color(0xCC000000)
    val OverlayGreenMid  = Color(0xB3001A0A)
    val OverlayDarkBottom = Color(0xF2000000)

    /* Texto */
    val TextPrimary   = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFF64748B)
    val TextTertiary  = Color(0xFF334155)

    /* Estados */
    val ErrorColor   = Color(0xFFFF6B6B)
    val SuccessColor = GreenPrimary
    val WarningColor = Color(0xFFF59E0B)
    val WarningLight = Color(0xFFFBBF24)
    val WarningText  = Color(0xFFFDE68A)

    /* Bordes / divisores */
    val BorderDefault = Color(0x33FFFFFF)      // blanco 20%
    val BorderFocused = GreenPrimary
    val DividerColor  = Color(0xFF1E293B)

    /* Categorías (acciones rápidas / gráficas) */
    val IndigoLight  = Color(0xFF818CF8)
    val IndigoDark   = Color(0xFF4F46E5)
    val VioletLight  = Color(0xFFA78BFA)
    val VioletDark   = Color(0xFF7C3AED)
    val RedLight     = Color(0xFFFCA5A5)
    val BlueMetaDark = Color(0xFF1E3A8A)
    val BlueMeta     = Color(0xFF3B82F6)

    /* Advertencia / consejo (ambar) */
    val AmberDark = Color(0xFF78350F)
    val AmberMid  = Color(0xFF92400E)

    /* Degradados reutilizables */
    val GradientGreen  get() = Brush.linearGradient(listOf(GreenDark, GreenPrimary))
    val GradientGreenV get() = Brush.verticalGradient(listOf(GreenLight, GreenPrimary))
    val GradientRed    get() = Brush.linearGradient(listOf(Color(0xFFDC2626), ErrorColor))
    val GradientRedV   get() = Brush.verticalGradient(listOf(RedLight, ErrorColor))
    val GradientIndigo get() = Brush.linearGradient(listOf(IndigoDark, IndigoLight))
    val GradientViolet get() = Brush.linearGradient(listOf(VioletDark, VioletLight))
    val GradientBalance get() = Brush.linearGradient(
        0f to Color(0xFF064E3B),
        0.5f to Color(0xFF065F46),
        1f to Color(0xFF047857)
    )
    val GradientAmber get() = Brush.linearGradient(
        0f to AmberDark.copy(alpha = 0.5f),
        1f to AmberMid.copy(alpha = 0.3f)
    )
    val GradientMeta get() = Brush.linearGradient(listOf(BlueMetaDark, BlueMeta))
}
