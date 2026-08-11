package fintrack.proyecto4.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Fondo decorativo de marca, fijo detrás de toda la navegación (no se estira con el
 * contenido de cada pantalla). En claro: gradiente radial blanco -> primary, arriba-centro,
 * inspirado en `radial-gradient(125% 125% at 50% 10%, #fff 40%, var(--primary) 100%)`, pero
 * retocado para que sature a primary completo alrededor del 75% de la altura hacia abajo
 * (así el bottom nav, que vive detrás con fondo transparente, queda sobre verde sólido).
 * En oscuro: partículas chicas (círculos lisos, sin blur, como las de la tarjeta de
 * saldo) que flotan lento sobre la base sólida, con el verde vivo del tema claro (no el
 * más apagado propio del oscuro) para que tenga personalidad en vez de leerse como un
 * fondo plano sin vida.
 */
@Composable
fun FinTrackAppBackground(colors: AppColors, modifier: Modifier = Modifier) {
    if (colors.isDark) {
        BoxWithConstraints(modifier = modifier.fillMaxSize().background(colors.bg)) {
            val infiniteTransition = rememberInfiniteTransition(label = "darkBgParticles")
            val phase by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = (2 * kotlin.math.PI).toFloat(),
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 30_000, easing = LinearEasing)
                ),
                label = "particlePhase"
            )
            val particleColor = LightAppColors.primary
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                DarkBgParticles.forEach { p ->
                    val driftPx = p.driftDp.toPx()
                    val cx = p.x * w + sin(phase + p.phaseOffset) * driftPx
                    val cy = p.y * h + cos(phase * 0.7f + p.phaseOffset) * driftPx
                    drawCircle(
                        color = particleColor.copy(alpha = p.alpha),
                        radius = p.radiusDp.toPx(),
                        center = Offset(cx, cy)
                    )
                }
            }
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

/** Una partícula del fondo oscuro: posición base como fracción de ancho/alto (0..1). */
private data class BgParticle(
    val x: Float,
    val y: Float,
    val radiusDp: Dp,
    val driftDp: Dp,
    val phaseOffset: Float,
    val alpha: Float
)

// Repartidas en la mitad de arriba de la pantalla (nunca abajo, cerca del nav, para no
// competir con las tarjetas que scrollean ahi). Radios y drift chicos: son particulas,
// no blobs — se notan por cantidad y movimiento, no por tamano.
private val DarkBgParticles = listOf(
    BgParticle(0.15f, 0.08f, 4.dp, 16.dp, 0.0f, 0.16f),
    BgParticle(0.38f, 0.14f, 3.dp, 12.dp, 0.9f, 0.20f),
    BgParticle(0.58f, 0.05f, 5.dp, 18.dp, 1.7f, 0.14f),
    BgParticle(0.78f, 0.17f, 3.dp, 14.dp, 2.5f, 0.18f),
    BgParticle(0.92f, 0.09f, 4.dp, 15.dp, 3.3f, 0.15f),
    BgParticle(0.08f, 0.26f, 3.dp, 10.dp, 4.1f, 0.12f),
    BgParticle(0.28f, 0.32f, 5.dp, 20.dp, 4.9f, 0.16f),
    BgParticle(0.50f, 0.22f, 3.dp, 13.dp, 0.5f, 0.19f),
    BgParticle(0.68f, 0.30f, 4.dp, 17.dp, 1.3f, 0.13f),
    BgParticle(0.85f, 0.24f, 3.dp, 11.dp, 2.1f, 0.17f),
    BgParticle(0.20f, 0.42f, 4.dp, 19.dp, 2.9f, 0.11f),
    BgParticle(0.62f, 0.40f, 3.dp, 12.dp, 3.7f, 0.15f)
)

/**
 * Fade de aparición por tarjeta/ítem: mientras el ítem sube desde abajo (donde vive el
 * bottom nav, transparente sobre [FinTrackAppBackground]) queda completamente tapado por
 * un velo de [veilColor] (por defecto el fondo de la página: blanco en claro, oscuro en
 * oscuro) hasta que el ítem entero — usando su propio alto medido como piso, no un valor
 * fijo, para que una tarjeta alta no cuente como "afuera" antes de tiempo — tuvo lugar
 * para asentarse por encima de esa franja. Recién ahí el velo se retira, de una, con un
 * fade corto y desacoplado del scroll (no gradual mientras se mueve).
 *
 * A propósito NO es un fundido continuo atado a la posición de scroll: eso deja al ítem
 * en un estado "fantasma" (ni tapado ni completo) durante el tramo en que su borde está
 * cerca del límite de arriba del nav, y ese estado intermedio es lo que se lee como si el
 * nav "cortara" al componente que está saliendo. Al ser binario (tapado del todo -> visible
 * del todo, sin punto medio ligado al scroll) nunca hay nada que cortar: no importa qué tan
 * lento se haga scroll, no hay un instante donde se vea la tarjeta a medias justo en esa
 * línea.
 *
 * El contenido de la tarjeta siempre se dibuja opaco (no se anima su alpha, que dejaría ver
 * el degradado de fondo a través mientras aparece); lo único que se anima es el velo. Es
 * por ítem y con memoria: una vez que se revela, queda así para siempre.
 *
 * Usa la posición del ítem en la ventana (no necesita LazyListState ni keys): se puede
 * aplicar directo a cualquier tarjeta dentro de un LazyColumn/LazyVerticalGrid.
 */
@Composable
fun Modifier.scrollRevealFade(
    minFadeZoneHeight: Dp = 130.dp,
    veilColor: Color = LocalAppColors.current.bg
): Modifier {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    var isFullyClear by remember { mutableStateOf(false) }
    val minFadeZonePx = with(density) { minFadeZoneHeight.toPx() }

    val veilAlpha by animateFloatAsState(
        targetValue = if (isFullyClear) 0f else 1f,
        animationSpec = tween(220),
        label = "scrollRevealVeil"
    )

    return this
        .onGloballyPositioned { coordinates ->
            if (isFullyClear) return@onGloballyPositioned
            val itemBottom = coordinates.boundsInWindow().bottom
            val windowHeight = windowInfo.containerSize.height.toFloat()
            val clearedAboveBottom = windowHeight - itemBottom
            val fadeZonePx = maxOf(minFadeZonePx, coordinates.size.height.toFloat())
            if (clearedAboveBottom >= fadeZonePx) isFullyClear = true
        }
        .drawWithContent {
            drawContent()
            if (veilAlpha > 0f) {
                drawRect(color = veilColor, alpha = veilAlpha)
            }
        }
}
