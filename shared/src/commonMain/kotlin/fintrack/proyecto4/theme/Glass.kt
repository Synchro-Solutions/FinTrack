package fintrack.proyecto4.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
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
@Composable
fun Modifier.glassCard(): Modifier = this.hazeEffect(
    state = LocalHazeState.current,
    style = HazeMaterials.thin(containerColor = LocalAppColors.current.surface)
)
