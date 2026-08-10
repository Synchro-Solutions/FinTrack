package fintrack.proyecto4.ai

import fintrack.proyecto4.savings.model.SavingsContribution
import fintrack.proyecto4.savings.model.SavingsGoal

class SavingsProjectionService(
    private val groqClient: GroqClient = GroqClient(),
    private val calculator: SavingsProjectionCalculator =
        SavingsProjectionCalculator()
) {

    suspend fun generateProjection(
        goal: SavingsGoal,
        contributions: List<SavingsContribution>
    ): Result<SavingsProjection> {
        return runCatching {
            val localProjection = calculator.calculate(
                goal = goal,
                contributions = contributions
            )

            if (
                localProjection.status == ProjectionStatus.COMPLETED ||
                localProjection.status == ProjectionStatus.NO_DATA
            ) {
                return@runCatching localProjection
            }

            val prompt = SavingsProjectionPrompt.buildPrompt(
                goalName = goal.name,
                remainingAmount = goal.remainingAmount,
                projection = localProjection
            )

            val rawResponse = groqClient.chat(
                systemPrompt = SavingsProjectionPrompt.SYSTEM_PROMPT,
                history = emptyList(),
                userMessage = prompt
            )

            val explanation = normalizeExplanation(
                response = rawResponse
            )

            localProjection.copy(
                explanation = explanation
            )
        }
    }

    fun calculateLocalProjection(
        goal: SavingsGoal,
        contributions: List<SavingsContribution>
    ): SavingsProjection {
        return calculator.calculate(
            goal = goal,
            contributions = contributions
        )
    }

    private fun normalizeExplanation(
        response: String
    ): String {
        val explanation = response
            .trim()
            .removePrefix("\"")
            .removeSuffix("\"")
            .removePrefix("```")
            .removeSuffix("```")
            .replace("\n", " ")
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()

        require(explanation.isNotBlank()) {
            "La IA no generó una explicación válida."
        }

        return explanation.take(500)
    }
}