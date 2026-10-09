package edu.bluejack26_1.wakord.ui.feature.settings

import edu.bluejack26_1.wakord.domain.model.SyncStatus
import edu.bluejack26_1.wakord.domain.model.ThemeMode

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val referencePitch: Float = 440f,
    val pushEnabled: Boolean = true,
    val keepOffline: Boolean = true,
    val sync: SyncStatus = SyncStatus(),
    val isPitchSheetOpen: Boolean = false,
    val isLogoutDialogOpen: Boolean = false,
    val isLoggingOut: Boolean = false,
    val isLoggedOut: Boolean = false,
)

sealed interface SettingsAction {
    data class SelectTheme(val mode: ThemeMode) : SettingsAction
    data object OpenReferencePitch : SettingsAction
    data object CloseReferencePitch : SettingsAction
    data class SetReferencePitch(val hz: Float) : SettingsAction
    data class SetPushEnabled(val enabled: Boolean) : SettingsAction
    data class SetKeepOffline(val enabled: Boolean) : SettingsAction
    data object RequestLogout : SettingsAction
    data object DismissLogout : SettingsAction
    data object ConfirmLogout : SettingsAction
}
