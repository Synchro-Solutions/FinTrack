package fintrack.proyecto4.ai

class FinancialAdviceService(
    private val groqClient: GroqClient = GroqClient()
) {

    suspend fun generateAdvice(
        totalIncome: Long,
        totalExpenses: Long,
        expensesByCategory: Map<String, Long>
    ): Result<String> {
        return runCatching {
            val prompt = buildPrompt(
                totalIncome = totalIncome,
                totalExpenses = totalExpenses,
                expensesByCategory = expensesByCategory
            )

            val response = groqClient.chat(
                systemPrompt = SYSTEM_PROMPT,
                history = emptyList(),
                userMessage = prompt
            )

            normalizeAdvice(response)
        }
    }

    private fun buildPrompt(
        totalIncome: Long,
        totalExpenses: Long,
        expensesByCategory: Map<String, Long>
    ): String {
        val balance = totalIncome - totalExpenses

        val categoriesText = expensesByCategory
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .joinToString("\n") {
                "- ${it.key}: ₡${it.value}"
            }
            .ifBlank {
                "No hay gastos clasificados."
            }

        return """
            Analiza la información financiera del usuario y genera un consejo útil.

            Ingresos totales: ₡$totalIncome
            Gastos totales: ₡$totalExpenses
            Balance: ₡$balance

            Principales gastos:
            $categoriesText

            Genera un consejo diferente, claro y práctico.
            Máximo 2 oraciones.
        """.trimIndent()
    }

    private fun normalizeAdvice(
        response: String
    ): String {
        val advice = response
            .trim()
            .removePrefix("\"")
            .removeSuffix("\"")
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        require(advice.isNotBlank()) {
            "La IA no generó un consejo financiero."
        }

        return advice.take(300)
    }

    companion object {
        private const val SYSTEM_PROMPT = """
            Eres un asistente financiero para una aplicación de finanzas personales.

            Genera consejos breves, claros y responsables basados solamente en los datos proporcionados.

            Reglas:
            - Responde únicamente con el consejo.
            - Máximo 2 oraciones.
            - No uses formato JSON.
            - No uses listas.
            - No recomiendes préstamos.
            - No inventes datos.
            - No prometas resultados financieros.
            - Responde en español.
        """
    }
}