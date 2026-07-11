package fintrack.proyecto4.savings.repository

import androidx.compose.runtime.mutableStateListOf
import fintrack.proyecto4.savings.model.GoalStatus
import fintrack.proyecto4.savings.model.SavingsContribution
import fintrack.proyecto4.savings.model.SavingsGoal
import fintrack.proyecto4.savings.remote.SavingsFirestoreRepository
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class SavingsRepository(
    private val remoteRepository: SavingsFirestoreRepository = SavingsFirestoreRepository()
) {

    companion object {
        private val goals = mutableStateListOf<SavingsGoal>()
        private val contributions = mutableStateListOf<SavingsContribution>()
    }

    fun getGoals(): List<SavingsGoal> = goals.toList()

    fun getContributionsByGoal(goalId: String): List<SavingsContribution> {
        return contributions.filter { it.goalId == goalId }.reversed()
    }

    suspend fun loadFromFirestore(): Result<Unit> {
        return try {
            val remoteGoals = remoteRepository.getGoals()

            goals.clear()
            goals.addAll(remoteGoals)

            contributions.clear()
            remoteGoals.forEach { goal ->
                contributions.addAll(remoteRepository.getContributions(goal.id))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createGoal(
        name: String,
        targetAmount: Double,
        deadline: String?,
        iconName: String
    ): Result<SavingsGoal> {
        if (name.isBlank()) return Result.failure(Exception("El nombre no puede estar vacío"))
        if (targetAmount <= 0) return Result.failure(Exception("El monto debe ser mayor a 0"))

        if (!deadline.isNullOrBlank() && !isFutureDate(deadline)) {
            return Result.failure(Exception("La fecha límite debe ser futura"))
        }

        if (goals.count { it.status == GoalStatus.ACTIVE } >= 10) {
            return Result.failure(Exception("Máximo 10 metas activas"))
        }

        val goal = SavingsGoal(
            id = generateGoalId(),
            name = name.trim(),
            targetAmount = targetAmount,
            currentAmount = 0.0,
            deadline = deadline?.takeIf { it.isNotBlank() },
            iconName = iconName.ifBlank { "⭐" },
            status = GoalStatus.ACTIVE
        )

        goals.add(0, goal)
        remoteRepository.saveGoal(goal)

        return Result.success(goal)
    }

    suspend fun addContribution(goalId: String, amount: Double): Result<SavingsGoal> {
        if (amount <= 0) return Result.failure(Exception("El monto del abono debe ser mayor a 0"))

        val index = goals.indexOfFirst { it.id == goalId }
        if (index == -1) return Result.failure(Exception("Meta no encontrada"))

        val goal = goals[index]

        if (goal.status == GoalStatus.COMPLETED) {
            return Result.failure(Exception("Esta meta ya está completada"))
        }

        if (goal.status == GoalStatus.CANCELLED) {
            return Result.failure(Exception("No se puede abonar a una meta cancelada"))
        }

        if (amount > goal.remainingAmount) {
            return Result.failure(Exception("El abono no puede superar el monto restante"))
        }

        val newAmount = goal.currentAmount + amount
        val newStatus = if (newAmount >= goal.targetAmount) GoalStatus.COMPLETED else GoalStatus.ACTIVE

        val contribution = SavingsContribution(
            id = generateContributionId(),
            goalId = goalId,
            amount = amount,
            createdAt = getCurrentDateText()
        )

        val updatedGoal = goal.copy(
            currentAmount = newAmount,
            status = newStatus
        )

        contributions.add(contribution)
        goals[index] = updatedGoal

        remoteRepository.saveContribution(contribution)
        remoteRepository.saveGoal(updatedGoal)

        return Result.success(updatedGoal)
    }

    suspend fun cancelGoal(goalId: String): Result<SavingsGoal> {
        val index = goals.indexOfFirst { it.id == goalId }
        if (index == -1) return Result.failure(Exception("Meta no encontrada"))

        val goal = goals[index]

        if (goal.status != GoalStatus.ACTIVE) {
            return Result.failure(Exception("Solo se pueden cancelar metas activas"))
        }

        val updatedGoal = goal.copy(status = GoalStatus.CANCELLED)

        goals[index] = updatedGoal
        remoteRepository.saveGoal(updatedGoal)

        return Result.success(updatedGoal)
    }

    suspend fun updateGoal(
        goalId: String,
        name: String,
        deadline: String?,
        iconName: String
    ): Result<SavingsGoal> {
        val index = goals.indexOfFirst { it.id == goalId }
        if (index == -1) return Result.failure(Exception("Meta no encontrada"))

        val goal = goals[index]

        if (goal.status != GoalStatus.ACTIVE) {
            return Result.failure(Exception("Solo se pueden editar metas activas"))
        }

        if (name.isBlank()) {
            return Result.failure(Exception("El nombre no puede estar vacío"))
        }

        if (!deadline.isNullOrBlank() && !isFutureDate(deadline)) {
            return Result.failure(Exception("La fecha límite debe ser futura"))
        }

        val updatedGoal = goal.copy(
            name = name.trim(),
            deadline = deadline?.takeIf { it.isNotBlank() },
            iconName = iconName.ifBlank { "⭐" }
        )

        goals[index] = updatedGoal
        remoteRepository.saveGoal(updatedGoal)

        return Result.success(updatedGoal)
    }

    @OptIn(ExperimentalTime::class)
    private fun getCurrentDateText(): String {
        val todayEpochDays = (Clock.System.now().epochSeconds / 86_400).toInt()
        val date = LocalDate.fromEpochDays(todayEpochDays)

        val day = date.dayOfMonth.toString().padStart(2, '0')
        val month = date.monthNumber.toString().padStart(2, '0')
        val year = date.year

        return "$day/$month/$year"
    }

    @OptIn(ExperimentalTime::class)
    private fun isFutureDate(value: String): Boolean {
        return try {
            val todayEpochDays = (Clock.System.now().epochSeconds / 86_400).toInt()
            val selectedDate = parseDate(value)
            selectedDate.toEpochDays() > todayEpochDays
        } catch (_: Exception) {
            false
        }
    }

    private fun parseDate(value: String): LocalDate {
        val parts = value.split("/")

        return LocalDate(
            year = parts[2].toInt(),
            monthNumber = parts[1].toInt(),
            dayOfMonth = parts[0].toInt()
        )
    }

    private fun generateGoalId(): String {
        return "goal_${goals.size + 1}_${kotlin.random.Random.nextInt(1000, 9999)}"
    }

    private fun generateContributionId(): String {
        return "contribution_${contributions.size + 1}_${kotlin.random.Random.nextInt(1000, 9999)}"
    }
}