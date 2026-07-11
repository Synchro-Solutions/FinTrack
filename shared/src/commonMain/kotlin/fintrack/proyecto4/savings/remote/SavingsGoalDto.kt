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
    val status: String = "ACTIVE"
)