package fintrack.proyecto4.transaction

/**
 * Transacción persistida (ingreso o gasto), ya sea capturada manualmente o vía
 * el asistente OCR.
 */
data class Transaction(
    val id: String,
    val type: TransactionType,
    val amount: Long,
    val description: String,
    val category: String,
    val paymentMethod: PaymentMethod?,
    val date: String,
    val createdAt: Long = 0L
)
