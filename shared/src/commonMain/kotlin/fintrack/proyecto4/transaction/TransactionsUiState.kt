package fintrack.proyecto4.transaction

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

enum class TransactionsFilter {
    ALL,
    INCOME,
    EXPENSE
}

/**
 * Rango de fechas del historial. Por defecto se ve todo el historial ([ALL]); el usuario
 * decide si lo acota desde el diálogo de filtros. [CUSTOM] usa [TransactionsUiState.customDateFrom]
 * y [TransactionsUiState.customDateTo] (formato "dd/mm/aaaa", igual que el resto de la app).
 */
enum class DateScope {
    ALL,
    CURRENT_MONTH,
    LAST_3_MONTHS,
    CURRENT_YEAR,
    CUSTOM
}

data class TransactionsUiState(
    val isLoading: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val searchQuery: String = "",
    val filter: TransactionsFilter = TransactionsFilter.ALL,
    val dateScope: DateScope = DateScope.ALL,
    val customDateFrom: String? = null,
    val customDateTo: String? = null,
    val categoryFilter: String? = null,
    val paymentMethodFilter: PaymentMethod? = null,
    val visibleCount: Int = PageSize,
    val errorMessage: String? = null
) {
    /**
     * Catálogo completo de categorías de la app (no solo las que el usuario ya usó), para que
     * el filtro de categoría siga apareciendo aunque el usuario todavía no tenga movimientos
     * registrados.
     */
    val availableCategories: List<String>
        get() = AllTransactionCategories

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
            .filter { transaction -> matchesDateScope(transaction.date, dateScope, customDateFrom, customDateTo) }
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

/** Si la fecha de la transacción no se puede interpretar, no se oculta por este filtro. */
private fun matchesDateScope(
    dateValue: String,
    scope: DateScope,
    customFrom: String?,
    customTo: String?
): Boolean {
    if (scope == DateScope.ALL) return true
    val parsed = parseFormFieldDate(dateValue) ?: return true
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    return when (scope) {
        DateScope.ALL -> true
        DateScope.CURRENT_MONTH -> parsed.year == today.year && parsed.month == today.month
        DateScope.CURRENT_YEAR -> parsed.year == today.year
        DateScope.LAST_3_MONTHS -> {
            val threshold = today.minus(DatePeriod(months = 3))
            parsed in threshold..today
        }
        DateScope.CUSTOM -> {
            val from = customFrom?.let { parseFormFieldDate(it) }
            val to = customTo?.let { parseFormFieldDate(it) }
            (from == null || parsed >= from) && (to == null || parsed <= to)
        }
    }
}
