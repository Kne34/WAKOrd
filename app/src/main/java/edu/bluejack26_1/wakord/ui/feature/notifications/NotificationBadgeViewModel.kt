package edu.bluejack26_1.wakord.ui.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bluejack26_1.wakord.WAKordApp
import edu.bluejack26_1.wakord.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn

class NotificationBadgeViewModel(notificationRepository: NotificationRepository) : ViewModel() {
    val unreadCount: StateFlow<Int> = notificationRepository.unreadCount
        .catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                NotificationBadgeViewModel((this[APPLICATION_KEY] as WAKordApp).container.notificationRepository)
            }
        }
    }
}
