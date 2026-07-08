package fintrack.proyecto4.movimientos

import fintrack.proyecto4.transaction.Transaction
import fintrack.proyecto4.transaction.TransactionType

enum class MovimientosFilter {
    TODOS,
    INGRESOS,
    GASTOS
}

data class MovimientosUiState(
    val isLoading: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val searchQuery: String = "",
    val filter: MovimientosFilter = MovimientosFilter.TODOS,
    val errorMessage: String? = null
) {
    val filteredTransactions: List<Transaction>
        get() = transactions
            .asSequence()
            .filter { transaction ->
                when (filter) {
                    MovimientosFilter.TODOS -> true
                    MovimientosFilter.INGRESOS -> transaction.type == TransactionType.INCOME
                    MovimientosFilter.GASTOS -> transaction.type == TransactionType.EXPENSE
                }
            }
            .filter { transaction ->
                searchQuery.isBlank() ||
                    transaction.description.contains(searchQuery, ignoreCase = true) ||
                    transaction.category.contains(searchQuery, ignoreCase = true)
            }
            .toList()
}
