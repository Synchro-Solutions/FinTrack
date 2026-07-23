package fintrack.proyecto4.savings.viewmodel

import androidx.compose.runtime.*
import fintrack.proyecto4.savings.model.GoalCategory
import fintrack.proyecto4.savings.model.GoalColor
import fintrack.proyecto4.savings.model.GoalPriority
import fintrack.proyecto4.savings.model.GoalStatus
import fintrack.proyecto4.savings.model.SavingsContribution
import fintrack.proyecto4.savings.model.SavingsGoal
import fintrack.proyecto4.savings.repository.SavingsRepository

enum class GoalFilter {
    ALL,
    ACTIVE,
    COMPLETED,
    CANCELLED
}

enum class GoalSort {
    MOST_RECENT,
    HIGHEST_PROGRESS,
    LOWEST_PROGRESS,
    DEADLINE,
    NAME
}

class SavingsViewModel(
    private val repository: SavingsRepository = SavingsRepository()
) {
    var goals by mutableStateOf(repository.getGoals())
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var selectedFilter by mutableStateOf(GoalFilter.ALL)
        private set

    var selectedSort by mutableStateOf(GoalSort.MOST_RECENT)
        private set

    val activeGoals: List<SavingsGoal>
        get() = goals.filter {
            it.status == GoalStatus.ACTIVE
        }

    val completedGoals: List<SavingsGoal>
        get() = goals.filter {
            it.status == GoalStatus.COMPLETED
        }

    val cancelledGoals: List<SavingsGoal>
        get() = goals.filter {
            it.status == GoalStatus.CANCELLED
        }

    val totalSaved: Double
        get() = goals
            .filter {
                it.status != GoalStatus.CANCELLED
            }
            .sumOf {
                it.currentAmount
            }

    val totalTarget: Double
        get() = goals
            .filter {
                it.status != GoalStatus.CANCELLED
            }
            .sumOf {
                it.targetAmount
            }

    val totalRemaining: Double
        get() = goals
            .filter {
                it.status == GoalStatus.ACTIVE
            }
            .sumOf {
                it.remainingAmount
            }

    val averageProgress: Int
        get() {
            val validGoals = goals.filter {
                it.status != GoalStatus.CANCELLED
            }

            if (validGoals.isEmpty()) {
                return 0
            }

            return validGoals
                .map {
                    it.progressPercentage
                }
                .average()
                .toInt()
        }

    val highPriorityGoals: List<SavingsGoal>
        get() = activeGoals.filter {
            it.priority == GoalPriority.HIGH
        }

    val overdueGoals: List<SavingsGoal>
        get() = activeGoals.filter {
            it.isOverdue
        }

    val mainGoal: SavingsGoal?
        get() {
            return activeGoals
                .sortedWith(
                    compareByDescending<SavingsGoal> {
                        priorityValue(it.priority)
                    }.thenByDescending {
                        it.progressPercentage
                    }
                )
                .firstOrNull()
        }

    val visibleGoals: List<SavingsGoal>
        get() {
            val filteredGoals = when (selectedFilter) {
                GoalFilter.ALL -> goals

                GoalFilter.ACTIVE -> {
                    goals.filter {
                        it.status == GoalStatus.ACTIVE
                    }
                }

                GoalFilter.COMPLETED -> {
                    goals.filter {
                        it.status == GoalStatus.COMPLETED
                    }
                }

                GoalFilter.CANCELLED -> {
                    goals.filter {
                        it.status == GoalStatus.CANCELLED
                    }
                }
            }

            return when (selectedSort) {
                GoalSort.MOST_RECENT -> {
                    filteredGoals
                }

                GoalSort.HIGHEST_PROGRESS -> {
                    filteredGoals.sortedByDescending {
                        it.progressPercentage
                    }
                }

                GoalSort.LOWEST_PROGRESS -> {
                    filteredGoals.sortedBy {
                        it.progressPercentage
                    }
                }

                GoalSort.DEADLINE -> {
                    filteredGoals.sortedWith(
                        compareBy<SavingsGoal> {
                            it.deadline == null
                        }.thenBy {
                            deadlineToSortableValue(
                                it.deadline
                            )
                        }
                    )
                }

                GoalSort.NAME -> {
                    filteredGoals.sortedBy {
                        it.name.lowercase()
                    }
                }
            }
        }

    suspend fun loadGoals() {
        isLoading = true

        val result = repository.loadFromFirestore()

        if (result.isSuccess) {
            refreshGoals()
            errorMessage = null
        } else {
            errorMessage =
                result.exceptionOrNull()?.message
                    ?: "Error cargando metas"
        }

        isLoading = false
    }

    fun selectFilter(
        filter: GoalFilter
    ) {
        selectedFilter = filter
    }

    fun selectSort(
        sort: GoalSort
    ) {
        selectedSort = sort
    }

    fun getContributions(
        goalId: String
    ): List<SavingsContribution> {
        return repository.getContributionsByGoal(
            goalId
        )
    }

    suspend fun createGoal(
        name: String,
        targetAmountText: String,
        deadline: String?,
        iconName: String,
        category: GoalCategory,
        colorName: GoalColor,
        priority: GoalPriority,
        notes: String
    ): Boolean {
        val amount = parseAmount(
            targetAmountText
        )

        if (amount == null) {
            errorMessage =
                "Ingrese un monto válido"

            return false
        }

        val result = repository.createGoal(
            name = name,
            targetAmount = amount,
            deadline = deadline,
            iconName = iconName,
            category = category,
            colorName = colorName,
            priority = priority,
            notes = notes
        )

        return handleResult(result)
    }

    suspend fun addContribution(
        goalId: String,
        amountText: String
    ): Boolean {
        val amount = parseAmount(
            amountText
        )

        if (amount == null) {
            errorMessage =
                "Ingrese un monto válido"

            return false
        }

        val result = repository.addContribution(
            goalId = goalId,
            amount = amount
        )

        return handleResult(result)
    }

    suspend fun cancelGoal(
        goalId: String
    ): Boolean {
        val result = repository.cancelGoal(
            goalId
        )

        return handleResult(result)
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
    ): Boolean {
        val result = repository.updateGoal(
            goalId = goalId,
            name = name,
            deadline = deadline,
            iconName = iconName,
            category = category,
            colorName = colorName,
            priority = priority,
            notes = notes
        )

        return handleResult(result)
    }

    fun clearError() {
        errorMessage = null
    }

    private fun parseAmount(
        amountText: String
    ): Double? {
        return amountText
            .replace("₡", "")
            .replace(",", "")
            .replace(".", "")
            .replace(" ", "")
            .toDoubleOrNull()
    }

    private fun handleResult(
        result: Result<SavingsGoal>
    ): Boolean {
        return if (result.isSuccess) {
            refreshGoals()
            errorMessage = null
            true
        } else {
            errorMessage =
                result.exceptionOrNull()?.message
                    ?: "Ocurrió un error"

            false
        }
    }

    private fun refreshGoals() {
        goals = repository
            .getGoals()
            .toList()
    }

    private fun priorityValue(
        priority: GoalPriority
    ): Int {
        return when (priority) {
            GoalPriority.HIGH -> 3
            GoalPriority.MEDIUM -> 2
            GoalPriority.LOW -> 1
        }
    }

    private fun deadlineToSortableValue(
        deadline: String?
    ): Int {
        if (deadline.isNullOrBlank()) {
            return Int.MAX_VALUE
        }

        return try {
            val parts = deadline.split("/")

            if (parts.size != 3) {
                return Int.MAX_VALUE
            }

            val day = parts[0].toInt()
            val month = parts[1].toInt()
            val year = parts[2].toInt()

            year * 10_000 +
                    month * 100 +
                    day
        } catch (_: Exception) {
            Int.MAX_VALUE
        }
    }
}