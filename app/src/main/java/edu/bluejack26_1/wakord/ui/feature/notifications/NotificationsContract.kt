package edu.bluejack26_1.wakord.ui.feature.notifications

import androidx.annotation.StringRes
import edu.bluejack26_1.wakord.domain.model.AppNotification
import edu.bluejack26_1.wakord.domain.model.TargetType

data class NotificationsUiState(
    val isLoading: Boolean = true,
    val newItems: List<AppNotification> = emptyList(),
    val earlierItems: List<AppNotification> = emptyList(),
    val unreadCount: Int = 0,
    @param:StringRes val errorMessage: Int? = null,
) {
    val isEmpty: Boolean get() = !isLoading && newItems.isEmpty() && earlierItems.isEmpty()
}

sealed interface NotificationsAction {
    data class Open(val notification: AppNotification) : NotificationsAction
    data object MarkAllRead : NotificationsAction
    data object DismissError : NotificationsAction
}

sealed interface NotificationsEffect {
    data class Navigate(val targetType: TargetType, val targetId: String) : NotificationsEffect
}
