package fintrack.proyecto4.savings.remote

import kotlinx.serialization.Serializable

@Serializable
data class SavingsContributionDto(
    val id: String = "",
    val goalId: String = "",
    val amount: Double = 0.0,
    val createdAt: String = ""
)