package fintrack.proyecto4.movimientos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fintrack.proyecto4.transaction.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MovimientosViewModel(
    private val repository: TransactionRepository = TransactionRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MovimientosUiState(transactions = repository.getTransactions())
    )
    val uiState: StateFlow<MovimientosUiState> = _uiState.asStateFlow()

    /** Se llama cada vez que se entra a la pantalla, para reflejar movimientos recién
     *  guardados desde el formulario manual o el flujo OCR. */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.loadFromFirestore()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    transactions = repository.getTransactions(),
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun updateFilter(filter: MovimientosFilter) {
        _uiState.update { it.copy(filter = filter) }
    }
}
