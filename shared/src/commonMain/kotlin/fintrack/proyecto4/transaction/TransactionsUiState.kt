package fintrack.proyecto4.transaction

import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

enum class TransactionsFilter {
    ALL,
    INCOME,
    EXPENSE
}

/**
 * Por defecto se ve todo el historial ([ALL]); el usuario decide si quiere acotarlo a
 * [CURRENT_MONTH] desde el diálogo de filtros.
 */
enum class DateScope {
    ALL,
    CURRENT_MONTH
}

data class TransactionsUiState(
    val isLoading: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val searchQuery: String = "",
    val filter: TransactionsFilter = TransactionsFilter.ALL,
    val dateScope: DateScope = DateScope.ALL,
    val categoryFilter: String? = null,
    val paymentMethodFilter: PaymentMethod? = null,
    val visibleCount: Int = PageSize,
    val errorMessage: String? = null
) {
    /** Categorías realmente presentes en los movimientos del usuario (no una lista fija). */
    val availableCategories: List<String>
        get() = transactions.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()

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
            .filter { transaction -> dateScope == DateScope.ALL || isInCurrentMonth(transaction.date) }
            .filter { transaction -> categoryFilter == null || transaction.category == categoryFilter }
            .filter { transaction -> paymentMethodFilter == null || transaction.paymentMethod == paymentMethodFilter }
            .filter { transaction -> matchesSearch(transaction, searchQuery) }
            .toList()

    /** US-13: historial paginado, 20 resultados a la vez ("Cargar más" avanza [visibleCount]). */
    val pagedTransactions: List<Transaction>
        get() = filteredTransactions.take(visibleCount)

    val hasMoreToLoad: Boolean
        get() = filteredTransactions.size > visibleCount

    val activeFilterCount: Int
        get() = (if (dateScope != DateScope.ALL) 1 else 0) +
            (if (categoryFilter != null) 1 else 0) +
            (if (paymentMethodFilter != null) 1 else 0)

    companion object {
        const val PageSize = 20
    }
}

/** US-50: busca por descripción, categoría o monto exacto. */
private fun matchesSearch(transaction: Transaction, query: String): Boolean {
    val trimmed = query.trim()
    if (trimmed.isBlank()) return true
    val exactAmountMatch = trimmed.toLongOrNull() == transaction.amount
    return exactAmountMatch ||
        transaction.description.contains(trimmed, ignoreCase = true) ||
        transaction.category.contains(trimmed, ignoreCase = true)
}

/** Si la fecha no se puede interpretar, no se oculta la transacción por este filtro. */
private fun isInCurrentMonth(dateValue: String): Boolean {
    val parsed = parseFormFieldDate(dateValue) ?: return true
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    return parsed.year == today.year && parsed.month == today.month
}
