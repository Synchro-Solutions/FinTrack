package fintrack.proyecto4.savings.remote

import kotlinx.serialization.Serializable

@Serializable
data class SavingsGoalDto(
    val id: String = "",
    val name: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val deadline: String? = null,
    val iconName: String = "⭐",
    val status: String = "ACTIVE",

    // Información adicional de la meta
    val category: String = "OTHER",
    val colorName: String = "GREEN",
    val priority: String = "MEDIUM",
    val notes: String = "",

    // Plan generado por IA
    val aiMonthlySaving: Double? = null,
    val aiCategoriesToReduce: List<String> = emptyList(),
    val aiExplanation: String = ""
)