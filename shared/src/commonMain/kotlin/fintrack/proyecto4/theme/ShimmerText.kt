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
import androidx.compose.ui.text.style.TextOverflow
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
    fontWeight: FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    // 1f = intensidad estandar. Valores mayores = banda de brillo mas ancha y barrido
    // mas rapido/frecuente, para casos como el item seleccionado del nav, que necesita
    // notarse mas obvio que el resto.
    intensity: Float = 1f
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (2600 / intensity).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )
    // Barre de derecha (+sweep) a izquierda (-sweep). La banda de acento tiene una
    // meseta (dos paradas seguidas en accentColor, no solo un punto medio) para que
    // el brillo se note con mas fuerza al pasar, y un sweep mas corto para que cruce
    // el texto mas seguido; sigue siendo sutil porque el resto del ciclo es baseColor.
    // La banda va inclinada (offset en Y ademas de X) en vez de perfectamente
    // horizontal, para que el barrido se lea como diagonal.
    val sweep = 160f
    val centerX = lerp(sweep, -sweep, progress)
    val bandHalfWidth = 95f * intensity
    val diagonalTilt = 46f
    val brush = Brush.linearGradient(
        colors = listOf(baseColor, accentColor, accentColor, baseColor),
        start = Offset(centerX - bandHalfWidth, -diagonalTilt),
        end = Offset(centerX + bandHalfWidth, diagonalTilt)
    )
    Text(
        text = text,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow,
        style = TextStyle(
            brush = brush,
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontWeight = fontWeight
        )
    )
}

/**
 * Mismo barrido animado que [ShimmerText] (mismo ritmo: 2600ms/intensity, lineal, en
 * bucle) pero devuelve el [Brush] solo, para usar en un borde (`BorderStroke(width,
 * brush = shimmerBorderBrush(...))`) en vez de en texto. Pensado para que un borde de
 * tarjeta tenga vida sin depender de hover/click, igual que el shimmer de texto.
 */
@Composable
fun shimmerBorderBrush(
    baseColor: Color,
    accentColor: Color,
    intensity: Float = 1f
): Brush {
    val transition = rememberInfiniteTransition(label = "borderShimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (2600 / intensity).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "borderShimmerProgress"
    )
    val sweep = 320f
    val centerX = lerp(sweep, -sweep, progress)
    val bandHalfWidth = 170f * intensity
    return Brush.linearGradient(
        colors = listOf(baseColor, accentColor, accentColor, baseColor),
        start = Offset(centerX - bandHalfWidth, 0f),
        end = Offset(centerX + bandHalfWidth, 0f)
    )
}
