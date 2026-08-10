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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import kotlin.time.Clock

data class MonthlySummaryState(
    val summary: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasRequested: Boolean = false
)

class MonthlySummaryViewModel(
    private val transactionRepository: TransactionRepository,
    private val uid: String
) : ViewModel() {

    companion object {
        private val dailyCache = mutableMapOf<String, String>()
    }

    private val client = GroqClient()

    private val _state = MutableStateFlow(MonthlySummaryState())
    val state: StateFlow<MonthlySummaryState> = _state.asStateFlow()

    fun generateSummary(force: Boolean = false) {
        if (_state.value.isLoading) return

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val dayKey = "$uid|${today.year}-${pad(today.month.number)}-${pad(today.day)}"

        if (!force) {
            dailyCache[dayKey]?.let { cached ->
                _state.value = MonthlySummaryState(summary = cached, hasRequested = true)
                return
            }
        }

        _state.value = _state.value.copy(isLoading = true, error = null, hasRequested = true)

        viewModelScope.launch {
            try {
                val transactions = transactionRepository.getTransactions(uid)
                val context = buildContextJson(transactions)
                val reply = client.chat(
                    systemPrompt = SUMMARY_SYSTEM_PROMPT,
                    history = emptyList(),
                    userMessage = "Genera el resumen del mes con estos datos en JSON:\n$context"
                ).trim()

                dailyCache[dayKey] = reply
                _state.value = MonthlySummaryState(summary = reply, hasRequested = true)
            } catch (e: Exception) {
                _state.value = MonthlySummaryState(
                    error = e.message ?: "No se pudo generar el resumen. Intenta de nuevo.",
                    hasRequested = true
                )
            }
        }
    }

    private fun buildContextJson(transactions: List<Transaction>): String {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val currentKey = "${today.year}-${pad(today.month.number)}"

        var prevMonth = today.month.number - 1
        var prevYear = today.year
        if (prevMonth == 0) { prevMonth = 12; prevYear -= 1 }
        val prevKey = "$prevYear-${pad(prevMonth)}"

        val gastos = transactions.filter { it.type == TransactionType.EXPENSE }
        val gastosActual = gastos.filter { periodKeyOf(it.date) == currentKey }
        val gastosPrevio = gastos.filter { periodKeyOf(it.date) == prevKey }

        val totalActual = gastosActual.sumOf { it.amount }
        val totalPrevio = gastosPrevio.sumOf { it.amount }

        val top3 = gastosActual
            .groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .joinToString(", ") { "{\"categoria\":\"${it.key}\",\"monto\":${it.value}}" }

        return """
{
  "moneda": "CRC",
  "mesActual": {
    "nombre": "${monthLabel(today.month.number)} ${today.year}",
    "totalGastado": $totalActual,
    "numTransacciones": ${gastosActual.size},
    "top3Categorias": [$top3]
  },
  "mesAnterior": {
    "nombre": "${monthLabel(prevMonth)} $prevYear",
    "totalGastado": $totalPrevio,
    "numTransacciones": ${gastosPrevio.size}
  }
}
        """.trimIndent()
    }

    private fun periodKeyOf(date: String): String? {
        val parts = date.split("/")
        if (parts.size != 3) return null
        val month = parts[1].toIntOrNull() ?: return null
        val year = parts[2].toIntOrNull() ?: return null
        return "$year-${pad(month)}"
    }

    private fun pad(n: Int): String = n.toString().padStart(2, '0')

    private fun monthLabel(month: Int): String = when (month) {
        1 -> "Enero"; 2 -> "Febrero"; 3 -> "Marzo"; 4 -> "Abril"
        5 -> "Mayo"; 6 -> "Junio"; 7 -> "Julio"; 8 -> "Agosto"
        9 -> "Septiembre"; 10 -> "Octubre"; 11 -> "Noviembre"; 12 -> "Diciembre"
        else -> ""
    }
}

private val SUMMARY_SYSTEM_PROMPT = """
Eres un asistente financiero de FinTrack (app de finanzas personales en Costa Rica, moneda colones ₡).
Con los datos del mes que te den en JSON, escribe un RESUMEN NARRATIVO en español de 3 a 5 oraciones.
El resumen DEBE incluir:
- El total gastado del mes actual.
- La categoría principal de gasto.
- Una comparación con el mes anterior (si gastó más o menos y en cuánto).
- Una observación o recomendación relevante.
Reglas:
- Tono cercano y claro, como si le contaras a la persona cómo le fue.
- Un solo párrafo, sin viñetas, sin markdown, sin títulos.
- Usa montos en colones con el símbolo ₡ cuando corresponda.
- Si el mes actual no tiene gastos, indícalo amablemente en una o dos oraciones.
""".trimIndent()
