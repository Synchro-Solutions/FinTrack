package fintrack.proyecto4.transaction

import androidx.compose.runtime.mutableStateListOf
import fintrack.proyecto4.transaction.remote.TransactionFirestoreRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class TransactionRepository(
    private val remoteRepository: TransactionFirestoreRepository = TransactionFirestoreRepository()
) {

    companion object {
        private val transactions = mutableStateListOf<Transaction>()
    }

    fun getTransactions(): List<Transaction> =
        transactions.sortedByDescending { it.createdAt }

    fun getTransaction(id: String): Transaction? =
        transactions.firstOrNull { it.id == id }

    suspend fun loadFromFirestore(): Result<Unit> {
        return try {
            val remoteTransactions = remoteRepository.getTransactions()
            transactions.clear()
            transactions.addAll(remoteTransactions)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTransaction(
        type: TransactionType,
        amount: Long,
        description: String,
        category: String,
        paymentMethod: PaymentMethod?,
        date: String
    ): Result<Transaction> {
        if (amount <= 0) return Result.failure(Exception("El monto debe ser mayor a 0"))
        if (description.isBlank()) return Result.failure(Exception("La descripción no puede estar vacía"))
        if (category.isBlank()) return Result.failure(Exception("Selecciona una categoría"))

        val transaction = Transaction(
            id = generateTransactionId(),
            type = type,
            amount = amount,
            description = description.trim(),
            category = category,
            paymentMethod = paymentMethod,
            date = date,
            createdAt = currentEpochMillis()
        )

        transactions.add(0, transaction)

        return try {
            remoteRepository.saveTransaction(transaction)
            Result.success(transaction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTransaction(
        id: String,
        type: TransactionType,
        amount: Long,
        description: String,
        category: String,
        paymentMethod: PaymentMethod?,
        date: String
    ): Result<Transaction> {
        val index = transactions.indexOfFirst { it.id == id }
        if (index == -1) return Result.failure(Exception("Transacción no encontrada"))

        if (amount <= 0) return Result.failure(Exception("El monto debe ser mayor a 0"))
        if (description.isBlank()) return Result.failure(Exception("La descripción no puede estar vacía"))
        if (category.isBlank()) return Result.failure(Exception("Selecciona una categoría"))

        val updated = transactions[index].copy(
            type = type,
            amount = amount,
            description = description.trim(),
            category = category,
            paymentMethod = paymentMethod,
            date = date
        )

        transactions[index] = updated

        return try {
            remoteRepository.saveTransaction(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(id: String): Result<Unit> {
        val existed = transactions.removeAll { it.id == id }
        if (!existed) return Result.failure(Exception("Transacción no encontrada"))

        return try {
            remoteRepository.deleteTransaction(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun currentEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()

    private fun generateTransactionId(): String {
        return "tx_${transactions.size + 1}_${kotlin.random.Random.nextInt(1000, 9999)}"
    }
}
