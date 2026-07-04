package fintrack.proyecto4.savings.model

data class SavingsContribution(
    val id: String,
    val goalId: String,
    val amount: Double,
    val createdAt: String
)