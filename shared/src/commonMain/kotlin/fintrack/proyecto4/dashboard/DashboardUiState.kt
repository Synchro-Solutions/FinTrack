package fintrack.proyecto4.dashboard

import androidx.compose.ui.graphics.Color

data class DashboardUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val userName: String = "Usuario",
    val mesActual: String = "",
    val saldoVisible: Boolean = true,
    val kpis: KpiData = KpiData(),
    val chartData: List<MonthlyChartData> = emptyList(),
    val presupuestos: List<PresupuestoItem> = emptyList(),
    val metaPrincipal: MetaItem? = null,
    val consejoFinanciero: String = "",
    val ultimosMovimientos: List<MovimientoItem> = emptyList(),
    val notificationCount: Int = 0,
    val ocrPendingCount: Int = 0
)

data class KpiData(
    val ingresos: Long = 0,
    val gastos: Long = 0,
    val balance: Long = 0,
    val ahorroPercent: Int = 0
)

data class MonthlyChartData(
    val mes: String,
    val ingresos: Long,
    val gastos: Long
)

data class PresupuestoItem(
    val id: String,
    val nombre: String,
    val gastado: Long,
    val total: Long,
    val color: Color
) {
    val porcentaje: Int get() = if (total > 0) ((gastado * 100) / total).toInt() else 0
}

data class MetaItem(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val fechaVencimiento: String,
    val ahorrado: Long,
    val meta: Long,
    val prioridad: String
) {
    val porcentaje: Int get() = if (meta > 0) ((ahorrado * 100) / meta).toInt() else 0
}

data class MovimientoItem(
    val id: String,
    val nombre: String,
    val categoria: String,
    val fecha: String,
    val monto: Long,
    val esIngreso: Boolean
)
