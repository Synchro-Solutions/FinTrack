package fintrack.proyecto4.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fintrack.proyecto4.auth.AuthClient
import fintrack.proyecto4.budget.BudgetItem
import fintrack.proyecto4.budget.BudgetRepository
import fintrack.proyecto4.budget.BudgetStatus
import fintrack.proyecto4.budget.NoOpBudgetRepository
import fintrack.proyecto4.dashboard.KpiData
import fintrack.proyecto4.dashboard.MetaItem
import fintrack.proyecto4.dashboard.MonthlyChartData
import fintrack.proyecto4.reportes.CategoriaReporteItem
import fintrack.proyecto4.reportes.MetodoPagoReporteItem
import fintrack.proyecto4.reportes.ReportePeriodo
import fintrack.proyecto4.reportes.ReportesUiState
import fintrack.proyecto4.reportes.ReportesViewModel
import fintrack.proyecto4.theme.FinTrackColors
import fintrack.proyecto4.theme.LocalAppColors
import fintrack.proyecto4.theme.ShimmerText
import fintrack.proyecto4.theme.glassCard
import fintrack.proyecto4.theme.montserratFamily
import fintrack.proyecto4.transaction.NoOpTransactionRepository
import fintrack.proyecto4.transaction.TransactionRepository
import fintrack.proyecto4.util.formatColones
import fintrack.proyecto4.util.formatColonesCompacto

@Composable
fun ReportesScreen(
    transactionRepository: TransactionRepository = NoOpTransactionRepository(),
    budgetRepository: BudgetRepository = NoOpBudgetRepository(),
    onBack: () -> Unit = {},
    onShareText: (String) -> Unit = {},
    onVerCategoria: (String) -> Unit = {}
) {
    val colors = LocalAppColors.current
    val uid = AuthClient.currentUserId() ?: ""
    val viewModel = viewModel(key = "reportes_$uid") {
        ReportesViewModel(transactionRepository, uid, budgetRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(24.dp).clickable(onClick = onBack)
                    )
                    // Reusan el mismo onShareText que ya usa el Dashboard para su consejo
                    // financiero — no hay generación de PDF (requeriría una librería nueva
                    // multiplataforma que hoy no existe en el proyecto); el CSV va como texto
                    // compartible, que ya cubre pegar en Excel/Sheets o enviarlo por cualquier vía.
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Exportar CSV",
                            tint = FinTrackColors.GreenPrimary,
                            modifier = Modifier.size(20.dp).clickable { onShareText(buildCsv(state)) }
                        )
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartir reporte",
                            tint = FinTrackColors.GreenPrimary,
                            modifier = Modifier.size(20.dp).clickable { onShareText(buildResumenTexto(state)) }
                        )
                    }
                }
            }

            item {
                ShimmerText(
                    text = "Reportes",
                    baseColor = colors.textPrimary,
                    accentColor = colors.primary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            item {
                PeriodoSelector(
                    seleccionado = state.periodoSeleccionado,
                    onSeleccionar = { viewModel.onPeriodoSeleccionado(it) }
                )
            }

            if (!state.isLoading) {
                if (!state.hasMovimientosEnPeriodo) {
                    item {
                        EmptyReportesState(
                            modifier = Modifier.fillParentMaxWidth().padding(top = 40.dp)
                        )
                    }
                } else {
                    item {
                        KpiSummaryRow(
                            kpis = state.kpis,
                            comparacionIngresosPercent = state.comparacionIngresosPercent,
                            comparacionGastosPercent = state.comparacionGastosPercent
                        )
                    }

                    if (state.chartData.isNotEmpty()) {
                        item {
                            TendenciaSection(
                                periodoLabel = state.periodoSeleccionado.label,
                                data = state.chartData
                            )
                        }
                    }

                    item {
                        CategoriaDonutSection(
                            titulo = "Gastos por categoría",
                            items = state.gastosPorCategoria,
                            onCategoriaClick = onVerCategoria
                        )
                    }
                    item {
                        CategoriaDonutSection(
                            titulo = "Ingresos por categoría",
                            items = state.ingresosPorCategoria,
                            onCategoriaClick = onVerCategoria
                        )
                    }
                    item { MetodoPagoSection(state.gastoPorMetodoPago) }
                }
            }

            item { MetasResumenSection(state.metas) }
            item { PresupuestosResumenSection(state.presupuestos) }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

// ── Texto compartible / CSV ─────────────────────────────────────────────────

private fun buildResumenTexto(state: ReportesUiState): String {
    return buildString {
        appendLine("📊 Reporte FinTrack — ${state.periodoSeleccionado.label}")
        appendLine()
        appendLine("Ingresos: ${formatColones(state.kpis.ingresos)}")
        appendLine("Gastos: ${formatColones(state.kpis.gastos)}")
        appendLine("Balance: ${formatColones(state.kpis.balance)}")
        appendLine("Ahorro: ${state.kpis.ahorroPercent}%")
        if (state.gastosPorCategoria.isNotEmpty()) {
            appendLine()
            appendLine("Top gastos por categoría:")
            state.gastosPorCategoria.take(5).forEach { item ->
                appendLine("• ${item.categoria}: ${formatColones(item.monto)} (${item.porcentaje}%)")
            }
        }
    }.trim()
}

private fun buildCsv(state: ReportesUiState): String {
    return buildString {
        appendLine("Periodo,Tipo,Categoria,Monto,Porcentaje")
        state.gastosPorCategoria.forEach { item ->
            appendLine("${state.periodoSeleccionado.label},Gasto,${item.categoria},${item.monto},${item.porcentaje}")
        }
        state.ingresosPorCategoria.forEach { item ->
            appendLine("${state.periodoSeleccionado.label},Ingreso,${item.categoria},${item.monto},${item.porcentaje}")
        }
    }.trim()
}

// ── Selector de período: cápsula animada, siempre con las 5 opciones visibles ──

@Composable
private fun PeriodoSelector(
    seleccionado: ReportePeriodo,
    onSeleccionar: (ReportePeriodo) -> Unit
) {
    val colors = LocalAppColors.current
    val periodos = ReportePeriodo.entries
    val selectedIndex = periodos.indexOf(seleccionado)

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Período",
                color = colors.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = montserratFamily()
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .padding(4.dp)
        ) {
            val segmentWidth = maxWidth / periodos.size
            // Sutil: la cápsula se desliza en vez de saltar de color, sin overshoot ni rebote.
            val offsetX by animateDpAsState(
                targetValue = segmentWidth * selectedIndex,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                label = "periodoPillOffset"
            )

            Box(
                modifier = Modifier
                    .offset(x = offsetX)
                    .width(segmentWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(FinTrackColors.GreenPrimary.copy(alpha = 0.16f))
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                periodos.forEach { periodo ->
                    val isSelected = periodo == seleccionado
                    // Mismo idioma que los badges de estado en Presupuestos: texto en el
                    // propio color sobre un tinte suave, no texto blanco sobre color solido.
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) FinTrackColors.GreenPrimary else colors.textSecondary,
                        animationSpec = tween(durationMillis = 320),
                        label = "periodoTextColor"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSeleccionar(periodo) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = periodo.shortLabel,
                            color = textColor,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            fontFamily = montserratFamily()
                        )
                    }
                }
            }
        }
    }
}

