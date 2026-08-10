package fintrack.proyecto4.notifications

import fintrack.proyecto4.budget.BudgetRepository
import fintrack.proyecto4.budget.currentPeriodKey
import fintrack.proyecto4.onboarding.OnboardingRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class BudgetAlertService(
    private val budgetRepository: BudgetRepository,
    private val notificationRepository: NotificationRepository,
    private val onboardingRepository: OnboardingRepository
) {

    @OptIn(ExperimentalTime::class)
    private fun now(): Long = Clock.System.now().toEpochMilliseconds()

    suspend fun onExpenseRegistered(uid: String, categoryName: String, amount: Long) {
        if (uid.isBlank() || amount <= 0) return

        val profile = try { onboardingRepository.getProfile(uid) } catch (_: Exception) { null }
        val alertsEnabled = profile?.budgetAlertEnabled ?: true
        if (!alertsEnabled) return

        val period = currentPeriodKey()
        val budgets = try { budgetRepository.getBudgets(uid) } catch (_: Exception) { return }
        val budget = budgets.firstOrNull {
            it.categoryName == categoryName && (it.periodKey.isBlank() || it.periodKey == period)
        } ?: return

        val newSpent = budget.spent + amount
        try { budgetRepository.updateSpent(uid, budget.id, newSpent) } catch (_: Exception) { return }

        val updated = budget.copy(spent = newSpent)
        val pct = updated.rawUsagePct

        when {
            pct > 1.0f && !budget.exceededSent -> {
                val title = "Presupuesto excedido"
                val body = "Superaste tu presupuesto de ${categoryName} " +
                    "(${updated.usagePercentInt}% de ${formatColones(updated.limit)})."
                emit(uid, NotificationType.BUDGET_EXCEEDED, title, body)
                budgetRepository.markAlertSent(uid, budget.id, alertSent = true, exceededSent = true)
            }
            pct >= updated.alertThreshold && !budget.alertSent -> {
                val title = "Alerta de presupuesto"
                val body = "Vas al ${updated.usagePercentInt}% de tu presupuesto de ${categoryName}."
                emit(uid, NotificationType.BUDGET_ALERT, title, body)
                budgetRepository.markAlertSent(uid, budget.id, alertSent = true, exceededSent = budget.exceededSent)
            }
        }
    }

    private suspend fun emit(uid: String, type: NotificationType, title: String, body: String) {
        notificationRepository.addNotification(
            uid,
            AppNotification(type = type, title = title, body = body, read = false, createdAt = now())
        )
        showLocalNotification(title, body)
    }

    private fun formatColones(amount: Double): String {
        val n = amount.toLong().toString()
            .reversed().chunked(3).joinToString(" ").reversed()
        return "₡$n"
    }
}
