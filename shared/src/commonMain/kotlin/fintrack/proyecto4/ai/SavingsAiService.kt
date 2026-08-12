package fintrack.proyecto4.ai

import kotlinx.serialization.json.Json

class SavingsAiService(
    private val groqClient: GroqClient = GroqClient()
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    suspend fun generateSavingsPlan(
        goalName: String,
        targetAmount: Double,
        deadline: String?,
        requiredMonthlySaving: Double,
        totalIncome: Long,
        totalExpenses: Long,
        expensesByCategory: Map<String, Long>
    ): Result<SavingsPlan> {
        return runCatching {
            validateInput(
                goalName = goalName,
                targetAmount = targetAmount,
                requiredMonthlySaving = requiredMonthlySaving
            )

            val prompt = SavingsPrompt.buildPlanPrompt(
                goalName = goalName.trim(),
                targetAmount = targetAmount,
                deadline = deadline,
                requiredMonthlySaving = requiredMonthlySaving,
                totalIncome = totalIncome,
                totalExpenses = totalExpenses,
                expensesByCategory = expensesByCategory
            )

            val rawResponse = groqClient.chat(
                systemPrompt = SavingsPrompt.SYSTEM_PROMPT,
                history = emptyList(),
                userMessage = prompt
            )

            val cleanJson = extractJsonObject(rawResponse)

            val plan = json.decodeFromString<SavingsPlan>(cleanJson)

            validatePlan(
                plan = plan,
                requiredMonthlySaving = requiredMonthlySaving
            )
        }
    }

    private fun validateInput(
        goalName: String,
        targetAmount: Double,
        requiredMonthlySaving: Double
    ) {
        require(goalName.isNotBlank()) {
            "El nombre de la meta es obligatorio."
        }

        require(targetAmount > 0.0) {
            "El monto objetivo debe ser mayor que cero."
        }

        require(requiredMonthlySaving >= 0.0) {
            "El ahorro mensual calculado no puede ser negativo."
        }
    }

    private fun validatePlan(
        plan: SavingsPlan,
        requiredMonthlySaving: Double
    ): SavingsPlan {
        require(plan.explanation.isNotBlank()) {
            "La IA no generó una explicación válida."
        }

        val normalizedMonthlySaving = maxOf(
            plan.monthlySaving,
            requiredMonthlySaving
        )

        require(normalizedMonthlySaving > 0.0) {
            "La IA no generó un monto de ahorro válido."
        }

        val normalizedCategories = plan.categoriesToReduce
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(3)

        return plan.copy(
            monthlySaving = normalizedMonthlySaving,
            categoriesToReduce = normalizedCategories,
            explanation = plan.explanation.trim()
        )
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

        require(
            firstBrace >= 0 &&
                    lastBrace > firstBrace
        ) {
            "La respuesta de la IA no contiene un JSON válido."
        }

        return cleanedResponse.substring(
            startIndex = firstBrace,
            endIndex = lastBrace + 1
        )
    }
}