// ── KPIs ─────────────────────────────────────────────────────────────────────

@Composable
private fun KpiSummaryRow(
    kpis: KpiData,
    comparacionIngresosPercent: Int?,
    comparacionGastosPercent: Int?
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiSummaryCard(
                label = "Ingresos",
                value = formatColonesCompacto(kpis.ingresos),
                valueColor = FinTrackColors.GreenPrimary,
                deltaPercent = comparacionIngresosPercent,
                deltaFavorableWhenPositive = true,
                modifier = Modifier.weight(1f)
            )
            KpiSummaryCard(
                label = "Gastos",
                value = formatColonesCompacto(kpis.gastos),
                valueColor = FinTrackColors.ErrorColor,
                deltaPercent = comparacionGastosPercent,
                deltaFavorableWhenPositive = false,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiSummaryCard(
                label = "Balance",
                value = formatColonesCompacto(kpis.balance),
                valueColor = if (kpis.balance < 0) FinTrackColors.ErrorColor else FinTrackColors.GreenPrimary,
                modifier = Modifier.weight(1f)
            )
            KpiSummaryCard(
                label = "Ahorro",
                value = "${kpis.ahorroPercent}%",
                valueColor = FinTrackColors.GreenPrimary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun KpiSummaryCard(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    deltaPercent: Int? = null,
    deltaFavorableWhenPositive: Boolean = true
) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .glassCard()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(value, color = valueColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (deltaPercent != null) {
            Spacer(Modifier.height(4.dp))
            val isFavorable = if (deltaFavorableWhenPositive) deltaPercent >= 0 else deltaPercent <= 0
            val deltaColor = if (isFavorable) FinTrackColors.GreenPrimary else FinTrackColors.ErrorColor
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (deltaPercent >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint = deltaColor,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = "${if (deltaPercent >= 0) "+" else ""}$deltaPercent% vs. anterior",
                    color = deltaColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ── Tendencia (reusa BarChart/DarkCard/LegendDot de ChartComponents.kt) ────────

@Composable
private fun TendenciaSection(periodoLabel: String, data: List<MonthlyChartData>) {
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()
    DarkCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("Tendencia · $periodoLabel", color = colors.textSecondary, fontSize = 11.sp, fontFamily = montserrat)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(FinTrackColors.GreenPrimary, "Ingresos")
            LegendDot(FinTrackColors.ErrorColor, "Gastos")
        }
        Spacer(Modifier.height(16.dp))
        BarChart(data)
    }
}

// ── Gastos/ingresos por categoría (donut a mano con Canvas) ───────────────────

@Composable
private fun categoriaPalette(): List<Color> = listOf(
    FinTrackColors.GreenPrimary,
    FinTrackColors.IndigoLight,
    FinTrackColors.VioletLight,
    FinTrackColors.BlueMeta,
    FinTrackColors.WarningLight,
    FinTrackColors.RedLight,
    FinTrackColors.GreenLight,
    FinTrackColors.IndigoDark
)

@Composable
private fun CategoriaDonutSection(
    titulo: String,
    items: List<CategoriaReporteItem>,
    onCategoriaClick: (String) -> Unit
) {
    if (items.isEmpty()) return
    val colors = LocalAppColors.current
    val montserrat = montserratFamily()
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
private fun DonutChart(items: List<CategoriaReporteItem>, colors: List<Color>, modifier: Modifier = Modifier) {
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
private fun CategoriaLegendRow(item: CategoriaReporteItem, color: Color, onClick: () -> Unit) {
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

// ── Gasto por método de pago ────────────────────────────────────────────────

@Composable
private fun MetodoPagoSection(items: List<MetodoPagoReporteItem>) {
    if (items.isEmpty()) return
    val colors = LocalAppColors.current
    DarkCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            "Gasto por método de pago",
            color = colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = montserratFamily()
        )
        Spacer(Modifier.height(14.dp))
        items.forEachIndexed { index, item ->
            MetodoPagoRow(item)
            if (index != items.lastIndex) Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun MetodoPagoRow(item: MetodoPagoReporteItem) {
    val colors = LocalAppColors.current
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(item.metodo, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text("${formatColones(item.monto)} · ${item.porcentaje}%", color = colors.textSecondary, fontSize = 12.sp)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { item.porcentaje / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = FinTrackColors.GreenPrimary,
            trackColor = colors.surfaceSecondary,
            strokeCap = StrokeCap.Round
        )
    }
}

// ── Resumen de metas ─────────────────────────────────────────────────────────

@Composable
private fun MetasResumenSection(metas: List<MetaItem>) {
    if (metas.isEmpty()) return
    val colors = LocalAppColors.current
    val visibles = metas.take(5)
    DarkCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            "Metas de ahorro",
            color = colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = montserratFamily()
        )
        Spacer(Modifier.height(14.dp))
        visibles.forEachIndexed { index, meta ->
            MetaResumenRow(meta)
            if (index != visibles.lastIndex) Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun MetaResumenRow(meta: MetaItem) {
    val colors = LocalAppColors.current
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(meta.nombre, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text("${meta.porcentaje}%", color = FinTrackColors.GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (meta.porcentaje / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = FinTrackColors.GreenPrimary,
            trackColor = colors.surfaceSecondary,
            strokeCap = StrokeCap.Round
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "${formatColones(meta.ahorrado)} de ${formatColones(meta.meta)}",
            color = colors.textSecondary,
            fontSize = 11.sp
        )
    }
}

// ── Resumen de presupuestos ──────────────────────────────────────────────────

@Composable
private fun PresupuestosResumenSection(presupuestos: List<BudgetItem>) {
    if (presupuestos.isEmpty()) return
    val colors = LocalAppColors.current
    val ordenados = presupuestos.sortedByDescending { it.usagePct }.take(5)
    DarkCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            "Presupuestos",
            color = colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = montserratFamily()
        )
        Spacer(Modifier.height(14.dp))
        ordenados.forEachIndexed { index, budget ->
            PresupuestoResumenRow(budget)
            if (index != ordenados.lastIndex) Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PresupuestoResumenRow(budget: BudgetItem) {
    val colors = LocalAppColors.current
    val statusColor = when (budget.status) {
        BudgetStatus.CRITICAL -> FinTrackColors.ErrorColor
        BudgetStatus.WARNING -> FinTrackColors.WarningColor
        BudgetStatus.OK -> FinTrackColors.GreenPrimary
    }
    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(budget.categoryName, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                "${(budget.usagePct * 100).toInt()}%",
                color = statusColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { budget.usagePct },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = statusColor,
            trackColor = colors.surfaceSecondary,
            strokeCap = StrokeCap.Round
        )
    }
}

// ── Estado vacío ───────────────────────────────────────────────────────────

@Composable
private fun EmptyReportesState(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "📊", fontSize = 64.sp)
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Sin movimientos en este período",
            color = colors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Registra transacciones o elige otro rango de fechas para ver tu reporte.",
            color = colors.textSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
    }
}
