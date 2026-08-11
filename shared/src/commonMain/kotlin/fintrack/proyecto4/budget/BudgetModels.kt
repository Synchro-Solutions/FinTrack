package fintrack.proyecto4.budget

import androidx.compose.ui.graphics.Color
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import kotlin.time.Clock

enum class BudgetStatus { OK, WARNING, CRITICAL }

data class BudgetCategory(
    val name: String,
    val icon: String,
    val colorHex: String
)

// Todas las categorías usan tonos de la familia verde oficial de la marca (no colores
// arbitrarios sin relación con la paleta), aunque eso signifique que se distingan menos
// entre sí sobre el fondo degradado.
val BUDGET_CATEGORIES = listOf(
    BudgetCategory("Alimentación",    "🛒", "#1F6B54"),
    BudgetCategory("Transporte",      "🚌", "#26705B"),
    BudgetCategory("Vivienda",        "🏠", "#2F7D65"),
    BudgetCategory("Servicios",       "⚡", "#3B8F74"),
    BudgetCategory("Salud",           "🍎", "#4FA184"),
    BudgetCategory("Entretenimiento", "🎮", "#5FAF93"),
    BudgetCategory("Ropa",            "👕", "#62C9A7"),
    BudgetCategory("Educación",       "📚", "#7DD4B7"),
    BudgetCategory("Otro",            "📦", "#5A9683")
)

data class BudgetItem(
    val id: String,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: Color,
    val spent: Double,
    val limit: Double,
    val period: String = "mensual",
    val alertThreshold: Float = 0.8f,
    /** Mes ("yyyy-MM") al que corresponde el [spent] actual. Si no coincide con
     *  [currentBudgetPeriodKey], el próximo recálculo lo trata como un período nuevo. */
    val spentPeriodKey: String = "",
    /** Ya se disparó la alerta de umbral para este presupuesto en [spentPeriodKey]. */
    val alertSent: Boolean = false
) {
    val usagePct: Float get() = if (limit > 0) (spent / limit).toFloat().coerceAtMost(1f) else 0f
    val remaining: Double get() = limit - spent
    val status: BudgetStatus get() = when {
        usagePct >= 0.90f -> BudgetStatus.CRITICAL
        usagePct >= alertThreshold -> BudgetStatus.WARNING
        else -> BudgetStatus.OK
    }
}

/** Período mensual actual en formato "yyyy-MM", usado para saber a qué mes corresponde
 *  el `spent` acumulado de un presupuesto. */
@OptIn(kotlin.time.ExperimentalTime::class)
fun currentBudgetPeriodKey(): String {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    return "${today.year}-${today.month.number.toString().padStart(2, '0')}"
}

data class BudgetListState(
    val budgets: List<BudgetItem> = emptyList(),
    val isLoading: Boolean = true
) {
    val totalLimit: Double get() = budgets.sumOf { it.limit }
    val totalSpent: Double get() = budgets.sumOf { it.spent }
    val totalAvailable: Double get() = totalLimit - totalSpent
}
