package fintrack.proyecto4.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.util.lerp
import androidx.compose.material3.Text

/**
 * Texto con un brillo (shimmer) que barre de derecha a izquierda, en bucle continuo
 * (no depende de hover/click). Tiñe momentáneamente el interior de las letras con
 * [accentColor] sin cambiar tamaño ni negrita; el resto del tiempo se ve [baseColor]
 * sólido, para no perder legibilidad. Pensado para placeholders/hints, no para texto
 * que el usuario está escribiendo.
 */
@Composable
fun ShimmerText(
    text: String,
    baseColor: Color,
    accentColor: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily? = null,
    fontWeight: FontWeight? = null
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )
    // Barre de derecha (+sweep) a izquierda (-sweep); la banda de acento es angosta
    // frente a las zonas solidas en baseColor a los lados, para que predomine el blanco.
    val sweep = 220f
    val centerX = lerp(sweep, -sweep, progress)
    val brush = Brush.linearGradient(
        colors = listOf(baseColor, baseColor, accentColor, baseColor, baseColor),
        start = Offset(centerX - 70f, 0f),
        end = Offset(centerX + 70f, 0f)
    )
    Text(
        text = text,
        modifier = modifier,
        style = TextStyle(
            brush = brush,
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontWeight = fontWeight
        )
    )
}
