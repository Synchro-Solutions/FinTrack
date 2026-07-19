package fintrack.proyecto4.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class CreateBudgetState(
    val selectedCategory: BudgetCategory? = null,
    val limitAmount: String = "",
    val alertThreshold: Float = 0.8f,
    val isSaving: Boolean = false,
    val savedOk: Boolean = false,
    val error: String? = null
) {
    val canSave: Boolean get() = selectedCategory != null && limitAmount.isNotBlank()
        && (limitAmount.toDoubleOrNull() ?: 0.0) > 0
}

class CreateBudgetViewModel(
    private val repository: BudgetRepository,
    private val uid: String
) : ViewModel() {

    private val _state = MutableStateFlow(CreateBudgetState())
    val state: StateFlow<CreateBudgetState> = _state

    fun selectCategory(category: BudgetCategory) {
        _state.value = _state.value.copy(selectedCategory = category, error = null)
    }

    fun setLimitAmount(amount: String) {
        if (amount.all { it.isDigit() || it == '.' }) {
            _state.value = _state.value.copy(limitAmount = amount, error = null)
        }
    }

    fun setAlertThreshold(threshold: Float) {
        _state.value = _state.value.copy(alertThreshold = threshold)
    }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        val category = s.selectedCategory ?: return
        val limit = s.limitAmount.toDoubleOrNull() ?: return

        viewModelScope.launch {
            _state.value = s.copy(isSaving = true, error = null)
            try {
                val item = BudgetItem(
                    id = "",
                    categoryName = category.name,
                    categoryIcon = category.icon,
                    categoryColor = colorFromHex(category.colorHex),
                    spent = 0.0,
                    limit = limit,
                    period = "mensual",
                    alertThreshold = s.alertThreshold,
                    periodKey = currentPeriodKey()
                )
                repository.addBudget(uid, item)
                _state.value = _state.value.copy(isSaving = false, savedOk = true)
                onDone()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = "Error al guardar. Intenta de nuevo."
                )
            }
        }
    }
}
