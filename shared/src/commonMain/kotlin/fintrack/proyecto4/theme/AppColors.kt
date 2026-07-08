package fintrack.proyecto4.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColors(
    val bg: Color,
    val surface: Color,
    val surfaceSecondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val border: Color,
    val navBar: Color,
    val isDark: Boolean
)

val DarkAppColors = AppColors(
    bg               = Color(0xFF080E1A),
    surface          = Color(0xFF111827),
    surfaceSecondary = Color(0xFF1A2332),
    textPrimary      = Color(0xFFF1F5F9),
    textSecondary    = Color(0xFF64748B),
    divider          = Color(0xFF1E293B),
    border           = Color(0x33FFFFFF),
    navBar           = Color(0xFF0F1923),
    isDark           = true
)

val LightAppColors = AppColors(
    bg               = Color(0xFFF1F5F9),
    surface          = Color(0xFFFFFFFF),
    surfaceSecondary = Color(0xFFEFF4F8),
    textPrimary      = Color(0xFF0F172A),
    textSecondary    = Color(0xFF64748B),
    divider          = Color(0xFFE2E8F0),
    border           = Color(0xFFCBD5E1),
    navBar           = Color(0xFFFFFFFF),
    isDark           = false
)

val LocalAppColors = compositionLocalOf { DarkAppColors }

/**
 * Superficie "gris" con más contraste que [AppColors.surfaceSecondary] frente a [AppColors.bg].
 * En modo oscuro ambos ya se distinguen bien, pero en modo claro son casi idénticos
 * (surfaceSecondary 0xFFEFF4F8 vs bg 0xFFF1F5F9), por lo que campos de texto y badges se
 * veían "planos"/invisibles. Fix temporal: en modo claro usa el mismo gris ya usado por las
 * chips de categoría/método de pago (0xFFE1E7F0), que sí tiene contraste notorio.
 */
val AppColors.subtleSurface: Color
    get() = if (isDark) surfaceSecondary else Color(0xFFE1E7F0)
