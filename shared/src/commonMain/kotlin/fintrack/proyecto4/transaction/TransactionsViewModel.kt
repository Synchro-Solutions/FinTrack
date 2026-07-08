package fintrack.proyecto4.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * @param uid Usuario actualmente autenticado (ver AuthClient.currentUserId()). Las
 *   transacciones mostradas son siempre las del usuario en sesión, nunca una lista global.
 */
class TransactionsViewModel(
    private val repository: TransactionRepository,
    private val uid: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    /** Se llama cada vez que se entra a la pantalla, para reflejar transacciones recién
     *  guardadas desde el formulario manual o el flujo OCR. */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val transactions = repository.getTransactions(uid).sortedByDescending { it.createdAt }
                _uiState.update { it.copy(isLoading = false, transactions = transactions, errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "No se pudieron cargar los movimientos") }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun updateFilter(filter: TransactionsFilter) {
        _uiState.update { it.copy(filter = filter) }
    }
}
