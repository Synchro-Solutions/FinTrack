package fintrack.proyecto4.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fintrack.proyecto4.dashboard.MonthlyChartData
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors
import fintrack.proyecto4.theme.glassCard
import fintrack.proyecto4.theme.montserratFamily

/**
 * Contenedor tipo "vidrio esmerilado" usado por las secciones del Dashboard y Reportes.
 */
@Composable
internal fun DarkCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .glassCard()
            .padding(18.dp)
    ) {
        Column(content = content)
    }
}

@Composable
internal fun LegendDot(color: Color, label: String) {
    val colors = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(5.dp))
        Text(label, color = colors.textSecondary, fontSize = 11.sp, fontFamily = montserratFamily())
    }
}

@Composable
internal fun BarChart(data: List<MonthlyChartData>) {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()
    val maxVal = data.maxOfOrNull { maxOf(it.ingresos, it.gastos) }?.toFloat() ?: 1f
    val maxH = 90.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEachIndexed { index, item ->
            // Stagger por mes: cada columna arranca un poco despues que la anterior,
            // para que el crecimiento se lea de izquierda a derecha en vez de todas
            // las barras subiendo a la vez.
            val barDelay = index * 70
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.height(maxH)
                ) {
                    GradientBar(fraction = item.ingresos / maxVal, width = 11.dp, brush = FinTrackColors.GradientGreenV, delayMillis = barDelay)
                    GradientBar(fraction = item.gastos / maxVal, width = 11.dp, brush = FinTrackColors.GradientRedV, delayMillis = barDelay + 60)
                }
                Spacer(Modifier.height(6.dp))
                Text(item.mes, color = colors.textSecondary, fontSize = 10.sp, fontFamily = montserrat)
            }
        }
    }
}

@Composable
internal fun GradientBar(fraction: Float, width: Dp, brush: Brush, delayMillis: Int = 0) {
    val targetFraction = fraction.coerceIn(0.03f, 1f)
    val animatedFraction = remember { Animatable(0f) }
    LaunchedEffect(targetFraction) {
        animatedFraction.animateTo(
            targetValue = targetFraction,
            animationSpec = tween(durationMillis = 650, delayMillis = delayMillis, easing = FastOutSlowInEasing)
        )
    }
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight(animatedFraction.value.coerceIn(0.001f, 1f))
            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
            .background(brush)
    )
}
