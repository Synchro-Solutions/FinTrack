package fintrack.proyecto4.budget

import androidx.compose.ui.graphics.Color
import fintrack.proyecto4.transaction.Transaction
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import kotlin.time.Clock

enum class BudgetStatus { OK, WARNING, CRITICAL, EXCEEDED }

fun currentPeriodKey(): String {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    return "${today.year}-${today.monthNumber.toString().padStart(2, '0')}"
}

fun periodKeyOfDate(date: String): String? {
    val parts = date.split("/")
    if (parts.size != 3) return null
    val month = parts[1].toIntOrNull() ?: return null
    val year = parts[2].toIntOrNull() ?: return null
    return "$year-${month.toString().padStart(2, '0')}"
}

fun periodLabelFromKey(key: String): String {
    val parts = key.split("-")
    if (parts.size != 2) return key
    val year = parts[0]
    val month = parts[1].toIntOrNull() ?: return key
    val name = when (month) {
        1 -> "Enero"; 2 -> "Febrero"; 3 -> "Marzo"; 4 -> "Abril"
        5 -> "Mayo"; 6 -> "Junio"; 7 -> "Julio"; 8 -> "Agosto"
        9 -> "Septiembre"; 10 -> "Octubre"; 11 -> "Noviembre"; 12 -> "Diciembre"
        else -> return key
    }
    return "$name $year"
}

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
    val periodKey: String = "",
    val isActive: Boolean = true,
    val updatedAt: Long = 0L,
    val alertSent: Boolean = false,
    val exceededSent: Boolean = false
) {
    val rawUsagePct: Float get() = if (limit > 0) (spent / limit).toFloat() else 0f
    val usagePct: Float get() = rawUsagePct.coerceAtMost(1f)
    val usagePercentInt: Int get() = (rawUsagePct * 100).toInt()
    val remaining: Double get() = limit - spent

    val status: BudgetStatus get() = when {
        rawUsagePct >= 1.00f -> BudgetStatus.EXCEEDED
        rawUsagePct >= 0.90f -> BudgetStatus.CRITICAL
        rawUsagePct >= 0.70f -> BudgetStatus.WARNING
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
    val isLoading: Boolean = true,
    val availablePeriods: List<String> = emptyList(),
    val selectedPeriod: String = "",
    val transactions: List<Transaction> = emptyList()
) {
    val totalLimit: Double get() = budgets.sumOf { it.limit }
    val totalSpent: Double get() = budgets.sumOf { it.spent }
    val totalAvailable: Double get() = totalLimit - totalSpent
}
