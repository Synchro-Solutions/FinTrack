package fintrack.proyecto4.savings.remote

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import fintrack.proyecto4.auth.AuthClient
import fintrack.proyecto4.savings.model.GoalStatus
import fintrack.proyecto4.savings.model.SavingsContribution
import fintrack.proyecto4.savings.model.SavingsGoal

class SavingsFirestoreRepository {

    private val firestore = Firebase.firestore

    private fun currentUserId(): String {
        return AuthClient.currentUserId()
            ?: error("Usuario no autenticado")
    }

    private fun goalsCollection() =
        firestore
            .collection("users")
            .document(currentUserId())
            .collection("goals")

    private fun entriesCollection(goalId: String) =
        goalsCollection()
            .document(goalId)
            .collection("entries")

    suspend fun getGoals(): List<SavingsGoal> {
        return goalsCollection()
            .get()
            .documents
            .map { document ->
                SavingsGoal(
                    id = document.get<String?>("id") ?: document.id,
                    name = document.get<String?>("name") ?: "",
                    targetAmount = document.get<Double?>("targetAmount") ?: 0.0,
                    currentAmount = document.get<Double?>("currentAmount") ?: 0.0,
                    deadline = document.get<String?>("deadline"),
                    iconName = document.get<String?>("iconName") ?: "⭐",
                    status = GoalStatus.valueOf(
                        document.get<String?>("status") ?: GoalStatus.ACTIVE.name
                    )
                )
            }
    }

    suspend fun saveGoal(goal: SavingsGoal) {
        goalsCollection()
            .document(goal.id)
            .set(
                mapOf(
                    "id" to goal.id,
                    "name" to goal.name,
                    "targetAmount" to goal.targetAmount,
                    "currentAmount" to goal.currentAmount,
                    "deadline" to goal.deadline,
                    "iconName" to goal.iconName,
                    "status" to goal.status.name
                )
            )
    }

    suspend fun saveContribution(contribution: SavingsContribution) {
        entriesCollection(contribution.goalId)
            .document(contribution.id)
            .set(
                mapOf(
                    "id" to contribution.id,
                    "goalId" to contribution.goalId,
                    "amount" to contribution.amount,
                    "createdAt" to contribution.createdAt
                )
            )
    }

    suspend fun getContributions(goalId: String): List<SavingsContribution> {
        return entriesCollection(goalId)
            .get()
            .documents
            .map { document ->
                SavingsContribution(
                    id = document.get<String?>("id") ?: document.id,
                    goalId = document.get<String?>("goalId") ?: goalId,
                    amount = document.get<Double?>("amount") ?: 0.0,
                    createdAt = document.get<String?>("createdAt") ?: ""
                )
            }
    }
}