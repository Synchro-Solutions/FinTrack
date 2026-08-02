package fintrack.proyecto4.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Fondo decorativo de marca, fijo detrás de toda la navegación (no se estira con el
 * contenido de cada pantalla). En claro: gradiente radial blanco -> primary, arriba-centro,
 * inspirado en `radial-gradient(125% 125% at 50% 10%, #fff 40%, var(--primary) 100%)`, pero
 * retocado para que sature a primary completo alrededor del 75% de la altura hacia abajo
 * (así el bottom nav, que vive detrás con fondo transparente, queda sobre verde sólido).
 * En oscuro: capa completa (`inset-0`) del color primaryDark ("Glow") al 20% de opacidad y
 * blur de 100dp sobre la base sólida, inspirado en
 * `bg-neutral-900` + `absolute inset-0 bg-[glow] opacity-20 blur-[100px]`
 * (el `bg-size:20px_20px` del original no aplica: es una propiedad de imagen de fondo,
 * no tiene efecto sobre un color sólido, así que no hay nada que replicar ahí).
 */
@Composable
fun FinTrackAppBackground(colors: AppColors, modifier: Modifier = Modifier) {
    if (colors.isDark) {
        Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.primaryDark.copy(alpha = 0.2f))
                    .blur(100.dp)
            )
        }
    } else {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()
            val brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.7f to Color.White,
                    1f to colors.primary
                ),
                center = Offset(widthPx * 0.5f, heightPx * 0.1f),
                radius = maxOf(widthPx, heightPx) * 0.9f
            )
            Box(modifier = Modifier.fillMaxSize().background(brush))
        }
    }
}

/**
 * Fade de aparición por tarjeta/ítem: mientras el ítem sube desde abajo (donde vive el
 * bottom nav, transparente sobre [FinTrackAppBackground]) va apareciendo "fantasma"
 * (alpha 0 -> 1) en los últimos [fadeZoneHeight] antes de asentarse. A diferencia de una
 * máscara fija en la posición del viewport, esto es por ítem y con memoria: una vez que
 * el ítem termina de aparecer, el alpha queda clavado en 1 para siempre (no se vuelve a
 * desvanecer aunque el ítem vuelva a pasar por esa franja al hacer scroll hacia abajo).
 *
 * Usa la posición del ítem en la ventana (no necesita LazyListState ni keys): se puede
 * aplicar directo a cualquier tarjeta dentro de un LazyColumn/LazyVerticalGrid.
 */
@Composable
fun Modifier.scrollRevealFade(fadeZoneHeight: Dp = 130.dp): Modifier {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    var maxAlpha by remember { mutableFloatStateOf(0f) }
    val fadeZonePx = with(density) { fadeZoneHeight.toPx() }

    return this
        .onGloballyPositioned { coordinates ->
            val itemBottom = coordinates.boundsInWindow().bottom
            val windowHeight = windowInfo.containerSize.height.toFloat()
            val clearedAboveBottom = windowHeight - itemBottom
            val alpha = (clearedAboveBottom / fadeZonePx).coerceIn(0f, 1f)
            if (alpha > maxAlpha) maxAlpha = alpha
        }
        .graphicsLayer { alpha = maxAlpha }
}
