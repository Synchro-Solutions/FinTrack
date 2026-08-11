package fintrack.proyecto4.savings.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.vinceglb.confettikit.compose.ConfettiKit
import io.github.vinceglb.confettikit.core.Angle
import io.github.vinceglb.confettikit.core.Party
import io.github.vinceglb.confettikit.core.Position
import io.github.vinceglb.confettikit.core.Spread
import io.github.vinceglb.confettikit.core.emitter.Emitter
import kotlin.time.Duration.Companion.milliseconds

/**
 * Confeti al cumplir una meta: cae desde todo el borde superior de la pantalla (no desde
 * los lados) simulando lluvia, con spread amplio para que se reparta a lo ancho en vez de
 * caer en una sola columna. No es un loop: el Party emite durante [Emitter.duration] y
 * se apaga sola; el llamador controla cuanto dura visible en pantalla dejando de componer
 * este elemento (la fisica de caida de ConfettiKit sigue un rato mas tras el ultimo
 * emitido, por eso el llamador le da un poco mas de tiempo montado que la duracion de
 * emision antes de desmontarlo).
 */
@Composable
fun GoalConfetti(modifier: Modifier = Modifier) {
    // Verdes de marca (FinTrackColors) + blanco y dorado de advertencia para que
    // la lluvia no se vea monocromatica.
    val palette = listOf(0x62C9A7, 0x41A87F, 0x91D9C1, 0xFFFFFF, 0xFBBF24)

    val fromTop = Party(
        angle = Angle.BOTTOM,
        spread = 100,
        speed = 4f,
        maxSpeed = 10f,
        damping = 0.9f,
        colors = palette,
        position = Position.Relative(0.0, 0.0).between(Position.Relative(1.0, 0.0)),
        emitter = Emitter(duration = 700.milliseconds).max(140)
    )

    ConfettiKit(
        modifier = modifier.fillMaxSize(),
        parties = listOf(fromTop)
    )
}
