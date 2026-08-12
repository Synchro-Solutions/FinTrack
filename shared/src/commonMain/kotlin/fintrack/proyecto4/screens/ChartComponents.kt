package fintrack.proyecto4.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fintrack.proyecto4.dashboard.MonthlyChartData
import fintrack.proyecto4.reportes.CategoriaReporteItem
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors
import fintrack.proyecto4.theme.glassCard
import fintrack.proyecto4.theme.montserratFamily
import fintrack.proyecto4.util.formatColonesCompacto

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
    // coerceAtLeast(1f) evita dividir entre 0 más abajo (item.ingresos / maxVal): un usuario
    // sin transacciones tiene todos los meses en 0, y 0f/0f = NaN, que hace crashear la
    // animación de las barras (Animatable.animateTo no acepta NaN).
    val maxVal = (data.maxOfOrNull { maxOf(it.ingresos, it.gastos) }?.toFloat() ?: 1f).coerceAtLeast(1f)
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
    // Blindaje: si llega un valor no finito (NaN/Infinity por una división inesperada),
    // se trata como 0 para no pasar NaN a animateTo, que lanzaría IllegalStateException.
    val safeFraction = if (fraction.isFinite()) fraction else 0f
    val targetFraction = safeFraction.coerceIn(0.03f, 1f)
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

/**
 * Paleta cíclica para segmentos de categoría (donut de Reportes y Dashboard): si hay más
 * categorías que colores, se repite desde el inicio en vez de fallar o quedarse sin color.
 */
@Composable
internal fun categoriaPalette(): List<Color> = listOf(
    FinTrackColors.GreenPrimary,
    FinTrackColors.IndigoLight,
    FinTrackColors.VioletLight,
    FinTrackColors.BlueMeta,
    FinTrackColors.WarningLight,
    FinTrackColors.RedLight,
    FinTrackColors.GreenLight,
    FinTrackColors.IndigoDark
)

/** Tarjeta con donut + leyenda para un desglose de categorías (gastos/ingresos). Tocar una
 *  categoría de la leyenda navega a su historial filtrado (mismo patrón en toda la app, ver
 *  PendingCategoryFilter), en vez de un tooltip flotante que no existe como componente aquí. */
@Composable
internal fun CategoriaDonutSection(
    titulo: String,
    items: List<CategoriaReporteItem>,
    onCategoriaClick: (String) -> Unit,
    /** Mensaje mostrado cuando [items] está vacío; null (default) omite la tarjeta entera,
     *  como ya hacía Reportes. */
    emptyMessage: String? = null
) {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()

    if (items.isEmpty()) {
        if (emptyMessage == null) return
        DarkCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(titulo, color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = montserrat)
            Spacer(Modifier.height(10.dp))
            Text(emptyMessage, color = colors.textSecondary, fontSize = 12.sp, fontFamily = montserrat)
        }
        return
    }

    val palette = categoriaPalette()

    DarkCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(titulo, color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = montserrat)
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            DonutChart(
                items = items,
                colors = items.indices.map { palette[it % palette.size] },
                modifier = Modifier.size(96.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.take(5).forEachIndexed { index, item ->
                    CategoriaLegendRow(
                        item = item,
                        color = palette[index % palette.size],
                        onClick = { onCategoriaClick(item.categoria) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun DonutChart(items: List<CategoriaReporteItem>, colors: List<Color>, modifier: Modifier = Modifier) {
    val total = items.sumOf { it.monto }.coerceAtLeast(1L).toFloat()
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.24f
        var startAngle = -90f
        items.forEachIndexed { index, item ->
            val sweep = (item.monto / total) * 360f
            drawArc(
                color = colors[index],
                startAngle = startAngle,
                sweepAngle = sweep.coerceAtLeast(0f),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                size = Size(size.width - strokeWidth, size.height - strokeWidth)
            )
            startAngle += sweep
        }
    }
}

@Composable
internal fun CategoriaLegendRow(item: CategoriaReporteItem, color: Color, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(
            item.categoria,
            color = colors.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text("${formatColonesCompacto(item.monto)} · ${item.porcentaje}%", color = colors.textSecondary, fontSize = 11.sp)
    }
}
