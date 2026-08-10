package fintrack.proyecto4.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fintrack.proyecto4.ai.FinancialAdviceService
import fintrack.proyecto4.budget.BudgetRepository
import fintrack.proyecto4.budget.NoOpBudgetRepository
import fintrack.proyecto4.onboarding.NoOpOnboardingRepository
import fintrack.proyecto4.onboarding.OnboardingRepository
import fintrack.proyecto4.savings.model.GoalStatus
import fintrack.proyecto4.savings.repository.SavingsRepository
import fintrack.proyecto4.transaction.NoOpTransactionRepository
import fintrack.proyecto4.transaction.Transaction
import fintrack.proyecto4.transaction.TransactionRepository
import fintrack.proyecto4.transaction.TransactionType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import fintrack.proyecto4.util.buildMonthlyChartData
import fintrack.proyecto4.util.toSpanishLabel

private const val UltimosMovimientosCount = 4

private const val FinancialAdviceIntervalMillis =
    10 * 60 * 1000L

class DashboardViewModel(
    private val transactionRepository: TransactionRepository =
        NoOpTransactionRepository(),

    private val uid: String = "",

    private val onboardingRepository: OnboardingRepository =
        NoOpOnboardingRepository(),

    private val budgetRepository: BudgetRepository =
        NoOpBudgetRepository(),

    private val savingsRepository: SavingsRepository =
        SavingsRepository(),

    private val financialAdviceService: FinancialAdviceService =
        FinancialAdviceService()
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(DashboardUiState())

    val uiState: StateFlow<DashboardUiState> =
        _uiState.asStateFlow()

    init {
        loadDashboard()
        startFinancialAdviceUpdates()
    }

    /**
     * Carga toda la información inicial del Dashboard.
     */
    fun loadDashboard() {
        viewModelScope.launch {
            loadDashboardData(
                showLoading = true
            )
        }
    }

    /**
     * Refresca manualmente el Dashboard.
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isRefreshing = true)
            }

            loadDashboardData(
                showLoading = false
            )

            _uiState.update {
                it.copy(isRefreshing = false)
            }
        }
    }

    /**
     * Obtiene los datos del usuario y genera el consejo financiero.
     */
    private suspend fun loadDashboardData(
        showLoading: Boolean
    ) {
        if (showLoading) {
            _uiState.update {
                it.copy(isLoading = true)
            }
        }

        val transactions = try {
            transactionRepository.getTransactions(uid)
        } catch (_: Exception) {
            emptyList()
        }

        val profile = try {
            onboardingRepository.getProfile(uid)
        } catch (_: Exception) {
            null
        }

        val budgets = try {
            budgetRepository.getBudgets(uid)
        } catch (_: Exception) {
            emptyList()
        }

        try {
            savingsRepository.loadFromFirestore()
        } catch (_: Exception) {
            // Se mantienen las metas cargadas localmente.
        }

        val goals =
            savingsRepository.getGoals()

        val ingresos = transactions
            .filter {
                it.type == TransactionType.INCOME
            }
            .sumOf {
                it.amount
            }

        val gastos = transactions
            .filter {
                it.type == TransactionType.EXPENSE
            }
            .sumOf {
                it.amount
            }

        val balance =
            ingresos - gastos

        val ahorroPercent = if (ingresos > 0) {
            ((balance * 100) / ingresos).toInt()
        } else {
            0
        }

        val ultimosMovimientos = transactions
            .sortedByDescending {
                it.createdAt
            }
            .take(UltimosMovimientosCount)
            .map {
                it.toMovimientoItem()
            }

        val chartData =
            buildMonthlyChartData(transactions, monthsBack = 6)

        val presupuestos = budgets.map { budget ->
            PresupuestoItem(
                id = budget.id,
                nombre = budget.categoryName,
                gastado = budget.spent.toLong(),
                total = budget.limit.toLong(),
                color = budget.categoryColor
            )
        }

        val notificationCount = budgets.count {
            it.usagePct >= it.alertThreshold
        }

        val fallbackAdvice = buildConsejo(
            balance = balance,
            ahorroPercent = ahorroPercent,
            budgetCount = budgets.size
        )

        _uiState.update { state ->
            state.copy(
                isLoading = false,
                userName = profile?.name ?: "Usuario",
                fotoUrl = profile?.photoPath,
                mesActual = currentMonthLabel(),

                kpis = KpiData(
                    ingresos = ingresos,
                    gastos = gastos,
                    balance = balance,
                    ahorroPercent = ahorroPercent
                ),

                chartData = chartData,
                presupuestos = presupuestos,

                metaPrincipal = goals
                    .firstOrNull {
                        it.status == GoalStatus.ACTIVE
                    }
                    ?.let { goal ->
                        MetaItem(
                            id = goal.id,
                            nombre = goal.name,
                            descripcion = goal.iconName,
                            fechaVencimiento =
                                goal.deadline ?: "—",

                            ahorrado =
                                goal.currentAmount.toLong(),

                            meta =
                                goal.targetAmount.toLong(),

                            prioridad = if (
                                (goal.progress * 100).toInt() < 30
                            ) {
                                "Alta prioridad"
                            } else {
                                "En progreso"
                            }
                        )
                    },

                // Primero se muestra el consejo local.
                consejoFinanciero = fallbackAdvice,

                ultimosMovimientos =
                    ultimosMovimientos,

                notificationCount =
                    notificationCount,

                ocrPendingCount = 0
            )
        }

        // Después se reemplaza por el consejo generado con IA.
        generateFinancialAdvice(
            transactions = transactions,
            fallbackAdvice = fallbackAdvice
        )
    }

    /**
     * Genera automáticamente un consejo nuevo cada 10 minutos.
     */
    private fun startFinancialAdviceUpdates() {
        viewModelScope.launch {
            while (isActive) {
                delay(FinancialAdviceIntervalMillis)
                updateFinancialAdvice()
            }
        }
    }

    /**
     * Vuelve a consultar los movimientos para generar
     * un consejo actualizado.
     */
    private suspend fun updateFinancialAdvice() {
        val transactions = try {
            transactionRepository.getTransactions(uid)
        } catch (_: Exception) {
            return
        }

        val ingresos = transactions
            .filter {
                it.type == TransactionType.INCOME
            }
            .sumOf {
                it.amount
            }

        val gastos = transactions
            .filter {
                it.type == TransactionType.EXPENSE
            }
            .sumOf {
                it.amount
            }

        val balance =
            ingresos - gastos

        val ahorroPercent = if (ingresos > 0) {
            ((balance * 100) / ingresos).toInt()
        } else {
            0
        }

        val fallbackAdvice = buildConsejo(
            balance = balance,
            ahorroPercent = ahorroPercent,
            budgetCount = _uiState.value.presupuestos.size
        )

        generateFinancialAdvice(
            transactions = transactions,
            fallbackAdvice = fallbackAdvice
        )
    }

    /**
     * Envía los datos financieros a Groq.
     */
    private suspend fun generateFinancialAdvice(
        transactions: List<Transaction>,
        fallbackAdvice: String
    ) {
        val totalIncome = transactions
            .filter {
                it.type == TransactionType.INCOME
            }
            .sumOf {
                it.amount
            }

        val totalExpenses = transactions
            .filter {
                it.type == TransactionType.EXPENSE
            }
            .sumOf {
                it.amount
            }

        val expensesByCategory = transactions
            .filter {
                it.type == TransactionType.EXPENSE
            }
            .groupBy {
                it.category.ifBlank {
                    "Otros"
                }
            }
            .mapValues { entry ->
                entry.value.sumOf {
                    it.amount
                }
            }

        val result =
            financialAdviceService.generateAdvice(
                totalIncome = totalIncome,
                totalExpenses = totalExpenses,
                expensesByCategory = expensesByCategory
            )

        val advice = result.getOrElse {
            fallbackAdvice
        }

        _uiState.update {
            it.copy(
                consejoFinanciero = advice
            )
        }
    }

    fun toggleSaldoVisible() {
        _uiState.update {
            it.copy(
                saldoVisible = !it.saldoVisible
            )
        }
    }

    fun marcarNotificacionesLeidas() {
        _uiState.update {
            it.copy(
                notificationCount = 0
            )
        }
    }

    fun navegarAIngreso() {}

    fun navegarAGasto() {}

    fun navegarAOcr() {}

    fun navegarAReportes() {}

    fun verTodosPresupuestos() {}

    fun verTodasMetas() {}

    private fun currentMonthLabel(): String {
        val today = Clock.System.todayIn(
            TimeZone.currentSystemDefault()
        )

        return "${today.month.toSpanishLabel()} ${today.year}"
    }

    /**
     * Consejo de respaldo cuando Groq no responde.
     */
    private fun buildConsejo(
        balance: Long,
        ahorroPercent: Int,
        budgetCount: Int
    ): String {
        return when {
            balance < 0 -> {
                "Tus gastos superan tus ingresos este mes. " +
                        "Revisa tus categorías de gasto y ajusta tu presupuesto."
            }

            ahorroPercent >= 20 -> {
                "Excelente manejo financiero. Estás ahorrando el " +
                        "$ahorroPercent% de tus ingresos."
            }

            ahorroPercent >= 10 -> {
                "Vas bien. Intenta aumentar tu ahorro reduciendo " +
                        "gastos no esenciales."
            }

            budgetCount == 0 -> {
                "Crea presupuestos por categoría para llevar un " +
                        "mejor control de tus gastos."
            }

            else -> {
                "Sigue registrando tus movimientos para llevar un " +
                        "control preciso de tu ahorro mensual."
            }
        }
    }

    private fun Transaction.toMovimientoItem(): MovimientoItem {
        return MovimientoItem(
            id = id,
            nombre = description,
            categoria = category,
            fecha = date,
            monto = amount,
            esIngreso = type == TransactionType.INCOME
        )
    }
}