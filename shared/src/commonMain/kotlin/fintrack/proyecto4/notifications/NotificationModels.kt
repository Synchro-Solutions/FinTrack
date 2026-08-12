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

/** Días que se conservan las notificaciones antes de eliminarse automáticamente (US-43). */
const val NotificationRetentionDays = 30

interface NotificationRepository {
    suspend fun getNotifications(uid: String): List<AppNotification>
    suspend fun addNotification(uid: String, notification: AppNotification)
    suspend fun markAllRead(uid: String)
    suspend fun unreadCount(uid: String): Int

    /** Elimina físicamente una notificación (swipe con confirmación, US-43). */
    suspend fun deleteNotification(uid: String, notificationId: String)
}

class NoOpNotificationRepository : NotificationRepository {
    override suspend fun getNotifications(uid: String): List<AppNotification> = emptyList()
    override suspend fun addNotification(uid: String, notification: AppNotification) = Unit
    override suspend fun markAllRead(uid: String) = Unit
    override suspend fun unreadCount(uid: String): Int = 0
    override suspend fun deleteNotification(uid: String, notificationId: String) = Unit
}
