package fintrack.proyecto4.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotificationsState(
    val notifications: List<AppNotification> = emptyList(),
    val isLoading: Boolean = true
)

class NotificationsViewModel(
    private val repository: NotificationRepository,
    private val uid: String
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsState())
    val state: StateFlow<NotificationsState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val items = repository.getNotifications(uid)
            _state.value = NotificationsState(notifications = items, isLoading = false)
            repository.markAllRead(uid)
        }
    }
}
