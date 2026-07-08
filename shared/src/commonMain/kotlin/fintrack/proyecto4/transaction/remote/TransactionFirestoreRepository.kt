package fintrack.proyecto4.transaction.remote

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import fintrack.proyecto4.auth.AuthClient
import fintrack.proyecto4.transaction.PaymentMethod
import fintrack.proyecto4.transaction.Transaction
import fintrack.proyecto4.transaction.TransactionType

class TransactionFirestoreRepository {

    private val firestore = Firebase.firestore

    private fun currentUserId(): String {
        return AuthClient.currentUserId()
            ?: error("Usuario no autenticado")
    }

    private fun transactionsCollection() =
        firestore
            .collection("users")
            .document(currentUserId())
            .collection("transactions")

    suspend fun getTransactions(): List<Transaction> {
        return transactionsCollection()
            .get()
            .documents
            .map { document ->
                Transaction(
                    id = document.get<String?>("id") ?: document.id,
                    type = TransactionType.valueOf(
                        document.get<String?>("type") ?: TransactionType.EXPENSE.name
                    ),
                    amount = document.get<Long?>("amount") ?: 0L,
                    description = document.get<String?>("description") ?: "",
                    category = document.get<String?>("category") ?: "",
                    paymentMethod = document.get<String?>("paymentMethod")
                        ?.let { PaymentMethod.valueOf(it) },
                    date = document.get<String?>("date") ?: "",
                    createdAt = document.get<Long?>("createdAt") ?: 0L
                )
            }
    }

    suspend fun saveTransaction(transaction: Transaction) {
        transactionsCollection()
            .document(transaction.id)
            .set(
                mapOf(
                    "id" to transaction.id,
                    "type" to transaction.type.name,
                    "amount" to transaction.amount,
                    "description" to transaction.description,
                    "category" to transaction.category,
                    "paymentMethod" to transaction.paymentMethod?.name,
                    "date" to transaction.date,
                    "createdAt" to transaction.createdAt
                )
            )
    }

    suspend fun deleteTransaction(id: String) {
        transactionsCollection().document(id).delete()
    }
}
