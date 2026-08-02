package fintrack.proyecto4.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/**
 * Fondo decorativo de marca, fijo detrás de toda la navegación (no se estira con el
 * contenido de cada pantalla). En claro: gradiente radial blanco -> primary, arriba-centro,
 * inspirado en `radial-gradient(125% 125% at 50% 10%, #fff 40%, var(--primary) 100%)`, pero
 * retocado para que sature a primary completo alrededor del 75% de la altura hacia abajo
 * (así el bottom nav, que vive detrás con fondo transparente, queda sobre verde sólido).
 * En oscuro: base sólida + un halo difuminado del color primaryDark ("Glow").
 */
@Composable
fun FinTrackAppBackground(colors: AppColors, modifier: Modifier = Modifier) {
    if (colors.isDark) {
        Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-120).dp)
                    .size(420.dp)
                    .background(colors.primaryDark.copy(alpha = 0.35f), CircleShape)
                    .blur(100.dp)
            )
        }
    } else {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()
            val brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.7f to androidx.compose.ui.graphics.Color.White,
                    1f to colors.primary
                ),
                center = Offset(widthPx * 0.5f, heightPx * 0.1f),
                radius = maxOf(widthPx, heightPx) * 0.9f
            )
            Box(modifier = Modifier.fillMaxSize().background(brush))
        }
    }
}
