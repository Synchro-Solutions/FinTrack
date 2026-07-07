package fintrack.proyecto4.savings.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class SavingsGoal(
    val id: String,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val deadline: String? = null,
    val iconName: String = "⭐",
    val status: GoalStatus = GoalStatus.ACTIVE
) {
    val progress: Float
        get() = if (targetAmount <= 0) 0f
        else (currentAmount / targetAmount).coerceIn(0.0, 1.0).toFloat()

    val progressPercentage: Int
        get() = (progress * 100).toInt()

    val remainingAmount: Double
        get() = (targetAmount - currentAmount).coerceAtLeast(0.0)

    val daysRemaining: Int?
        @OptIn(ExperimentalTime::class)
        get() {
            val dateText = deadline ?: return null

            return try {
                val todayEpochDays = (Clock.System.now().epochSeconds / 86_400).toInt()
                val today = LocalDate.fromEpochDays(todayEpochDays)
                val deadlineDate = parseDate(dateText)

                today.daysUntil(deadlineDate)
            } catch (_: Exception) {
                null
            }
        }

    val deadlineLabel: String
        get() {
            val days = daysRemaining ?: return "Sin fecha definida"

            return when {
                days > 1 -> "Quedan $days días"
                days == 1 -> "Queda 1 día"
                days == 0 -> "Vence hoy"
                days == -1 -> "Vencida hace 1 día"
                else -> "Vencida hace ${-days} días"
            }
        }

    private fun parseDate(value: String): LocalDate {
        val parts = value.split("/")

        return if (parts.size == 3) {
            LocalDate(
                year = parts[2].toInt(),
                monthNumber = parts[1].toInt(),
                dayOfMonth = parts[0].toInt()
            )
        } else {
            LocalDate.parse(value)
        }
    }
}

enum class GoalStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED
}