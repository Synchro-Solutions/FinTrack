package fintrack.proyecto4.notifications

/**
 * Tipo de notificación, usado para elegir el ícono/acento en la bandeja (US-43).
 * Se lee con un valor por defecto tolerante para no romper documentos guardados por
 * versiones anteriores (mismo criterio que el resto de entidades del proyecto).
 */
enum class NotificationType { BUDGET, GOAL, TIP, GENERAL }

/**
 * Notificación mostrada en la bandeja (US-43). Persistida en
 * users/{uid}/notifications/{id}.
 *
 * @param createdAt epoch en milisegundos; se usa para ordenar (DESC) y para la
 *   retención de 30 días.
 * @param isRead false hasta que el usuario abre la bandeja, momento en que todas las
 *   visibles se marcan como leídas.
 */
data class AppNotification(
    val id: String,
    val title: String,
    val body: String,
    val type: NotificationType,
    val isRead: Boolean,
    val createdAt: Long
)

/** Cuántos días se conservan las notificaciones antes de eliminarse automáticamente. */
const val NotificationRetentionDays = 30

/** Máximo de notificaciones que devuelve la bandeja (US-43: LIMIT 50). */
const val NotificationPageLimit = 50

interface NotificationRepository {
    /**
     * Notificaciones del usuario ordenadas por createdAt DESC (máx [NotificationPageLimit]).
     * Antes de devolverlas elimina las de más de [NotificationRetentionDays] días.
     */
    suspend fun getNotifications(uid: String): List<AppNotification>

    /** Cantidad de no leídas, para el badge de la campana en el Dashboard. */
    suspend fun unreadCount(uid: String): Int

    /** Marca como leídas todas las no leídas del usuario (al abrir la bandeja). */
    suspend fun markAllAsRead(uid: String)

    /** Elimina físicamente una notificación (swipe con confirmación). */
    suspend fun deleteNotification(uid: String, notificationId: String)

    /** Crea una notificación; devuelve la creada con el id generado. */
    suspend fun addNotification(uid: String, notification: AppNotification): AppNotification
}

/** Implementación por defecto para plataformas sin Firestore (Web, tests). */
class NoOpNotificationRepository : NotificationRepository {
    override suspend fun getNotifications(uid: String): List<AppNotification> = emptyList()
    override suspend fun unreadCount(uid: String): Int = 0
    override suspend fun markAllAsRead(uid: String) = Unit
    override suspend fun deleteNotification(uid: String, notificationId: String) = Unit
    override suspend fun addNotification(uid: String, notification: AppNotification): AppNotification = notification
}
