package fintrack.proyecto4.budget

import androidx.compose.ui.graphics.Color

enum class BudgetStatus { OK, WARNING, CRITICAL }

data class BudgetItem(
    val id: String,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: Color,
    val spent: Double,
    val limit: Double,
    val period: String = "mensual"
) {
    val usagePct: Float get() = if (limit > 0) (spent / limit).toFloat().coerceAtMost(1f) else 0f
    val remaining: Double get() = limit - spent
    val status: BudgetStatus get() = when {
        usagePct >= 0.90f -> BudgetStatus.CRITICAL
        usagePct >= 0.70f -> BudgetStatus.WARNING
        else -> BudgetStatus.OK
    }
}

data class BudgetListState(
    val budgets: List<BudgetItem> = emptyList(),
    val isLoading: Boolean = true
) {
    val totalLimit: Double get() = budgets.sumOf { it.limit }
    val totalSpent: Double get() = budgets.sumOf { it.spent }
    val totalAvailable: Double get() = totalLimit - totalSpent
}
