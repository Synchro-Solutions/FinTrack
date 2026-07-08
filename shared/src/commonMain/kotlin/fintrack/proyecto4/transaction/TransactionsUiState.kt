package fintrack.proyecto4.transaction

enum class TransactionsFilter {
    ALL,
    INCOME,
    EXPENSE
}

data class TransactionsUiState(
    val isLoading: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val searchQuery: String = "",
    val filter: TransactionsFilter = TransactionsFilter.ALL,
    val errorMessage: String? = null
) {
    val filteredTransactions: List<Transaction>
        get() = transactions
            .asSequence()
            .filter { transaction ->
                when (filter) {
                    TransactionsFilter.ALL -> true
                    TransactionsFilter.INCOME -> transaction.type == TransactionType.INCOME
                    TransactionsFilter.EXPENSE -> transaction.type == TransactionType.EXPENSE
                }
            }
            .filter { transaction ->
                searchQuery.isBlank() ||
                    transaction.description.contains(searchQuery, ignoreCase = true) ||
                    transaction.category.contains(searchQuery, ignoreCase = true)
            }
            .toList()
}
