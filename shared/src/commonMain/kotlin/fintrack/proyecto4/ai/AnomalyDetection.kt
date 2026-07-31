package fintrack.proyecto4.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fintrack.proyecto4.transaction.Transaction
import fintrack.proyecto4.transaction.TransactionRepository
import fintrack.proyecto4.transaction.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class AnomalyAlert(
    val category: String,
    val amount: Long,
    val average: Long,
    val message: String
)

object AnomalyAlertBus {
    private val _current = MutableStateFlow<AnomalyAlert?>(null)
    val current: StateFlow<AnomalyAlert?> = _current.asStateFlow()

    fun post(alert: AnomalyAlert) { _current.value = alert }
    fun dismiss() { _current.value = null }
}

private const val MinHistoryForCategory = 3

private const val AnomalyFactor = 2.0

class AnomalyDetector(
    private val client: GroqClient = GroqClient()
) {

    suspend fun analyze(newTx: Transaction, priorHistory: List<Transaction>): AnomalyAlert? {
        if (newTx.type != TransactionType.EXPENSE) return null

        val montos = priorHistory
            .filter { it.type == TransactionType.EXPENSE && it.category == newTx.category }
            .map { it.amount }

        if (montos.size < MinHistoryForCategory) return null

        val avg = montos.average()
        if (avg <= 0.0) return null

        if (newTx.amount <= AnomalyFactor * avg) return null

        val stddev = standardDeviation(montos, avg)
        val message = buildMessage(newTx.category, newTx.amount, avg.toLong(), stddev.toLong())

        return AnomalyAlert(
            category = newTx.category,
            amount = newTx.amount,
            average = avg.toLong(),
            message = message
        )
    }

    @OptIn(ExperimentalTime::class)
    fun weeklyAnomalies(history: List<Transaction>): List<AnomalyAlert> {
        val now = Clock.System.now().toEpochMilliseconds()
        val weekAgo = now - 7L * 24 * 60 * 60 * 1000

        val gastos = history.filter { it.type == TransactionType.EXPENSE }
        val recientes = gastos.filter { it.createdAt >= weekAgo }

        return recientes.mapNotNull { tx ->
            val montos = gastos
                .filter { it.category == tx.category && it.id != tx.id }
                .map { it.amount }
            if (montos.size < MinHistoryForCategory) return@mapNotNull null
            val avg = montos.average()
            if (avg <= 0.0 || tx.amount <= AnomalyFactor * avg) return@mapNotNull null
            AnomalyAlert(
                category = tx.category,
                amount = tx.amount,
                average = avg.toLong(),
                message = "${tx.description.ifBlank { tx.category }}: ${formatColones(tx.amount)} " +
                    "(promedio ${formatColones(avg.toLong())})"
            )
        }
    }

    private suspend fun buildMessage(category: String, amount: Long, average: Long, stddev: Long): String {
        val fallback = "Este gasto es inusualmente alto para $category. Tu promedio es ${formatColones(average)}."
        return try {
            val context = "{\"categoria\":\"$category\",\"monto\":$amount,\"promedio\":$average,\"desviacionEstandar\":$stddev}"
            val reply = client.chat(
                systemPrompt = ANOMALY_SYSTEM_PROMPT,
                history = emptyList(),
                userMessage = context
            ).trim()
            reply.ifBlank { fallback }
        } catch (_: Exception) {
            fallback
        }
    }

    private fun standardDeviation(values: List<Long>, mean: Double): Double {
        if (values.size < 2) return 0.0
        val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
        return sqrt(variance)
    }
}

data class WeeklyAnomalyState(
    val anomalies: List<AnomalyAlert> = emptyList(),
    val checked: Boolean = false
)

class WeeklyAnomalyViewModel(
    private val transactionRepository: TransactionRepository,
    private val uid: String,
    private val detector: AnomalyDetector = AnomalyDetector()
) : ViewModel() {

    private val _state = MutableStateFlow(WeeklyAnomalyState())
    val state: StateFlow<WeeklyAnomalyState> = _state.asStateFlow()

    init {
        analyze()
    }

    fun analyze() {
        viewModelScope.launch {
            val history = try {
                transactionRepository.getTransactions(uid)
            } catch (_: Exception) {
                emptyList()
            }
            _state.value = WeeklyAnomalyState(
                anomalies = detector.weeklyAnomalies(history),
                checked = true
            )
        }
    }
}

private fun formatColones(amount: Long): String {
    val n = amount.toString().reversed().chunked(3).joinToString(" ").reversed()
    return "₡$n"
}

private val ANOMALY_SYSTEM_PROMPT = """
Eres el sistema de alertas de FinTrack (finanzas personales en Costa Rica, colones ₡).
Recibirás un JSON con un gasto inusual: categoria, monto, promedio y desviacionEstandar.
Devuelve UNA sola oración en español, breve y no alarmista, indicando que el gasto es
inusualmente alto para esa categoría y mencionando el promedio con símbolo ₡.
Sin markdown, sin comillas, sin saltos de línea. Ejemplo del estilo:
"Este gasto es inusualmente alto para Entretenimiento. Tu promedio es ₡25 000."
""".trimIndent()
