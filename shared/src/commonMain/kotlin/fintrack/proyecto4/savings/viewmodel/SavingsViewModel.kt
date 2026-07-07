package fintrack.proyecto4.savings.viewmodel

import androidx.compose.runtime.*
import fintrack.proyecto4.savings.model.GoalStatus
import fintrack.proyecto4.savings.model.SavingsContribution
import fintrack.proyecto4.savings.model.SavingsGoal
import fintrack.proyecto4.savings.repository.SavingsRepository

class SavingsViewModel(
    private val repository: SavingsRepository = SavingsRepository()
) {
    var goals by mutableStateOf(repository.getGoals())
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    val activeGoals: List<SavingsGoal>
        get() = goals.filter { it.status == GoalStatus.ACTIVE }

    val completedGoals: List<SavingsGoal>
        get() = goals.filter { it.status == GoalStatus.COMPLETED }

    suspend fun loadGoals() {
        isLoading = true

        val result = repository.loadFromFirestore()

        if (result.isSuccess) {
            refreshGoals()
            errorMessage = null
        } else {
            errorMessage = result.exceptionOrNull()?.message ?: "Error cargando metas"
        }

        isLoading = false
    }

    fun getContributions(goalId: String): List<SavingsContribution> {
        return repository.getContributionsByGoal(goalId)
    }

    suspend fun createGoal(
        name: String,
        targetAmountText: String,
        deadline: String?,
        iconName: String
    ): Boolean {
        val amount = targetAmountText
            .replace("₡", "")
            .replace(",", "")
            .replace(" ", "")
            .toDoubleOrNull()

        if (amount == null) {
            errorMessage = "Ingrese un monto válido"
            return false
        }

        val result = repository.createGoal(
            name = name,
            targetAmount = amount,
            deadline = deadline,
            iconName = iconName
        )

        return handleResult(result)
    }

    suspend fun addContribution(goalId: String, amountText: String): Boolean {
        val amount = amountText
            .replace("₡", "")
            .replace(",", "")
            .replace(" ", "")
            .toDoubleOrNull()

        if (amount == null) {
            errorMessage = "Ingrese un monto válido"
            return false
        }

        val result = repository.addContribution(goalId, amount)
        return handleResult(result)
    }

    suspend fun cancelGoal(goalId: String): Boolean {
        val result = repository.cancelGoal(goalId)
        return handleResult(result)
    }

    suspend fun updateGoal(
        goalId: String,
        name: String,
        deadline: String?,
        iconName: String
    ): Boolean {
        val result = repository.updateGoal(
            goalId = goalId,
            name = name,
            deadline = deadline,
            iconName = iconName
        )

        return handleResult(result)
    }

    fun clearError() {
        errorMessage = null
    }

    private fun handleResult(result: Result<SavingsGoal>): Boolean {
        return if (result.isSuccess) {
            refreshGoals()
            errorMessage = null
            true
        } else {
            errorMessage = result.exceptionOrNull()?.message
            false
        }
    }

    private fun refreshGoals() {
        goals = repository.getGoals().toList()
    }
}