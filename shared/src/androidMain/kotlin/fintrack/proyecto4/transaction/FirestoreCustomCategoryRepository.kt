package fintrack.proyecto4.transaction

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore

/**
 * Persiste categorías personalizadas en users/{uid}/categories/{id} — misma ruta ya
 * habilitada en firestore.rules (match /users/{userId}/categories/{categoryId}, con
 * lectura/escritura completa para el dueño), mismo patrón que
 * FirestoreBudgetRepository/FirestoreTransactionRepository.
 */
class FirestoreCustomCategoryRepository : CustomCategoryRepository {

    private val db = Firebase.firestore

    private fun col(uid: String) = db.collection("users").document(uid).collection("categories")

    override suspend fun getCategories(uid: String): List<CustomCategory> {
        return try {
            col(uid).get().documents.mapNotNull { doc ->
                runCatching {
                    CustomCategory(
                        id = doc.id,
                        name = doc.get<String?>("name") ?: "",
                        type = doc.get<String?>("type")
                            ?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }
                            ?: TransactionType.EXPENSE,
                        icon = doc.get<String?>("icon")
                    )
                }.getOrNull()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun addCategory(uid: String, category: CustomCategory): CustomCategory {
        val ref = col(uid).document
        ref.set(toMap(category))
        return category.copy(id = ref.id)
    }

    override suspend fun updateCategory(uid: String, category: CustomCategory) {
        col(uid).document(category.id).set(toMap(category))
    }

    override suspend fun deleteCategory(uid: String, categoryId: String) {
        col(uid).document(categoryId).delete()
    }

    private fun toMap(category: CustomCategory) = mapOf(
        "name" to category.name,
        "type" to category.type.name,
        "icon" to category.icon
    )
}
