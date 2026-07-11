package fintrack.proyecto4.transaction

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore

/**
 * Persiste transacciones en users/{uid}/transactions/{transactionId}.
 * Ya cubierto por las reglas de seguridad existentes del proyecto (mismo patrón que
 * FirestoreBudgetRepository/FirestoreOnboardingRepository).
 *
 * La lectura de cada documento usa valores por defecto para todos los campos, para que
 * la entidad pueda evolucionar (agregar campos nuevos) sin romper documentos guardados
 * por versiones anteriores de la app.
 */
class FirestoreTransactionRepository : TransactionRepository {

    private val db = Firebase.firestore

    private fun col(uid: String) = db.collection("users").document(uid).collection("transactions")

    override suspend fun getTransactions(uid: String): List<Transaction> {
        return try {
            col(uid).get().documents.mapNotNull { doc ->
                runCatching {
                    Transaction(
                        id = doc.id,
                        type = doc.get<String?>("type")
                            ?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }
                            ?: TransactionType.EXPENSE,
                        amount = doc.get<Long?>("amount") ?: 0L,
                        description = doc.get<String?>("description") ?: "",
                        category = doc.get<String?>("category") ?: "",
                        paymentMethod = doc.get<String?>("paymentMethod")
                            ?.let { runCatching { PaymentMethod.valueOf(it) }.getOrNull() },
                        date = doc.get<String?>("date") ?: "",
                        createdAt = doc.get<Long?>("createdAt") ?: 0L
                    )
                }.getOrNull()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun addTransaction(uid: String, transaction: Transaction) {
        val ref = if (transaction.id.isBlank()) col(uid).document else col(uid).document(transaction.id)
        ref.set(toMap(transaction))
    }

    override suspend fun updateTransaction(uid: String, transaction: Transaction) {
        col(uid).document(transaction.id).set(toMap(transaction))
    }

    override suspend fun deleteTransaction(uid: String, transactionId: String) {
        col(uid).document(transactionId).delete()
    }

    private fun toMap(transaction: Transaction) = mapOf(
        "type" to transaction.type.name,
        "amount" to transaction.amount,
        "description" to transaction.description,
        "category" to transaction.category,
        "paymentMethod" to transaction.paymentMethod?.name,
        "date" to transaction.date,
        "createdAt" to transaction.createdAt
    )
}
