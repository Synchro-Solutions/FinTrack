package fintrack.proyecto4.savings.repository

import androidx.compose.runtime.mutableStateListOf
import fintrack.proyecto4.savings.model.GoalCategory
import fintrack.proyecto4.savings.model.GoalColor
import fintrack.proyecto4.savings.model.GoalPriority
import fintrack.proyecto4.savings.model.GoalStatus
import fintrack.proyecto4.savings.model.SavingsContribution
import fintrack.proyecto4.savings.model.SavingsGoal
import fintrack.proyecto4.savings.remote.SavingsFirestoreRepository
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class SavingsRepository(
    private val remoteRepository: SavingsFirestoreRepository =
        SavingsFirestoreRepository()
) {

    companion object {
        private val goals =
            mutableStateListOf<SavingsGoal>()

        private val contributions =
            mutableStateListOf<SavingsContribution>()
    }

    fun getGoals(): List<SavingsGoal> {
        return goals.toList()
    }

    fun getContributionsByGoal(
        goalId: String
    ): List<SavingsContribution> {
        return contributions
            .filter { it.goalId == goalId }
            .reversed()
    }

    suspend fun loadFromFirestore(): Result<Unit> {
        return try {
            val remoteGoals =
                remoteRepository.getGoals()

            goals.clear()
            goals.addAll(remoteGoals)

            contributions.clear()

            remoteGoals.forEach { goal ->
                val goalContributions =
                    remoteRepository.getContributions(goal.id)

                contributions.addAll(goalContributions)
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
        iconName: String,
        category: GoalCategory,
        colorName: GoalColor,
        priority: GoalPriority,
        notes: String
    ): Result<SavingsGoal> {
        if (name.isBlank()) {
            return Result.failure(
                Exception("El nombre no puede estar vacío")
            )
        }

        if (targetAmount <= 0) {
            return Result.failure(
                Exception("El monto debe ser mayor a 0")
            )
        }

        if (
            !deadline.isNullOrBlank() &&
            !isFutureDate(deadline)
        ) {
            return Result.failure(
                Exception("La fecha límite no puede ser anterior a hoy")
            )
        }

        if (
            goals.count {
                it.status == GoalStatus.ACTIVE
            } >= 10
        ) {
            return Result.failure(
                Exception("Máximo 10 metas activas")
            )
        }

        val goal = SavingsGoal(
            id = generateGoalId(),
            name = name.trim(),
            targetAmount = targetAmount,
            currentAmount = 0.0,
            deadline = deadline
                ?.takeIf { it.isNotBlank() },
            iconName = iconName
                .ifBlank { "⭐" },
            status = GoalStatus.ACTIVE,
            category = category,
            colorName = colorName,
            priority = priority,
            notes = notes.trim()
        )

        return try {
            remoteRepository.saveGoal(goal)
            goals.add(0, goal)

            Result.success(goal)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addContribution(
        goalId: String,
        amount: Double
    ): Result<SavingsGoal> {
        if (amount <= 0) {
            return Result.failure(
                Exception(
                    "El monto del abono debe ser mayor a 0"
                )
            )
        }

        val index = goals.indexOfFirst {
            it.id == goalId
        }

        if (index == -1) {
            return Result.failure(
                Exception("Meta no encontrada")
            )
        }

        val goal = goals[index]

        if (
            goal.status ==
            GoalStatus.COMPLETED
        ) {
            return Result.failure(
                Exception(
                    "Esta meta ya está completada"
                )
            )
        }

        if (
            goal.status ==
            GoalStatus.CANCELLED
        ) {
            return Result.failure(
                Exception(
                    "No se puede abonar a una meta cancelada"
                )
            )
        }

        if (amount > goal.remainingAmount) {
            return Result.failure(
                Exception(
                    "El abono no puede superar el monto restante"
                )
            )
        }

        val newAmount =
            goal.currentAmount + amount

        val newStatus =
            if (newAmount >= goal.targetAmount) {
                GoalStatus.COMPLETED
            } else {
                GoalStatus.ACTIVE
            }

        val contribution =
            SavingsContribution(
                id = generateContributionId(),
                goalId = goalId,
                amount = amount,
                createdAt = getCurrentDateText()
            )

        val updatedGoal =
            goal.copy(
                currentAmount = newAmount,
                status = newStatus
            )

        return try {
            remoteRepository
                .saveContribution(contribution)

            remoteRepository
                .saveGoal(updatedGoal)

            contributions.add(contribution)
            goals[index] = updatedGoal

            Result.success(updatedGoal)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelGoal(
        goalId: String
    ): Result<SavingsGoal> {
        val index = goals.indexOfFirst {
            it.id == goalId
        }

        if (index == -1) {
            return Result.failure(
                Exception("Meta no encontrada")
            )
        }

        val goal = goals[index]

        if (
            goal.status !=
            GoalStatus.ACTIVE
        ) {
            return Result.failure(
                Exception(
                    "Solo se pueden cancelar metas activas"
                )
            )
        }

        val updatedGoal =
            goal.copy(
                status = GoalStatus.CANCELLED
            )

        return try {
            remoteRepository.saveGoal(updatedGoal)
            goals[index] = updatedGoal

            Result.success(updatedGoal)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGoal(
        goalId: String,
        name: String,
        deadline: String?,
        iconName: String,
        category: GoalCategory,
        colorName: GoalColor,
        priority: GoalPriority,
        notes: String
    ): Result<SavingsGoal> {
        val index = goals.indexOfFirst {
            it.id == goalId
        }

        if (index == -1) {
            return Result.failure(
                Exception("Meta no encontrada")
            )
        }

        val goal = goals[index]

        if (
            goal.status !=
            GoalStatus.ACTIVE
        ) {
            return Result.failure(
                Exception(
                    "Solo se pueden editar metas activas"
                )
            )
        }

        if (name.isBlank()) {
            return Result.failure(
                Exception(
                    "El nombre no puede estar vacío"
                )
            )
        }

        if (
            !deadline.isNullOrBlank() &&
            !isFutureDate(deadline)
        ) {
            return Result.failure(
                Exception(
                    "La fecha límite no puede ser anterior a hoy"
                )
            )
        }

        val updatedGoal =
            goal.copy(
                name = name.trim(),
                deadline = deadline
                    ?.takeIf { it.isNotBlank() },
                iconName = iconName
                    .ifBlank { "⭐" },
                category = category,
                colorName = colorName,
                priority = priority,
                notes = notes.trim()
            )

        return try {
            remoteRepository.saveGoal(updatedGoal)
            goals[index] = updatedGoal

            Result.success(updatedGoal)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun getCurrentDateText(): String {
        val todayEpochDays =
            (
                    Clock.System.now().epochSeconds /
                            86_400
                    ).toInt()

        val date =
            LocalDate.fromEpochDays(
                todayEpochDays
            )

        val day =
            date.dayOfMonth
                .toString()
                .padStart(2, '0')

        val month =
            date.monthNumber
                .toString()
                .padStart(2, '0')

        val year = date.year

        return "$day/$month/$year"
    }

    @OptIn(ExperimentalTime::class)
    private fun isFutureDate(
        value: String
    ): Boolean {
        return try {
            val todayEpochDays =
                (
                        Clock.System.now().epochSeconds /
                                86_400
                        ).toInt()

            val selectedDate =
                parseDate(value)

            selectedDate.toEpochDays() >=
                    todayEpochDays
        } catch (_: Exception) {
            false
        }
    }

    private fun parseDate(
        value: String
    ): LocalDate {
        val parts = value.split("/")

        if (parts.size != 3) {
            throw IllegalArgumentException(
                "Formato de fecha inválido"
            )
        }

        return LocalDate(
            year = parts[2].toInt(),
            monthNumber = parts[1].toInt(),
            dayOfMonth = parts[0].toInt()
        )
    }

    private fun generateGoalId(): String {
        return buildString {
            append("goal_")
            append(goals.size + 1)
            append("_")
            append(
                kotlin.random.Random.nextInt(
                    1000,
                    9999
                )
            )
        }
    }

    private fun generateContributionId(): String {
        return buildString {
            append("contribution_")
            append(contributions.size + 1)
            append("_")
            append(
                kotlin.random.Random.nextInt(
                    1000,
                    9999
                )
            )
        }
    }
}