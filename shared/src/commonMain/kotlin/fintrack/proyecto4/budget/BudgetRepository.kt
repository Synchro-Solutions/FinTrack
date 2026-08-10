package fintrack.proyecto4.budget

import androidx.compose.ui.graphics.Color

interface BudgetRepository {
    suspend fun getBudgets(uid: String): List<BudgetItem>
    suspend fun addBudget(uid: String, item: BudgetItem)
    suspend fun updateSpent(uid: String, budgetId: String, spent: Double)
    suspend fun deleteBudget(uid: String, budgetId: String)
    suspend fun updateBudget(uid: String, budgetId: String, newLimit: Double, newThreshold: Float)
    suspend fun deactivateBudget(uid: String, budgetId: String)
    suspend fun markAlertSent(uid: String, budgetId: String, alertSent: Boolean, exceededSent: Boolean)
}

class NoOpBudgetRepository : BudgetRepository {
    override suspend fun getBudgets(uid: String): List<BudgetItem> = emptyList()
    override suspend fun addBudget(uid: String, item: BudgetItem) = Unit
    override suspend fun updateSpent(uid: String, budgetId: String, spent: Double) = Unit
    override suspend fun deleteBudget(uid: String, budgetId: String) = Unit
    override suspend fun updateBudget(uid: String, budgetId: String, newLimit: Double, newThreshold: Float) = Unit
    override suspend fun deactivateBudget(uid: String, budgetId: String) = Unit
    override suspend fun markAlertSent(uid: String, budgetId: String, alertSent: Boolean, exceededSent: Boolean) = Unit
}

fun colorFromHex(hex: String): Color {
    val clean = hex.trimStart('#')
    return try {
        when (clean.length) {
            6 -> Color(
                red = clean.substring(0, 2).toInt(16),
                green = clean.substring(2, 4).toInt(16),
                blue = clean.substring(4, 6).toInt(16)
            )
            else -> Color(0xFF818CF8)
        }
    } catch (e: Exception) {
        Color(0xFF818CF8)
    }
}

fun colorToHex(color: Color): String {
    val r = (color.red * 255).toInt().coerceIn(0, 255)
    val g = (color.green * 255).toInt().coerceIn(0, 255)
    val b = (color.blue * 255).toInt().coerceIn(0, 255)
    return "#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}".uppercase()
}
