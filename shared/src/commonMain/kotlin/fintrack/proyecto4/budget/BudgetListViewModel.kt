package fintrack.proyecto4.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BudgetListViewModel(
    private val repository: BudgetRepository,
    private val uid: String
) : ViewModel() {

    private val _state = MutableStateFlow(BudgetListState())
    val state: StateFlow<BudgetListState> = _state

    init {
        loadBudgets()
    }

    fun loadBudgets() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val periodKey = currentPeriodKey()
            val budgets = repository.getBudgets(uid)
                .filter { it.periodKey.isBlank() || it.periodKey == periodKey }
                .sortedByDescending { it.rawUsagePct }
            _state.value = BudgetListState(budgets = budgets, isLoading = false)
        }
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
