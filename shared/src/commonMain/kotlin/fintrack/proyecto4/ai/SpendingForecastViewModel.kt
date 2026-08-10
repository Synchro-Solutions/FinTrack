package fintrack.proyecto4.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fintrack.proyecto4.budget.BudgetRepository
import fintrack.proyecto4.transaction.Transaction
import fintrack.proyecto4.transaction.TransactionRepository
import fintrack.proyecto4.transaction.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import kotlin.time.Clock

data class CategoryForecast(
    val categoria: String,
    val prediccion: Long,
    val presupuesto: Long?,
    val excedePresupuesto: Boolean,
    val comentario: String
)

data class ForecastState(
    val isLoading: Boolean = false,
    val forecasts: List<CategoryForecast> = emptyList(),
    val error: String? = null,
    val insufficientHistory: Boolean = false,
    val monthsAvailable: Int = 0,
    val hasRequested: Boolean = false
)

@Serializable
private data class CommentDto(
    val categoria: String = "",
    val comentario: String = ""
)

class SpendingForecastViewModel(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val uid: String
) : ViewModel() {

    companion object {
        private val dailyCache = mutableMapOf<String, ForecastState>()
    }

    private val client = GroqClient()
    private val json = Json { ignoreUnknownKeys = true }

    private val _state = MutableStateFlow(ForecastState())
    val state: StateFlow<ForecastState> = _state.asStateFlow()

    fun generateForecast(force: Boolean = false) {
        if (_state.value.isLoading) return

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val dayKey = "$uid|${today.year}-${pad(today.month.number)}-${pad(today.day)}"

        if (!force) {
            dailyCache[dayKey]?.let { cached ->
                _state.value = cached
                return
            }
        }

        _state.value = _state.value.copy(isLoading = true, error = null, hasRequested = true)

        viewModelScope.launch {
            try {
                val transactions = transactionRepository.getTransactions(uid)
                val budgets = try { budgetRepository.getBudgets(uid) } catch (_: Exception) { emptyList() }

                val months = lastThreeMonthKeys(today.year, today.month.number)
                val gastos = transactions.filter { it.type == TransactionType.EXPENSE }

                val presentMonths = months.filter { mk -> gastos.any { periodKeyOf(it.date) == mk } }
                val monthsAvailable = presentMonths.size

                if (monthsAvailable < 2) {
                    val result = ForecastState(
                        isLoading = false,
                        insufficientHistory = true,
                        monthsAvailable = monthsAvailable,
                        hasRequested = true
                    )
                    dailyCache[dayKey] = result
                    _state.value = result
                    return@launch
                }

                val weights = presentMonths.mapIndexed { index, mk -> mk to (presentMonths.size - index) }.toMap()
                val weightSum = weights.values.sum()

                val categorias = gastos
                    .filter { periodKeyOf(it.date) in presentMonths }
                    .map { it.category }
                    .distinct()

                val budgetByCat = budgets.associateBy { it.categoryName }

                val forecasts = categorias.map { cat ->
                    val weighted = presentMonths.sumOf { mk ->
                        val monthTotal = gastos
                            .filter { it.category == cat && periodKeyOf(it.date) == mk }
                            .sumOf { it.amount }
                        monthTotal * (weights[mk] ?: 0)
                    }
                    val prediccion = if (weightSum > 0) weighted / weightSum else 0L
                    val limite = budgetByCat[cat]?.limit?.toLong()
                    CategoryForecast(
                        categoria = cat,
                        prediccion = prediccion,
                        presupuesto = limite,
                        excedePresupuesto = limite != null && prediccion > limite,
                        comentario = ""
                    )
                }.filter { it.prediccion > 0 }
                    .sortedByDescending { it.prediccion }

                val comments = fetchComments(gastos, presentMonths, forecasts)
                val withComments = forecasts.map { f ->
                    f.copy(comentario = comments[f.categoria].orEmpty())
                }

                val result = ForecastState(
                    isLoading = false,
                    forecasts = withComments,
                    monthsAvailable = monthsAvailable,
                    hasRequested = true
                )
                dailyCache[dayKey] = result
                _state.value = result
            } catch (e: Exception) {
                _state.value = ForecastState(
                    isLoading = false,
                    error = e.message ?: "No se pudo generar la predicción.",
                    hasRequested = true
                )
            }
        }
    }

    private suspend fun fetchComments(
        gastos: List<Transaction>,
        presentMonths: List<String>,
        forecasts: List<CategoryForecast>
    ): Map<String, String> {
        return try {
            val breakdown = forecasts.joinToString(",\n") { f ->
                val porMes = presentMonths.joinToString(", ") { mk ->
                    val total = gastos.filter { it.category == f.categoria && periodKeyOf(it.date) == mk }.sumOf { it.amount }
                    "\"$mk\":$total"
                }
                "    {\"categoria\":\"${f.categoria}\",\"prediccionProximoMes\":${f.prediccion},\"gastoPorMes\":{$porMes}}"
            }
            val context = "[\n$breakdown\n]"

            val reply = client.chat(
                systemPrompt = FORECAST_SYSTEM_PROMPT,
                history = emptyList(),
                userMessage = "Datos por categoría (últimos meses y predicción del próximo mes):\n$context"
            )

            val cleaned = reply.trim()
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

            val dtos = json.decodeFromString<List<CommentDto>>(cleaned)
            dtos.associate { it.categoria to it.comentario }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun lastThreeMonthKeys(year: Int, month: Int): List<String> {
        val result = mutableListOf<String>()
        var y = year
        var m = month
        repeat(3) {
            result.add("$y-${pad(m)}")
            m -= 1
            if (m == 0) { m = 12; y -= 1 }
        }
        return result
    }

    private fun periodKeyOf(date: String): String? {
        val parts = date.split("/")
        if (parts.size != 3) return null
        val month = parts[1].toIntOrNull() ?: return null
        val year = parts[2].toIntOrNull() ?: return null
        return "$year-${pad(month)}"
    }

    private fun pad(n: Int): String = n.toString().padStart(2, '0')
}

private val FORECAST_SYSTEM_PROMPT = """
Eres un analista financiero de FinTrack (finanzas personales en Costa Rica, moneda colones ₡).
Recibirás datos por categoría con el gasto de los últimos meses y una predicción del próximo mes.
Devuelve ÚNICAMENTE un arreglo JSON válido (sin markdown, sin texto extra) con este formato:
[{"categoria":"<nombre>","comentario":"<observación breve>"}]
Reglas para cada comentario:
- Una sola oración corta en español, cualitativa (ej: "Tu gasto en transporte ha subido un 15% en los últimos 2 meses").
- Menciona la tendencia (subió/bajó/estable) y un porcentaje aproximado cuando tenga sentido.
- No inventes categorías: usa exactamente las que te den.
""".trimIndent()
