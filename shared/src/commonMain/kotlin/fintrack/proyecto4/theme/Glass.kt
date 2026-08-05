package fintrack.proyecto4.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * Estado de Haze compartido entre el fondo decorativo del app shell (la fuente que se
 * difumina, ver [FinTrackAppBackground] marcado con `hazeSource` en App.kt) y las
 * tarjetas que piden verlo esmerilado detrás.
 */
val LocalHazeState = staticCompositionLocalOf<HazeState> {
    error("HazeState no inicializado. Debe proveerse desde App.kt junto con FinTrackAppBackground.")
}

/**
 * Vidrio esmerilado real (blur del fondo detrás de la tarjeta vía Haze) en vez de una
 * superficie plana semitranslúcida: evita el efecto "lechoso"/inconsistente de un color
 * sólido con alpha sobre un fondo que cambia mucho de tono (el degradado del app shell
 * pasa de blanco a verde saturado).
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun Modifier.glassCard(): Modifier = this.hazeEffect(
    state = LocalHazeState.current,
    style = HazeMaterials.thin(containerColor = LocalAppColors.current.surface)
)

/**
 * Igual que [glassCard], pensado para la bottom nav: la única superficie fija (no una
 * tarjeta) que hoy pide Haze.
 *
 * Usa [HazeMaterials.ultraThin] (menos tinte, más transparencia real) en vez de [thin]
 * como [glassCard]: en una tarjeta el tinte de `thin` ayuda a que el texto tenga contraste,
 * pero en la nav ese mismo tinte se leía como una superficie opaca pintada encima, no como
 * vidrio. Blur bajo (16dp, menos que el 24dp por defecto): mucho blur se ve como niebla
 * espesa; poco blur es lo que da el aspecto de vidrio fino/liso, dejando reconocible lo
 * que hay detrás en vez de disolverlo.
 *
 * OJO: se probó agregar un fade progresivo (`HazeProgressive.verticalGradient`, intensidad
 * de blur de 0 a 1 de arriba a abajo) para que la niebla "naciera" suave en el borde de
 * arriba en vez de cortar duro. Se sacó: dejaba un parche blanco liso justo en la franja de
 * intensidad baja (el fallback/relleno de Haze ahí no estaba mostrando el fondo real). Si se
 * quiere retomar el fade, mejor con un `mask` (Brush.verticalGradient simple) en vez de
 * `progressive`, que es la vía más nueva/experimental de la librería.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun Modifier.glassNav(): Modifier = this.hazeEffect(
    state = LocalHazeState.current,
    style = HazeMaterials.ultraThin(containerColor = LocalAppColors.current.surface)
        .copy(blurRadius = 16.dp)
)
