package fintrack.proyecto4.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsState(
    val notifications: List<AppNotification> = emptyList(),
    val isLoading: Boolean = true
) {
    /** Bandeja vacía solo tras cargar (para no mostrar el mensaje durante la carga). */
    val isEmpty: Boolean get() = !isLoading && notifications.isEmpty()
}

/**
 * ViewModel de la bandeja de notificaciones (US-43).
 *
 * Al abrir la pantalla carga las notificaciones (ya ordenadas DESC por el repositorio) y,
 * acto seguido, marca todas como leídas para que el badge de la campana desaparezca. La
 * lista mostrada conserva el estado leído/no leído del momento de abrir, así el usuario
 * todavía distingue visualmente cuáles eran nuevas.
 *
 * @param uid Usuario en sesión (ver AuthClient.currentUserId()).
 */
class NotificationsViewModel(
    private val repository: NotificationRepository,
    private val uid: String
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsState())
    val state: StateFlow<NotificationsState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val items = try {
                repository.getNotifications(uid)
            } catch (e: Exception) {
                emptyList()
            }
            _state.update { it.copy(isLoading = false, notifications = items) }

            // Regla de negocio US-43: al abrir la bandeja, todas las visibles se marcan leídas.
            if (items.any { !it.isRead }) {
                try {
                    repository.markAllAsRead(uid)
                } catch (e: Exception) {
                    // Best-effort; el badge se recalcula en la próxima carga del Dashboard.
                }
            }
        }
    }

    /** Elimina físicamente la notificación (se llama solo tras confirmar el diálogo). */
    fun delete(notification: AppNotification) {
        viewModelScope.launch {
            try {
                repository.deleteNotification(uid, notification.id)
            } catch (e: Exception) {
                // Si falla el borrado remoto, se quita igual de la lista local; la próxima
                // carga reconciliará el estado real.
            }
            _state.update { current ->
                current.copy(notifications = current.notifications.filterNot { it.id == notification.id })
            }
        }
    }
}
