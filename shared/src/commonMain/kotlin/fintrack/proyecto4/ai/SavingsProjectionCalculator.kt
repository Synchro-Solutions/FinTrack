package fintrack.proyecto4.ai

import fintrack.proyecto4.savings.model.GoalStatus
import fintrack.proyecto4.savings.model.SavingsContribution
import fintrack.proyecto4.savings.model.SavingsGoal
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.math.ceil
import kotlin.time.Clock

class SavingsProjectionCalculator {

    fun calculate(
        goal: SavingsGoal,
        contributions: List<SavingsContribution>
    ): SavingsProjection {

        if (
            goal.status == GoalStatus.COMPLETED ||
            goal.remainingAmount <= 0.0
        ) {
            return SavingsProjection(
                averageMonthlySaving = 0.0,
                requiredMonthlySaving = 0.0,
                projectedCompletionDate = null,
                monthsRemaining = 0,
                status = ProjectionStatus.COMPLETED,
                explanation = "Esta meta ya fue completada."
            )
        }

        val today = Clock.System.todayIn(
            TimeZone.currentSystemDefault()
        )

        val validContributions = contributions
            .filter {
                it.goalId == goal.id &&
                        it.amount > 0.0
            }
            .mapNotNull { contribution ->
                parseDate(contribution.createdAt)
                    ?.let { date ->
                        ParsedContribution(
                            amount = contribution.amount,
                            date = date
                        )
                    }
            }
            .sortedBy {
                it.date
            }

        if (validContributions.isEmpty()) {
            return buildNoDataProjection(
                goal = goal
            )
        }

        val firstContributionDate =
            validContributions.first().date

        val elapsedMonths = calculateElapsedMonths(
            startDate = firstContributionDate,
            endDate = today
        ).coerceAtLeast(1)

        val totalContributed = validContributions
            .sumOf {
                it.amount
            }

        val averageMonthlySaving =
            totalContributed / elapsedMonths

        val monthsRemaining =
            calculateMonthsRemaining(goal)

        val requiredMonthlySaving = when {
            goal.remainingAmount <= 0.0 ->
                0.0

            monthsRemaining > 0 ->
                goal.remainingAmount / monthsRemaining

            else ->
                goal.remainingAmount
        }

        if (averageMonthlySaving <= 0.0) {
            return buildNoDataProjection(
                goal = goal
            )
        }

        val estimatedMonthsToComplete = ceil(
            goal.remainingAmount /
                    averageMonthlySaving
        )
            .toInt()
            .coerceAtLeast(1)

        val projectedDate = today.plus(
            DatePeriod(
                months = estimatedMonthsToComplete
            )
        )

        val status = determineStatus(
            goal = goal,
            averageMonthlySaving = averageMonthlySaving,
            requiredMonthlySaving = requiredMonthlySaving,
            projectedDate = projectedDate
        )

        return SavingsProjection(
            averageMonthlySaving = averageMonthlySaving,
            requiredMonthlySaving = requiredMonthlySaving,
            projectedCompletionDate = formatDate(projectedDate),
            monthsRemaining = monthsRemaining,
            status = status,
            explanation = buildLocalExplanation(
                status = status,
                averageMonthlySaving = averageMonthlySaving,
                requiredMonthlySaving = requiredMonthlySaving,
                projectedDate = projectedDate
            )
        )
    }

    private fun determineStatus(
        goal: SavingsGoal,
        averageMonthlySaving: Double,
        requiredMonthlySaving: Double,
        projectedDate: LocalDate
    ): ProjectionStatus {

        val deadline = goal.deadline
            ?.let {
                parseDate(it)
            }

        if (deadline != null) {
            return when {
                projectedDate < deadline ->
                    ProjectionStatus.AHEAD

                projectedDate == deadline ->
                    ProjectionStatus.ON_TRACK

                else ->
                    ProjectionStatus.BEHIND
            }
        }

        return when {
            requiredMonthlySaving <= 0.0 ->
                ProjectionStatus.ON_TRACK

            averageMonthlySaving >
                    requiredMonthlySaving * 1.10 ->
                ProjectionStatus.AHEAD

            averageMonthlySaving >=
                    requiredMonthlySaving * 0.90 ->
                ProjectionStatus.ON_TRACK

            else ->
                ProjectionStatus.BEHIND
        }
    }

    private fun calculateMonthsRemaining(
        goal: SavingsGoal
    ): Int {
        val daysRemaining =
            goal.daysRemaining
                ?: return 0

        if (daysRemaining <= 0) {
            return 0
        }

        return ceil(
            daysRemaining / 30.0
        )
            .toInt()
            .coerceAtLeast(1)
    }

    private fun calculateElapsedMonths(
        startDate: LocalDate,
        endDate: LocalDate
    ): Int {
        val totalMonths =
            (
                    endDate.year -
                            startDate.year
                    ) * 12 +
                    (
                            endDate.monthNumber -
                                    startDate.monthNumber
                            )

        return totalMonths + 1
    }

    private fun buildNoDataProjection(
        goal: SavingsGoal
    ): SavingsProjection {

        val requiredMonthlySaving =
            goal.suggestedMonthlySaving
                ?: goal.aiMonthlySaving
                ?: 0.0

        return SavingsProjection(
            averageMonthlySaving = 0.0,
            requiredMonthlySaving = requiredMonthlySaving,
            projectedCompletionDate = null,
            monthsRemaining = calculateMonthsRemaining(goal),
            status = ProjectionStatus.NO_DATA,
            explanation =
                "Todavía no hay suficientes aportes para calcular tu ritmo de ahorro."
        )
    }

    private fun buildLocalExplanation(
        status: ProjectionStatus,
        averageMonthlySaving: Double,
        requiredMonthlySaving: Double,
        projectedDate: LocalDate
    ): String {

        val difference =
            requiredMonthlySaving -
                    averageMonthlySaving

        return when (status) {
            ProjectionStatus.AHEAD -> {
                "Vas adelantado con tu meta. " +
                        "Al ritmo actual podrías completarla el " +
                        formatDate(projectedDate) +
                        "."
            }

            ProjectionStatus.ON_TRACK -> {
                "Vas por buen camino. " +
                        "Tu ritmo actual de ahorro es suficiente " +
                        "para mantener la meta."
            }

            ProjectionStatus.BEHIND -> {
                "Tu ritmo actual está por debajo de lo necesario. " +
                        "Intenta ahorrar aproximadamente ₡" +
                        difference
                            .coerceAtLeast(0.0)
                            .toLong() +
                        " adicionales por mes."
            }

            ProjectionStatus.COMPLETED -> {
                "Esta meta ya fue completada."
            }

            ProjectionStatus.NO_DATA -> {
                "Todavía no hay suficientes aportes para calcular la proyección."
            }
        }
    }

    private fun parseDate(
        value: String
    ): LocalDate? {
        val cleanValue = value
            .trim()
            .take(10)

        return try {
            if (cleanValue.contains("/")) {
                val parts =
                    cleanValue.split("/")

                if (parts.size != 3) {
                    return null
                }

                LocalDate(
                    year = parts[2].toInt(),
                    monthNumber = parts[1].toInt(),
                    dayOfMonth = parts[0].toInt()
                )
            } else {
                LocalDate.parse(cleanValue)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun formatDate(
        date: LocalDate
    ): String {
        val day =
            date.dayOfMonth
                .toString()
                .padStart(2, '0')

        val month =
            date.monthNumber
                .toString()
                .padStart(2, '0')

        return "$day/$month/${date.year}"
    }

    private data class ParsedContribution(
        val amount: Double,
        val date: LocalDate
    )
}