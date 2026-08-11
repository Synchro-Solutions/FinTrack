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
 * Rafaga unica de confeti al cumplir una meta: sale desde el borde izquierdo y
 * derecho (altura media) en diagonal hacia el centro/arriba, y la gravedad propia
 * del motor de ConfettiKit hace que despues caiga. No es un loop: cada Party emite
 * una sola vez (Emitter.max) y se apaga sola: basta con dejar de componer este
 * elemento (p.ej. al cerrar el dialogo de meta cumplida) para detenerlo.
 */
@Composable
fun GoalConfetti(modifier: Modifier = Modifier) {
    // Verdes de marca (FinTrackColors) + blanco y dorado de advertencia para que
    // el estallido no se vea monocromatico.
    val palette = listOf(0x62C9A7, 0x41A87F, 0x91D9C1, 0xFFFFFF, 0xFBBF24)

    val fromLeft = Party(
        angle = Angle.RIGHT - 45,
        spread = Spread.SMALL,
        speed = 12f,
        maxSpeed = 28f,
        damping = 0.9f,
        colors = palette,
        position = Position.Relative(0.0, 0.5),
        emitter = Emitter(duration = 200.milliseconds).max(60)
    )
    val fromRight = fromLeft.copy(
        angle = fromLeft.angle - 90,
        position = Position.Relative(1.0, 0.5)
    )

    ConfettiKit(
        modifier = modifier.fillMaxSize(),
        parties = listOf(fromLeft, fromRight)
    )
}
