package fintrack.proyecto4.dashboard

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // TODO: cargar datos reales desde repositorio
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    userName = "Ana Vargas",
                    mesActual = "Junio 2024",
                    kpis = KpiData(
                        ingresos = 1_000_000,
                        gastos = 239_000,
                        balance = 781_000,
                        ahorroPercent = 77
                    ),
                    chartData = sampleChartData(),
                    presupuestos = samplePresupuestos(),
                    metaPrincipal = sampleMeta(),
                    consejoFinanciero = "Tu tasa de ahorro del 77% supera el objetivo del 20%. Mantén el ritmo y alcanzarás tu fondo de emergencia en 3 meses.",
                    ultimosMovimientos = sampleMovimientos(),
                    notificationCount = 2,
                    ocrPendingCount = 0
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            // TODO: refrescar datos desde repositorio
            loadDashboard()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun toggleSaldoVisible() {
        _uiState.update { it.copy(saldoVisible = !it.saldoVisible) }
    }

    fun marcarNotificacionesLeidas() {
        // TODO: marcar notificaciones como leídas en el repositorio
        _uiState.update { it.copy(notificationCount = 0) }
    }

    fun navegarAIngreso() {
        // TODO: emitir evento de navegación a formulario de ingreso
    }

    fun navegarAGasto() {
        // TODO: emitir evento de navegación a formulario de gasto
    }

    fun navegarAOcr() {
        // TODO: emitir evento de navegación a captura OCR
    }

    fun navegarAReportes() {
        // TODO: emitir evento de navegación a pantalla de reportes
    }

    fun verTodosPresupuestos() {
        // TODO: navegar a lista completa de presupuestos
    }

    fun verTodasMetas() {
        // TODO: navegar a lista completa de metas
    }

    fun verTodosMovimientos() {
        // TODO: navegar a lista completa de movimientos
    }

    // ── Datos de muestra para desarrollo ────────────────────────────────────

    private fun sampleChartData() = listOf(
        MonthlyChartData("Ene", 900_000, 310_000),
        MonthlyChartData("Feb", 950_000, 280_000),
        MonthlyChartData("Mar", 1_000_000, 350_000),
        MonthlyChartData("Abr", 980_000, 260_000),
        MonthlyChartData("May", 1_050_000, 290_000),
        MonthlyChartData("Jun", 1_000_000, 239_000)
    )

    private fun samplePresupuestos() = listOf(
        PresupuestoItem("1", "Alimentación", 99_000, 150_000, Color(0xFFF59E0B)),
        PresupuestoItem("2", "Transporte", 24_000, 60_000, Color(0xFF3B82F6)),
        PresupuestoItem("3", "Entretenimiento", 12_000, 40_000, Color(0xFF8B5CF6))
    )

    private fun sampleMeta() = MetaItem(
        id = "1",
        nombre = "Fondo emergencia",
        descripcion = "Fondo de emergencia",
        fechaVencimiento = "2024-12-31",
        ahorrado = 285_000,
        meta = 500_000,
        prioridad = "Alta prioridad"
    )

    private fun sampleMovimientos() = listOf(
        MovimientoItem("1", "Salario mensual", "Salario", "06-01", 850_000, true),
        MovimientoItem("2", "Supermercado", "Alimentación", "06-03", 45_000, false),
        MovimientoItem("3", "Gasolina", "Transporte", "06-04", 16_000, false),
        MovimientoItem("4", "Electricidad CNFL", "Servicios", "06-05", 28_000, false)
    )
}
