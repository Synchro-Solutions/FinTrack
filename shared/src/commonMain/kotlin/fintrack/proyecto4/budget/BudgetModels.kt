package fintrack.proyecto4.budget

import androidx.compose.ui.graphics.Color

enum class BudgetStatus { OK, WARNING, CRITICAL }

data class BudgetCategory(
    val name: String,
    val icon: String,
    val colorHex: String
)

val BUDGET_CATEGORIES = listOf(
    BudgetCategory("Alimentación",    "🛒", "#818CF8"),
    BudgetCategory("Transporte",      "🚌", "#3B82F6"),
    BudgetCategory("Vivienda",        "🏠", "#FF6B6B"),
    BudgetCategory("Servicios",       "⚡", "#F59E0B"),
    BudgetCategory("Salud",           "🍎", "#22C55E"),
    BudgetCategory("Entretenimiento", "🎮", "#A78BFA"),
    BudgetCategory("Ropa",            "👕", "#F472B6"),
    BudgetCategory("Educación",       "📚", "#06B6D4"),
    BudgetCategory("Otro",            "📦", "#94A3B8")
)

data class BudgetItem(
    val id: String,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: Color,
    val spent: Double,
    val limit: Double,
    val period: String = "mensual",
    val alertThreshold: Float = 0.8f
) {
    val usagePct: Float get() = if (limit > 0) (spent / limit).toFloat().coerceAtMost(1f) else 0f
    val remaining: Double get() = limit - spent
    val status: BudgetStatus get() = when {
        usagePct >= 0.90f -> BudgetStatus.CRITICAL
        usagePct >= alertThreshold -> BudgetStatus.WARNING
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
