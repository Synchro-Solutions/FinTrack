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
                        alertThreshold = try { doc.get<Double>("alertThreshold").toFloat() } catch (_: Exception) { 0.8f },
                        periodKey = try { doc.get<String>("periodKey") } catch (_: Exception) { "" },
                        isActive = try { doc.get<Boolean>("isActive") } catch (_: Exception) { true },
                        updatedAt = try { doc.get<Long>("updatedAt") } catch (_: Exception) { 0L },
                        alertSent = try { doc.get<Boolean>("alertSent") } catch (_: Exception) { false },
                        exceededSent = try { doc.get<Boolean>("exceededSent") } catch (_: Exception) { false }
                    )
                }.getOrNull()
            }
            .filter { it.isActive }
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
                "alertThreshold" to item.alertThreshold.toDouble(),
                "periodKey" to item.periodKey
            )
        )
    }

    override suspend fun updateSpent(uid: String, budgetId: String, spent: Double) {
        col(uid).document(budgetId).update("spent" to spent)
    }

    override suspend fun deleteBudget(uid: String, budgetId: String) {
        col(uid).document(budgetId).delete()
    }

    override suspend fun updateBudget(uid: String, budgetId: String, newLimit: Double, newThreshold: Float) {
        col(uid).document(budgetId).update(
            "limit" to newLimit,
            "alertThreshold" to newThreshold.toDouble(),
            "updatedAt" to System.currentTimeMillis()
        )
    }

    override suspend fun deactivateBudget(uid: String, budgetId: String) {
        col(uid).document(budgetId).update(
            "isActive" to false,
            "updatedAt" to System.currentTimeMillis()
        )
    }

    override suspend fun markAlertSent(uid: String, budgetId: String, alertSent: Boolean, exceededSent: Boolean) {
        col(uid).document(budgetId).update(
            "alertSent" to alertSent,
            "exceededSent" to exceededSent
        )
    }
}
