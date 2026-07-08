package fintrack.proyecto4.dashboard

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fintrack.proyecto4.transaction.NoOpTransactionRepository
import fintrack.proyecto4.transaction.Transaction
import fintrack.proyecto4.transaction.TransactionRepository
import fintrack.proyecto4.transaction.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val UltimosMovimientosCount = 4

/**
 * @param uid Usuario actualmente autenticado (ver AuthClient.currentUserId()). Se usa para
 *   obtener los últimos movimientos reales del usuario en sesión desde [transactionRepository].
 */
class DashboardViewModel(
    private val transactionRepository: TransactionRepository = NoOpTransactionRepository(),
    private val uid: String = ""
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // TODO: cargar KPIs/gráfica/presupuestos/meta reales desde repositorio
            val ultimosMovimientos = try {
                transactionRepository.getTransactions(uid)
                    .sortedByDescending { it.createdAt }
                    .take(UltimosMovimientosCount)
                    .map { it.toMovimientoItem() }
            } catch (e: Exception) {
                emptyList()
            }
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
                    ultimosMovimientos = ultimosMovimientos,
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

    private fun Transaction.toMovimientoItem() = MovimientoItem(
        id = id,
        nombre = description,
        categoria = category,
        fecha = date,
        monto = amount,
        esIngreso = type == TransactionType.INCOME
    )
}
