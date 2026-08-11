package fintrack.proyecto4.reportes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fintrack.proyecto4.budget.BudgetRepository
import fintrack.proyecto4.budget.NoOpBudgetRepository
import fintrack.proyecto4.dashboard.KpiData
import fintrack.proyecto4.dashboard.MetaItem
import fintrack.proyecto4.savings.model.GoalStatus
import fintrack.proyecto4.savings.model.SavingsGoal
import fintrack.proyecto4.savings.repository.SavingsRepository
import fintrack.proyecto4.transaction.NoOpTransactionRepository
import fintrack.proyecto4.transaction.Transaction
import fintrack.proyecto4.transaction.TransactionRepository
import fintrack.proyecto4.transaction.TransactionType
import fintrack.proyecto4.transaction.parseFormFieldDate
import fintrack.proyecto4.util.buildMonthlyChartData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class ReportesViewModel(
    private val transactionRepository: TransactionRepository = NoOpTransactionRepository(),
    private val uid: String = "",
    private val budgetRepository: BudgetRepository = NoOpBudgetRepository(),
    private val savingsRepository: SavingsRepository = SavingsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportesUiState())
    val uiState: StateFlow<ReportesUiState> = _uiState.asStateFlow()

    private var allTransactions: List<Transaction> = emptyList()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            allTransactions = try {
                transactionRepository.getTransactions(uid)
            } catch (_: Exception) {
                emptyList()
            }

            val budgets = try {
                budgetRepository.getBudgets(uid)
            } catch (_: Exception) {
                emptyList()
            }

            try {
                savingsRepository.loadFromFirestore()
            } catch (_: Exception) {
                // Se mantienen las metas cargadas localmente, si las hay.
            }

            val metas = savingsRepository.getGoals()
                .filter { it.status == GoalStatus.ACTIVE }
                .map { it.toMetaItem() }

            _uiState.update {
                it.copy(presupuestos = budgets, metas = metas)
            }

            recomputeForPeriodo(_uiState.value.periodoSeleccionado)
        }
    }

    fun onPeriodoSeleccionado(periodo: ReportePeriodo) {
        recomputeForPeriodo(periodo)
    }

    private fun recomputeForPeriodo(periodo: ReportePeriodo) {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val monthsBack = when (periodo) {
            ReportePeriodo.MES_ACTUAL -> 1
            ReportePeriodo.ULTIMOS_3_MESES -> 3
            ReportePeriodo.ULTIMOS_6_MESES -> 6
            ReportePeriodo.ULTIMOS_12_MESES -> 12
            ReportePeriodo.ANIO_ACTUAL -> today.monthNumber
        }

        val currentEnd = today.year * 12 + today.monthNumber
        val currentStart = currentEnd - monthsBack + 1

        val periodoTransactions = transactionsInMonthRange(currentStart, currentEnd)

        val ingresos = periodoTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }

        val gastos = periodoTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        val balance = ingresos - gastos
        val ahorroPercent = if (ingresos > 0) ((balance * 100) / ingresos).toInt() else 0

        // Mismo tamaño de ventana, desplazada hacia atrás: para "3 meses" compara contra
        // los 3 meses anteriores a esos, para "Este año" contra el mismo tramo del año pasado.
        val previousEnd = currentStart - 1
        val previousStart = previousEnd - monthsBack + 1
        val periodoAnteriorTransactions = transactionsInMonthRange(previousStart, previousEnd)

        val ingresosAnteriores = periodoAnteriorTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }

        val gastosAnteriores = periodoAnteriorTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        // Un solo mes no aporta como tendencia; el chart solo aplica a los presets multi-mes.
        val chartData = if (periodo == ReportePeriodo.MES_ACTUAL) {
            emptyList()
        } else {
            buildMonthlyChartData(allTransactions, monthsBack)
        }

        _uiState.update { state ->
            state.copy(
                isLoading = false,
                periodoSeleccionado = periodo,
                kpis = KpiData(
                    ingresos = ingresos,
                    gastos = gastos,
                    balance = balance,
                    ahorroPercent = ahorroPercent
                ),
                comparacionIngresosPercent = percentChange(ingresos, ingresosAnteriores),
                comparacionGastosPercent = percentChange(gastos, gastosAnteriores),
                chartData = chartData,
                gastosPorCategoria = buildCategoriaBreakdown(periodoTransactions, TransactionType.EXPENSE),
                ingresosPorCategoria = buildCategoriaBreakdown(periodoTransactions, TransactionType.INCOME),
                gastoPorMetodoPago = buildMetodoPagoBreakdown(periodoTransactions)
            )
        }
    }

    private fun transactionsInMonthRange(startMonthIndex: Int, endMonthIndex: Int): List<Transaction> {
        return allTransactions.filter { transaction ->
            val fecha = parseFormFieldDate(transaction.date) ?: return@filter false
            val monthIndex = fecha.year * 12 + fecha.monthNumber
            monthIndex in startMonthIndex..endMonthIndex
        }
    }

    /** Null si no hay datos del período anterior para comparar (evita división por cero). */
    private fun percentChange(actual: Long, anterior: Long): Int? {
        if (anterior <= 0L) return null
        return (((actual - anterior) * 100) / anterior).toInt()
    }

    private fun buildCategoriaBreakdown(
        transactions: List<Transaction>,
        type: TransactionType
    ): List<CategoriaReporteItem> {
        val filtered = transactions.filter { it.type == type }
        val total = filtered.sumOf { it.amount }

        return filtered
            .groupBy { it.category.ifBlank { "Otros" } }
            .map { (categoria, txs) ->
                val monto = txs.sumOf { it.amount }
                CategoriaReporteItem(
                    categoria = categoria,
                    monto = monto,
                    porcentaje = if (total > 0) ((monto * 100) / total).toInt() else 0
                )
            }
            .sortedByDescending { it.monto }
    }

    private fun buildMetodoPagoBreakdown(transactions: List<Transaction>): List<MetodoPagoReporteItem> {
        val gastos = transactions.filter { it.type == TransactionType.EXPENSE }
        val total = gastos.sumOf { it.amount }

        return gastos
            .groupBy { it.paymentMethod }
            .map { (metodo, txs) ->
                val monto = txs.sumOf { it.amount }
                MetodoPagoReporteItem(
                    metodo = metodo?.label ?: "Sin especificar",
                    monto = monto,
                    porcentaje = if (total > 0) ((monto * 100) / total).toInt() else 0
                )
            }
            .sortedByDescending { it.monto }
    }

    private fun SavingsGoal.toMetaItem(): MetaItem {
        return MetaItem(
            id = id,
            nombre = name,
            descripcion = iconName,
            fechaVencimiento = deadline ?: "—",
            ahorrado = currentAmount.toLong(),
            meta = targetAmount.toLong(),
            prioridad = priorityLabel
        )
    }
}
