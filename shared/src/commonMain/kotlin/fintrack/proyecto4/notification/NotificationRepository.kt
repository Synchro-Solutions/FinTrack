package fintrack.proyecto4.notification

interface NotificationRepository {
    suspend fun addNotification(uid: String, notification: BudgetAlertNotification)
    suspend fun getNotifications(uid: String): List<BudgetAlertNotification>
}

class NoOpNotificationRepository : NotificationRepository {
    override suspend fun addNotification(uid: String, notification: BudgetAlertNotification) = Unit
    override suspend fun getNotifications(uid: String): List<BudgetAlertNotification> = emptyList()
}
