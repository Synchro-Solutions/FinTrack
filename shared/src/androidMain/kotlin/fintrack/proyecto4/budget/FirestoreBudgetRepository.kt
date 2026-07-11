package fintrack.proyecto4.budget

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore

/**
 * Persiste presupuestos en users/{uid}/budgets/{budgetId}.
 * Ya cubierto por las reglas de seguridad existentes del proyecto.
 */
class FirestoreBudgetRepository : BudgetRepository {

    private val db = Firebase.firestore

    private fun col(uid: String) = db.collection("users").document(uid).collection("budgets")

    override suspend fun getBudgets(uid: String): List<BudgetItem> {
        return try {
            col(uid).get().documents.mapNotNull { doc ->
                runCatching {
                    BudgetItem(
                        id = doc.id,
                        categoryName = doc.get("categoryName"),
                        categoryIcon = doc.get("categoryIcon"),
                        categoryColor = colorFromHex(doc.get("categoryColorHex")),
                        spent = doc.get<Double>("spent"),
                        limit = doc.get<Double>("limit"),
                        period = doc.get("period"),
                        alertThreshold = try { doc.get<Double>("alertThreshold").toFloat() } catch (_: Exception) { 0.8f }
                    )
                }.getOrNull()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun addBudget(uid: String, item: BudgetItem) {
        val ref = if (item.id.isBlank()) col(uid).document else col(uid).document(item.id)
        ref.set(
            mapOf(
                "categoryName" to item.categoryName,
                "categoryIcon" to item.categoryIcon,
                "categoryColorHex" to colorToHex(item.categoryColor),
                "spent" to item.spent,
                "limit" to item.limit,
                "period" to item.period,
                "alertThreshold" to item.alertThreshold.toDouble()
            )
        )
    }

    override suspend fun updateSpent(uid: String, budgetId: String, spent: Double) {
        col(uid).document(budgetId).update("spent" to spent)
    }

    override suspend fun deleteBudget(uid: String, budgetId: String) {
        col(uid).document(budgetId).delete()
    }
}
