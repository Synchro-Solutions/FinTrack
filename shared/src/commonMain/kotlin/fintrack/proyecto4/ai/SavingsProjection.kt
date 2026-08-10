package fintrack.proyecto4.ai

import kotlinx.serialization.Serializable

@Serializable
data class SavingsProjection(
    val averageMonthlySaving: Double = 0.0,
    val requiredMonthlySaving: Double = 0.0,
    val projectedCompletionDate: String? = null,
    val monthsRemaining: Int = 0,
    val status: ProjectionStatus = ProjectionStatus.NO_DATA,
    val explanation: String = ""
) {
    val isValid: Boolean
        get() = explanation.isNotBlank()
}

@Serializable
enum class ProjectionStatus {
    ON_TRACK,
    AHEAD,
    BEHIND,
    COMPLETED,
    NO_DATA
}