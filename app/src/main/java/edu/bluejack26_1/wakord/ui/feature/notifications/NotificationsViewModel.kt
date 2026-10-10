package edu.bluejack26_1.wakord.ui.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bluejack26_1.wakord.WAKordApp
import edu.bluejack26_1.wakord.core.error.messageRes
import edu.bluejack26_1.wakord.domain.repository.NotificationRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotificationsViewModel(
    private val notificationRepository: NotificationRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<NotificationsEffect>(Channel.BUFFERED)
    val effects: Flow<NotificationsEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            notificationRepository.notifications.collect { list ->
                val cutoff = clock() - NEW_WINDOW_MS
                val (newItems, earlier) = list.partition { !it.isRead || it.createdAt >= cutoff }
                _uiState.update {
                    it.copy(isLoading = false, newItems = newItems, earlierItems = earlier, unreadCount = list.count { n -> !n.isRead })
                }
            }
        }
    }

    fun onAction(action: NotificationsAction) {
        when (action) {
            is NotificationsAction.Open -> viewModelScope.launch {
                if (!action.notification.isRead) notificationRepository.markRead(action.notification.id)
                _effects.send(NotificationsEffect.Navigate(action.notification.targetType, action.notification.targetId))
            }
            NotificationsAction.MarkAllRead -> viewModelScope.launch {
                notificationRepository.markAllRead().onFailure { e -> _uiState.update { it.copy(errorMessage = e.messageRes()) } }
            }
            NotificationsAction.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    companion object {
        private const val NEW_WINDOW_MS = 24 * 60 * 60 * 1000L

        val Factory = viewModelFactory {
            initializer {
                NotificationsViewModel((this[APPLICATION_KEY] as WAKordApp).container.notificationRepository)
            }
        }
    }
}
