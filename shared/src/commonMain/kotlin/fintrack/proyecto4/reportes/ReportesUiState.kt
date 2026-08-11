package fintrack.proyecto4.reportes

import fintrack.proyecto4.budget.BudgetItem
import fintrack.proyecto4.dashboard.KpiData
import fintrack.proyecto4.dashboard.MetaItem
import fintrack.proyecto4.dashboard.MonthlyChartData

/**
 * Presets de rango de fechas para el Dashboard de Reportes (US-74).
 * Rango 100% personalizado queda fuera de alcance de esta historia.
 */
enum class ReportePeriodo(val label: String, val shortLabel: String) {
    MES_ACTUAL("Este mes", "Mes"),
    ULTIMOS_3_MESES("3 meses", "3M"),
    ULTIMOS_6_MESES("6 meses", "6M"),
    ULTIMOS_12_MESES("12 meses", "12M"),
    ANIO_ACTUAL("Este año", "Año")
}

data class CategoriaReporteItem(
    val categoria: String,
    val monto: Long,
    val porcentaje: Int
)

data class MetodoPagoReporteItem(
    val metodo: String,
    val monto: Long,
    val porcentaje: Int
)

data class ReportesUiState(
    val isLoading: Boolean = true,
    val periodoSeleccionado: ReportePeriodo = ReportePeriodo.MES_ACTUAL,
    val kpis: KpiData = KpiData(),
    // Null = sin datos del período anterior para comparar (ej. usuario nuevo).
    val comparacionIngresosPercent: Int? = null,
    val comparacionGastosPercent: Int? = null,
    val chartData: List<MonthlyChartData> = emptyList(),
    val gastosPorCategoria: List<CategoriaReporteItem> = emptyList(),
    val ingresosPorCategoria: List<CategoriaReporteItem> = emptyList(),
    val gastoPorMetodoPago: List<MetodoPagoReporteItem> = emptyList(),
    val metas: List<MetaItem> = emptyList(),
    val presupuestos: List<BudgetItem> = emptyList()
) {
    val hasMovimientosEnPeriodo: Boolean
        get() = kpis.ingresos > 0 || kpis.gastos > 0
}
