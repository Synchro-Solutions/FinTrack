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
    val status: GoalStatus = GoalStatus.ACTIVE,

    // Nuevos campos
    val category: GoalCategory = GoalCategory.OTHER,
    val colorName: GoalColor = GoalColor.GREEN,
    val priority: GoalPriority = GoalPriority.MEDIUM,
    val notes: String = ""
) {
    val progress: Float
        get() {
            if (targetAmount <= 0) return 0f

            return (currentAmount / targetAmount)
                .coerceIn(0.0, 1.0)
                .toFloat()
        }

    val progressPercentage: Int
        get() = (progress * 100).toInt()

    val remainingAmount: Double
        get() = (targetAmount - currentAmount)
            .coerceAtLeast(0.0)

    val isOverdue: Boolean
        get() = (daysRemaining ?: 0) < 0 &&
                status == GoalStatus.ACTIVE

    val daysRemaining: Int?
        @OptIn(ExperimentalTime::class)
        get() {
            val dateText = deadline ?: return null

            return try {
                val todayEpochDays =
                    (Clock.System.now().epochSeconds / 86_400).toInt()

                val today = LocalDate.fromEpochDays(todayEpochDays)
                val deadlineDate = parseDate(dateText)

                today.daysUntil(deadlineDate)
            } catch (_: Exception) {
                null
            }
        }

    val deadlineLabel: String
        get() {
            val days = daysRemaining
                ?: return "Sin fecha definida"

            return when {
                days > 1 -> "Quedan $days días"
                days == 1 -> "Queda 1 día"
                days == 0 -> "Vence hoy"
                days == -1 -> "Vencida hace 1 día"
                else -> "Vencida hace ${-days} días"
            }
        }

    val categoryLabel: String
        get() = when (category) {
            GoalCategory.HOME -> "Hogar"
            GoalCategory.VEHICLE -> "Vehículo"
            GoalCategory.TRAVEL -> "Viaje"
            GoalCategory.EDUCATION -> "Educación"
            GoalCategory.TECHNOLOGY -> "Tecnología"
            GoalCategory.EMERGENCY -> "Emergencia"
            GoalCategory.HEALTH -> "Salud"
            GoalCategory.OTHER -> "Otro"
        }

    val priorityLabel: String
        get() = when (priority) {
            GoalPriority.LOW -> "Baja"
            GoalPriority.MEDIUM -> "Media"
            GoalPriority.HIGH -> "Alta"
        }

    val suggestedMonthlySaving: Double?
        get() {
            val days = daysRemaining ?: return null

            if (days <= 0 || remainingAmount <= 0) {
                return null
            }

            val estimatedMonths = (days / 30.0)
                .coerceAtLeast(1.0)

            return remainingAmount / estimatedMonths
        }

    val suggestedWeeklySaving: Double?
        get() {
            val days = daysRemaining ?: return null

            if (days <= 0 || remainingAmount <= 0) {
                return null
            }

            val estimatedWeeks = (days / 7.0)
                .coerceAtLeast(1.0)

            return remainingAmount / estimatedWeeks
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

enum class GoalCategory {
    HOME,
    VEHICLE,
    TRAVEL,
    EDUCATION,
    TECHNOLOGY,
    EMERGENCY,
    HEALTH,
    OTHER
}

enum class GoalPriority {
    LOW,
    MEDIUM,
    HIGH
}

enum class GoalColor {
    GREEN,
    BLUE,
    ORANGE,
    PURPLE,
    RED
}