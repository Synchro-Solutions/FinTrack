package fintrack.proyecto4.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fintrack.proyecto4.transaction.NoOpTransactionRepository
import fintrack.proyecto4.transaction.Transaction
import fintrack.proyecto4.transaction.TransactionRepository
import fintrack.proyecto4.transaction.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BudgetListViewModel(
    private val repository: BudgetRepository,
    private val uid: String,
    private val transactionRepository: TransactionRepository = NoOpTransactionRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(BudgetListState())
    val state: StateFlow<BudgetListState> = _state

    private var allBudgets: List<BudgetItem> = emptyList()
    private var allTransactions: List<Transaction> = emptyList()

    init {
        loadBudgets()
    }

    fun loadBudgets() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            allBudgets = repository.getBudgets(uid)
            allTransactions = try {
                transactionRepository.getTransactions(uid)
            } catch (e: Exception) {
                emptyList()
            }

            val current = currentPeriodKey()
            val periods = (allBudgets.map { it.periodKey.ifBlank { current } } + current)
                .distinct()
                .sortedDescending()

            val selected = _state.value.selectedPeriod.ifBlank { current }
                .let { if (it in periods) it else current }

            emitForPeriod(selected, periods)
        }
    }

    fun selectPeriod(period: String) {
        val periods = _state.value.availablePeriods.ifEmpty { listOf(period) }
        emitForPeriod(period, periods)
    }

    private fun emitForPeriod(period: String, periods: List<String>) {
        val current = currentPeriodKey()
        val budgets = allBudgets
            .filter { it.periodKey.ifBlank { current } == period }
            .sortedByDescending { it.rawUsagePct }

        _state.value = BudgetListState(
            budgets = budgets,
            isLoading = false,
            availablePeriods = periods,
            selectedPeriod = period,
            transactions = allTransactions
        )
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            repository.deleteBudget(uid, budgetId)
            loadBudgets()
        }
    }

    fun updateBudget(budgetId: String, newLimit: Double, newThreshold: Float) {
        viewModelScope.launch {
            repository.updateBudget(uid, budgetId, newLimit, newThreshold)
            loadBudgets()
        }
    }

    fun deactivateBudget(budgetId: String) {
        viewModelScope.launch {
            repository.deactivateBudget(uid, budgetId)
            loadBudgets()
        }
    }
}

fun budgetTransactions(
    transactions: List<Transaction>,
    budget: BudgetItem,
    selectedPeriod: String
): List<Transaction> {
    val period = budget.periodKey.ifBlank { selectedPeriod }
    return transactions
        .filter {
            it.type == TransactionType.EXPENSE &&
                it.category == budget.categoryName &&
                periodKeyOfDate(it.date) == period
        }
        .sortedByDescending { it.createdAt }
}
