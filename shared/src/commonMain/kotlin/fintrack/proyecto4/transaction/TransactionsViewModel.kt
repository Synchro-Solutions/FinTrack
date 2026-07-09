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
        _uiState.update { it.copy(searchQuery = query, visibleCount = TransactionsUiState.PageSize) }
    }

    fun updateFilter(filter: TransactionsFilter) {
        _uiState.update { it.copy(filter = filter, visibleCount = TransactionsUiState.PageSize) }
    }

    /**
     * US-13: se llama una sola vez, al confirmar "Aplicar filtros" en el diálogo. Mientras el
     * usuario elige opciones dentro del diálogo (FiltersSheet en TransactionsScreen), esos
     * cambios quedan en estado local de la UI y no afectan el historial hasta este punto —
     * así la "X" de cerrar puede descartarlos sin tocar el ViewModel.
     */
    fun applyAdvancedFilters(
        dateScope: DateScope,
        customDateFrom: String?,
        customDateTo: String?,
        category: String?,
        paymentMethod: PaymentMethod?
    ) {
        _uiState.update {
            it.copy(
                dateScope = dateScope,
                customDateFrom = customDateFrom,
                customDateTo = customDateTo,
                categoryFilter = category,
                paymentMethodFilter = paymentMethod,
                visibleCount = TransactionsUiState.PageSize
            )
        }
    }

    /** US-13: pagina de a 20 resultados; cada llamada revela 20 más de la lista filtrada. */
    fun loadMore() {
        _uiState.update { it.copy(visibleCount = it.visibleCount + TransactionsUiState.PageSize) }
    }
}
