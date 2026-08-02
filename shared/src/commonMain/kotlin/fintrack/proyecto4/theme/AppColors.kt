package fintrack.proyecto4.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColors(
    val bg: Color,
    val surface: Color,
    val surfaceSecondary: Color,
    /** Verde principal de marca, distinto por tema (claro #3B8F74 / oscuro #62C9A7). */
    val primary: Color,
    /** Sombra del primary: color de botones en claro, color de glow en oscuro. */
    val primaryDark: Color,
    /** Tinte claro del primary, derivado (no especificado en la paleta original). */
    val primaryLight: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val border: Color,
    val navBar: Color,
    val isDark: Boolean
)

val DarkAppColors = AppColors(
    bg               = Color(0xFF081C19),
    surface          = Color(0xFF102C26),
    surfaceSecondary = Color(0xFF15352E),
    primary          = Color(0xFF62C9A7),
    primaryDark      = Color(0xFF41A87F),
    primaryLight     = Color(0xFF91D9C1),
    textPrimary      = Color(0xFFE7F4EE),
    textSecondary    = Color(0xFF8FB3A9),
    divider          = Color(0xFF1E3A33),
    border           = Color(0x33E7F4EE),
    navBar           = Color(0xFF102C26),
    isDark           = true
)

val LightAppColors = AppColors(
    bg               = Color(0xFFF4F7F2),
    surface          = Color(0xFFF8FAF7),
    surfaceSecondary = Color(0xFFEAF0E9),
    primary          = Color(0xFF3B8F74),
    primaryDark      = Color(0xFF26705B),
    primaryLight     = Color(0xFF80B6A5),
    textPrimary      = Color(0xFF102721),
    textSecondary    = Color(0xFF4B6B62),
    divider          = Color(0xFFDCE6DA),
    border           = Color(0xFFCBD9CE),
    navBar           = Color(0xFFF8FAF7),
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

/**
 * Superficie translúcida (glassmorphism ligero) para tarjetas que viven sobre el
 * fondo decorativo del app shell (FinTrackAppBackground): deja pasar un poco de ese
 * fondo en vez de taparlo con un panel opaco. No usar en diálogos/menús/dropdowns,
 * que necesitan quedar opacos para legibilidad sobre cualquier contenido detrás.
 */
val AppColors.glassSurface: Color
    get() = surface.copy(alpha = if (isDark) 0.6f else 0.85f)

/** Borde sutil a juego con [glassSurface], refuerza el efecto de vidrio. */
val AppColors.glassBorder: Color
    get() = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.5f)
