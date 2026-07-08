package fintrack.proyecto4.transaction

/**
 * Persiste transacciones en users/{uid}/transactions/{transactionId}.
 * El uid se resuelve en la capa de UI (AuthClient.currentUserId()) y se pasa explícito
 * a cada método, igual que BudgetRepository/OnboardingRepository — así el repositorio no
 * depende de ningún estado global de sesión y es trivial de testear con un uid fijo.
 */
interface TransactionRepository {
    suspend fun getTransactions(uid: String): List<Transaction>
    suspend fun addTransaction(uid: String, transaction: Transaction)
    suspend fun updateTransaction(uid: String, transaction: Transaction)
    suspend fun deleteTransaction(uid: String, transactionId: String)
}

/** Implementación por defecto para plataformas sin Firestore (Web, tests). */
class NoOpTransactionRepository : TransactionRepository {
    override suspend fun getTransactions(uid: String): List<Transaction> = emptyList()
    override suspend fun addTransaction(uid: String, transaction: Transaction) = Unit
    override suspend fun updateTransaction(uid: String, transaction: Transaction) = Unit
    override suspend fun deleteTransaction(uid: String, transactionId: String) = Unit
}
