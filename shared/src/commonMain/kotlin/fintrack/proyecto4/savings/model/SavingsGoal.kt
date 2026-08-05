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

    // Información general de la meta
    val category: GoalCategory = GoalCategory.OTHER,
    val colorName: GoalColor = GoalColor.GREEN,
    val priority: GoalPriority = GoalPriority.MEDIUM,
    val notes: String = "",

    // Plan de ahorro generado por IA
    val aiMonthlySaving: Double? = null,
    val aiCategoriesToReduce: List<String> = emptyList(),
    val aiExplanation: String = ""
) {

    /**
     * Progreso de la meta en un rango entre 0 y 1.
     */
    val progress: Float
        get() {
            if (targetAmount <= 0.0) {
                return 0f
            }

            return (currentAmount / targetAmount)
                .coerceIn(0.0, 1.0)
                .toFloat()
        }

    /**
     * Progreso representado como porcentaje.
     */
    val progressPercentage: Int
        get() = (progress * 100).toInt()

    /**
     * Monto pendiente para completar la meta.
     */
    val remainingAmount: Double
        get() = (targetAmount - currentAmount)
            .coerceAtLeast(0.0)

    /**
     * Indica si la meta está vencida.
     */
    val isOverdue: Boolean
        get() = (daysRemaining ?: 0) < 0 &&
                status == GoalStatus.ACTIVE

    /**
     * Indica si esta meta tiene un plan de ahorro
     * válido generado por inteligencia artificial.
     */
    val hasAiPlan: Boolean
        get() = aiMonthlySaving != null &&
                aiMonthlySaving > 0.0 &&
                aiExplanation.isNotBlank()

    /**
     * Cantidad de días restantes para alcanzar la fecha límite.
     */
    val daysRemaining: Int?
        @OptIn(ExperimentalTime::class)
        get() {
            val dateText = deadline ?: return null

            return try {
                val todayEpochDays =
                    (
                            Clock.System.now().epochSeconds /
                                    86_400
                            ).toInt()

                val today =
                    LocalDate.fromEpochDays(todayEpochDays)

                val deadlineDate =
                    parseDate(dateText)

                today.daysUntil(deadlineDate)
            } catch (_: Exception) {
                null
            }
        }

    /**
     * Texto amigable para mostrar la fecha restante.
     */
    val deadlineLabel: String
        get() {
            val days =
                daysRemaining
                    ?: return "Sin fecha definida"

            return when {
                days > 1 ->
                    "Quedan $days días"

                days == 1 ->
                    "Queda 1 día"

                days == 0 ->
                    "Vence hoy"

                days == -1 ->
                    "Vencida hace 1 día"

                else ->
                    "Vencida hace ${-days} días"
            }
        }

    /**
     * Nombre visible de la categoría.
     */
    val categoryLabel: String
        get() = when (category) {
            GoalCategory.HOME ->
                "Hogar"

            GoalCategory.VEHICLE ->
                "Vehículo"

            GoalCategory.TRAVEL ->
                "Viaje"

            GoalCategory.EDUCATION ->
                "Educación"

            GoalCategory.TECHNOLOGY ->
                "Tecnología"

            GoalCategory.EMERGENCY ->
                "Emergencia"

            GoalCategory.HEALTH ->
                "Salud"

            GoalCategory.OTHER ->
                "Otro"
        }

    /**
     * Nombre visible de la prioridad.
     */
    val priorityLabel: String
        get() = when (priority) {
            GoalPriority.LOW ->
                "Baja"

            GoalPriority.MEDIUM ->
                "Media"

            GoalPriority.HIGH ->
                "Alta"
        }

    /**
     * Ahorro mensual calculado localmente según
     * el monto restante y la fecha límite.
     *
     * Este cálculo no depende de la IA.
     */
    val suggestedMonthlySaving: Double?
        get() {
            val days =
                daysRemaining
                    ?: return null

            if (
                days <= 0 ||
                remainingAmount <= 0.0
            ) {
                return null
            }

            val estimatedMonths =
                (days / 30.0)
                    .coerceAtLeast(1.0)

            return remainingAmount /
                    estimatedMonths
        }

    /**
     * Ahorro semanal calculado localmente.
     */
    val suggestedWeeklySaving: Double?
        get() {
            val days =
                daysRemaining
                    ?: return null

            if (
                days <= 0 ||
                remainingAmount <= 0.0
            ) {
                return null
            }

            val estimatedWeeks =
                (days / 7.0)
                    .coerceAtLeast(1.0)

            return remainingAmount /
                    estimatedWeeks
        }

    /**
     * Convierte la fecha utilizada por la aplicación
     * en un objeto LocalDate.
     *
     * Admite:
     *
     * dd/MM/yyyy
     * yyyy-MM-dd
     */
    private fun parseDate(
        value: String
    ): LocalDate {
        val parts =
            value.split("/")

        return if (parts.size == 3) {
            LocalDate(
                year = parts[2].toInt(),
                month = parts[1].toInt(),
                day = parts[0].toInt()
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