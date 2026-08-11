package fintrack.proyecto4.notifications

enum class NotificationType {
    BUDGET_ALERT,
    BUDGET_EXCEEDED
}

data class AppNotification(
    val id: String = "",
    val type: NotificationType = NotificationType.BUDGET_ALERT,
    val title: String = "",
    val body: String = "",
    val read: Boolean = false,
    val createdAt: Long = 0L
)

interface NotificationRepository {
    suspend fun getNotifications(uid: String): List<AppNotification>
    suspend fun addNotification(uid: String, notification: AppNotification)
    suspend fun markAllRead(uid: String)
    suspend fun unreadCount(uid: String): Int
}

class NoOpNotificationRepository : NotificationRepository {
    override suspend fun getNotifications(uid: String): List<AppNotification> = emptyList()
    override suspend fun addNotification(uid: String, notification: AppNotification) = Unit
    override suspend fun markAllRead(uid: String) = Unit
    override suspend fun unreadCount(uid: String): Int = 0
}
