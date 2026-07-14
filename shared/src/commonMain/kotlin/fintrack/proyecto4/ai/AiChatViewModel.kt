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

class AiChatViewModel(
    private val transactionRepository: TransactionRepository,
    private val uid: String
) : ViewModel() {

    private val client = GroqClient()

    private val _messages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val messages: StateFlow<List<AiChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        _messages.value = listOf(
            AiChatMessage(
                role = ChatRole.MODEL,
                content = "¡Hola! Soy tu asistente financiero de FinTrack. " +
                        "Puedo ayudarte a analizar tus gastos, revisar tus presupuestos y darte consejos personalizados. " +
                        "¿En qué te puedo ayudar hoy?"
            )
        )
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank() || _isLoading.value) return

        val userMsg = AiChatMessage(role = ChatRole.USER, content = userText.trim())
        _messages.value = _messages.value + userMsg
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val transactions = transactionRepository.getTransactions(uid)
                val systemPrompt = buildSystemPrompt(transactions)
                val history = _messages.value.dropLast(1)

                val reply = client.chat(
                    systemPrompt = systemPrompt,
                    history = history,
                    userMessage = userText.trim()
                )

                _messages.value = _messages.value + AiChatMessage(role = ChatRole.MODEL, content = reply)
            } catch (e: Exception) {
                val detail = e.message ?: "Error desconocido"
                _messages.value = _messages.value + AiChatMessage(
                    role = ChatRole.MODEL,
                    content = "⚠️ Error: $detail"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() { _error.value = null }

    private fun buildSystemPrompt(transactions: List<Transaction>): String {
        val ingresos = transactions.filter { it.type == TransactionType.INCOME }
        val gastos = transactions.filter { it.type == TransactionType.EXPENSE }

        val totalIngresos = ingresos.sumOf { it.amount }
        val totalGastos = gastos.sumOf { it.amount }
        val balance = totalIngresos - totalGastos

        val categorias = gastos
            .groupBy { it.category }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .joinToString(", ") { "${it.key}: ₡${it.value}" }

        val ultimosGastos = gastos.take(10).joinToString("\n") {
            "- ${it.date}: ${it.description} (${it.category}) ₡${it.amount}"
        }

        return """
Eres un asistente financiero personal integrado en FinTrack, una app de finanzas personales en Costa Rica.
Responde SIEMPRE en español, de forma concisa, amigable y útil. Sin markdown excesivo.

Datos financieros del usuario:
- Balance actual: ₡$balance
- Total ingresos: ₡$totalIngresos (${ingresos.size} transacciones)
- Total gastos: ₡$totalGastos (${gastos.size} transacciones)
- Top categorías de gasto: $categorias

Últimos gastos:
$ultimosGastos

Usa estos datos para responder preguntas sobre sus finanzas. Si el usuario pregunta algo que no está en los datos, indícalo amablemente.
        """.trimIndent()
    }
}
