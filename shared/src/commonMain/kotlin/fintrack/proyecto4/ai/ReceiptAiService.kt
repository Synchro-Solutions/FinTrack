package fintrack.proyecto4.ai

import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Envía el texto crudo de ML Kit a Groq para corregir errores de OCR (monto, fecha,
 * comercio) y sugerir la categoría. Mismo patrón que SavingsAiService: prompt -> chat ->
 * limpieza de JSON -> decode tolerante.
 */
class ReceiptAiService(
    private val groqClient: GroqClient = GroqClient()
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    suspend fun enhance(rawText: String, categories: List<String>): Result<ReceiptAiResult> {
        return runCatching {
            require(rawText.isNotBlank()) { "No hay texto para analizar." }

            val prompt = ReceiptPrompt.buildPrompt(
                rawText = rawText,
                categories = categories,
                todayDate = todayAsDateText()
            )

            val rawResponse = groqClient.chat(
                systemPrompt = ReceiptPrompt.SYSTEM_PROMPT,
                history = emptyList(),
                userMessage = prompt
            )

            val cleanJson = extractJsonObject(rawResponse)
            json.decodeFromString<ReceiptAiResult>(cleanJson)
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun todayAsDateText(): String {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val day = today.day.toString().padStart(2, '0')
        val month = today.month.number.toString().padStart(2, '0')
        return "$day/$month/${today.year}"
    }

    private fun extractJsonObject(response: String): String {
        val cleanedResponse = response
            .trim()
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val firstBrace = cleanedResponse.indexOf('{')
        val lastBrace = cleanedResponse.lastIndexOf('}')

        require(firstBrace >= 0 && lastBrace > firstBrace) {
            "La respuesta de la IA no contiene un JSON válido."
        }

        return cleanedResponse.substring(startIndex = firstBrace, endIndex = lastBrace + 1)
    }
}
