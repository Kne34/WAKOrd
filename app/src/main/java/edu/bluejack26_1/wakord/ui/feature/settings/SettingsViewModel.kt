package edu.bluejack26_1.wakord.ui.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bluejack26_1.wakord.WAKordApp
import edu.bluejack26_1.wakord.core.audio.PlaybackController
import edu.bluejack26_1.wakord.domain.repository.AuthRepository
import edu.bluejack26_1.wakord.domain.repository.SettingsRepository
import edu.bluejack26_1.wakord.domain.repository.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository,
    private val playbackController: PlaybackController,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { settingsRepository.themeMode.collect { mode -> _uiState.update { it.copy(themeMode = mode) } } }
        viewModelScope.launch { settingsRepository.referencePitch.collect { hz -> _uiState.update { it.copy(referencePitch = hz) } } }
        viewModelScope.launch { settingsRepository.pushEnabled.collect { on -> _uiState.update { it.copy(pushEnabled = on) } } }
        viewModelScope.launch { settingsRepository.keepOffline.collect { on -> _uiState.update { it.copy(keepOffline = on) } } }
        viewModelScope.launch { syncRepository.status.collect { sync -> _uiState.update { it.copy(sync = sync) } } }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SelectTheme -> viewModelScope.launch { settingsRepository.setThemeMode(action.mode) }
            SettingsAction.OpenReferencePitch -> _uiState.update { it.copy(isPitchSheetOpen = true) }
            SettingsAction.CloseReferencePitch -> _uiState.update { it.copy(isPitchSheetOpen = false) }
            is SettingsAction.SetReferencePitch -> setReferencePitch(action.hz)
            is SettingsAction.SetPushEnabled -> viewModelScope.launch { settingsRepository.setPushEnabled(action.enabled) }
            is SettingsAction.SetKeepOffline -> viewModelScope.launch { settingsRepository.setKeepOffline(action.enabled) }
            SettingsAction.RequestLogout -> _uiState.update { it.copy(isLogoutDialogOpen = true) }
            SettingsAction.DismissLogout -> _uiState.update { if (it.isLoggingOut) it else it.copy(isLogoutDialogOpen = false) }
            SettingsAction.ConfirmLogout -> logout()
        }
    }

    private fun setReferencePitch(hz: Float) {
        val pitch = hz.roundToInt().coerceIn(MIN_PITCH, MAX_PITCH).toFloat()
        _uiState.update { it.copy(referencePitch = pitch) }
        viewModelScope.launch { settingsRepository.setReferencePitch(pitch) }
    }

    private fun logout() {
        if (_uiState.value.isLoggingOut) return
        _uiState.update { it.copy(isLoggingOut = true) }
        viewModelScope.launch {
            playbackController.close()
            authRepository.logout()
            _uiState.update { it.copy(isLoggingOut = false, isLogoutDialogOpen = false, isLoggedOut = true) }
        }
    }

    companion object {
        const val MIN_PITCH = 430
        const val MAX_PITCH = 450
        const val DEFAULT_PITCH = 440

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as WAKordApp).container
                SettingsViewModel(
                    container.settingsRepository,
                    container.syncRepository,
                    container.authRepository,
                    container.playbackController,
                )
            }
        }
    }
}
