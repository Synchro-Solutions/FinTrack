package fintrack.proyecto4.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fintrack.proyecto4.budget.BudgetRepository
import fintrack.proyecto4.budget.NoOpBudgetRepository
import fintrack.proyecto4.notifications.NoOpNotificationRepository
import fintrack.proyecto4.notifications.NotificationRepository
import fintrack.proyecto4.onboarding.NoOpOnboardingRepository
import fintrack.proyecto4.onboarding.OnboardingRepository
import fintrack.proyecto4.savings.model.GoalStatus
import fintrack.proyecto4.savings.repository.SavingsRepository
import fintrack.proyecto4.transaction.NoOpTransactionRepository
import fintrack.proyecto4.transaction.Transaction
import fintrack.proyecto4.transaction.TransactionRepository
import fintrack.proyecto4.transaction.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

private const val UltimosMovimientosCount = 4

class DashboardViewModel(
    private val transactionRepository: TransactionRepository = NoOpTransactionRepository(),
    private val uid: String = "",
    private val onboardingRepository: OnboardingRepository = NoOpOnboardingRepository(),
    private val budgetRepository: BudgetRepository = NoOpBudgetRepository(),
    private val savingsRepository: SavingsRepository = SavingsRepository(),
    private val notificationRepository: NotificationRepository = NoOpNotificationRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val transactions = try {
                transactionRepository.getTransactions(uid)
            } catch (e: Exception) {
                emptyList()
            }

            val profile = try {
                onboardingRepository.getProfile(uid)
            } catch (e: Exception) {
                null
            }

            val budgets = try {
                budgetRepository.getBudgets(uid)
            } catch (e: Exception) {
                emptyList()
            }

            savingsRepository.loadFromFirestore()
            val goals = savingsRepository.getGoals()

            val ingresos = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val gastos = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val balance = ingresos - gastos
            val ahorroPercent = if (ingresos > 0) ((balance * 100) / ingresos).toInt() else 0

            val ultimosMovimientos = transactions
                .sortedByDescending { it.createdAt }
                .take(UltimosMovimientosCount)
                .map { it.toMovimientoItem() }

            val chartData = buildChartData(transactions)

            val presupuestos = budgets.map { b ->
                PresupuestoItem(
                    id = b.id,
                    nombre = b.categoryName,
                    gastado = b.spent.toLong(),
                    total = b.limit.toLong(),
                    color = b.categoryColor
                )
            }

            val notificationCount = try {
                notificationRepository.unreadCount(uid)
            } catch (e: Exception) {
                0
            }

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    userName = profile?.name ?: "Usuario",
                    mesActual = currentMonthLabel(),
                    kpis = KpiData(
                        ingresos = ingresos,
                        gastos = gastos,
                        balance = balance,
                        ahorroPercent = ahorroPercent
                    ),
                    chartData = chartData,
                    presupuestos = presupuestos,
                    metaPrincipal = goals.firstOrNull { it.status == GoalStatus.ACTIVE }?.let { g ->
                        MetaItem(
                            id = g.id,
                            nombre = g.name,
                            descripcion = g.iconName,
                            fechaVencimiento = g.deadline ?: "—",
                            ahorrado = g.currentAmount.toLong(),
                            meta = g.targetAmount.toLong(),
                            prioridad = if ((g.progress * 100).toInt() < 30) "Alta prioridad" else "En progreso"
                        )
                    },
                    consejoFinanciero = buildConsejo(balance, ahorroPercent, budgets.size),
                    ultimosMovimientos = ultimosMovimientos,
                    notificationCount = notificationCount,
                    ocrPendingCount = 0
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadDashboard()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun toggleSaldoVisible() {
        _uiState.update { it.copy(saldoVisible = !it.saldoVisible) }
    }

    fun marcarNotificacionesLeidas() {
        _uiState.update { it.copy(notificationCount = 0) }
    }

    fun navegarAIngreso() {}
    fun navegarAGasto() {}
    fun navegarAOcr() {}
    fun navegarAReportes() {}
    fun verTodosPresupuestos() {}
    fun verTodasMetas() {}

    private fun currentMonthLabel(): String {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return "${today.month.toSpanishLabel()} ${today.year}"
    }

    private fun Month.toSpanishLabel(): String = when (this) {
        Month.JANUARY -> "Enero"
        Month.FEBRUARY -> "Febrero"
        Month.MARCH -> "Marzo"
        Month.APRIL -> "Abril"
        Month.MAY -> "Mayo"
        Month.JUNE -> "Junio"
        Month.JULY -> "Julio"
        Month.AUGUST -> "Agosto"
        Month.SEPTEMBER -> "Septiembre"
        Month.OCTOBER -> "Octubre"
        Month.NOVEMBER -> "Noviembre"
        Month.DECEMBER -> "Diciembre"
    }

    private fun buildChartData(transactions: List<Transaction>): List<MonthlyChartData> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val result = mutableListOf<MonthlyChartData>()
        for (offset in 5 downTo 0) {
            var monthNum = today.monthNumber - offset
            var year = today.year
            if (monthNum <= 0) { monthNum += 12; year-- }
            val label = Month(monthNum).toSpanishLabel().take(3)
            val monthTx = transactions.filter { tx ->
                // date format: "dd/MM/yyyy"
                val parts = tx.date.split("/")
                parts.size == 3 &&
                    parts[1].toIntOrNull() == monthNum &&
                    parts[2].toIntOrNull() == year
            }
            result.add(MonthlyChartData(
                mes = label,
                ingresos = monthTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
                gastos = monthTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            ))
        }
        return result
    }

    private fun buildConsejo(balance: Long, ahorroPercent: Int, budgetCount: Int): String = when {
        balance < 0 -> "Tus gastos superan tus ingresos este mes. Revisa tus categorías de gasto y ajusta tu presupuesto."
        ahorroPercent >= 20 -> "Excelente manejo financiero. Estás ahorrando el ${ahorroPercent}% de tus ingresos."
        ahorroPercent >= 10 -> "Vas bien. Intenta aumentar tu ahorro reduciendo gastos no esenciales."
        budgetCount == 0 -> "Crea presupuestos por categoría para llevar un mejor control de tus gastos."
        else -> "Sigue registrando tus movimientos para llevar un control preciso de tu ahorro mensual."
    }

    private fun Transaction.toMovimientoItem() = MovimientoItem(
        id = id,
        nombre = description,
        categoria = category,
        fecha = date,
        monto = amount,
        esIngreso = type == TransactionType.INCOME
    )
}
