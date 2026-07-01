package fintrack.proyecto4.transaction

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TransactionFormViewModel(
    initialType: TransactionType = TransactionType.EXPENSE
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TransactionFormState(type = initialType)
    )

    val uiState: StateFlow<TransactionFormState> = _uiState.asStateFlow()

    fun changeType(type: TransactionType) {
        _uiState.value = _uiState.value.copy(
            type = type,
            selectedCategory = null,
            description = ""
        )
    }

    fun updateAmount(amount: String) {
        val cleanAmount = amount.filter { it.isDigit() }
        _uiState.value = _uiState.value.copy(amount = cleanAmount)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun selectCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun selectPaymentMethod(paymentMethod: PaymentMethod) {
        _uiState.value = _uiState.value.copy(paymentMethod = paymentMethod)
    }

    fun updateDate(date: String) {
        _uiState.value = _uiState.value.copy(date = date)
    }

    fun reset(initialType: TransactionType) {
        _uiState.value = TransactionFormState(
            type = initialType
        )
    }

    fun saveTransaction() {
        // TODO: conectar con repositorio cuando exista persistencia real
    }
}