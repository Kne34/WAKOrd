package edu.bluejack26_1.wakord.ui.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bluejack26_1.wakord.WAKordApp
import edu.bluejack26_1.wakord.core.error.messageRes
import edu.bluejack26_1.wakord.domain.repository.AuthRepository
import edu.bluejack26_1.wakord.domain.validation.AuthValidator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PreferencesViewModel(
    private val authRepository: AuthRepository,
    private val validator: AuthValidator,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PreferencesUiState())
    val uiState: StateFlow<PreferencesUiState> = _uiState.asStateFlow()

    private val _effects = Channel<PreferencesEffect>(Channel.BUFFERED)
    val effects: Flow<PreferencesEffect> = _effects.receiveAsFlow()

    fun onAction(action: PreferencesAction) {
        when (action) {
            is PreferencesAction.ToggleInstrument -> _uiState.update {
                it.copy(selectedInstruments = it.selectedInstruments.toggle(action.instrument))
            }
            is PreferencesAction.ToggleGenre -> _uiState.update {
                it.copy(selectedGenres = it.selectedGenres.toggle(action.genre), genreError = null)
            }
            PreferencesAction.Continue -> save()
            PreferencesAction.Skip -> viewModelScope.launch { _effects.send(PreferencesEffect.NavigateHome) }
        }
    }

    private fun save() {
        val s = _uiState.value
        if (s.isSaving) return
        val genreError = validator.genres(s.selectedGenres)
        _uiState.update { it.copy(genreError = genreError, errorMessage = null) }
        if (genreError != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = authRepository.savePreferences(s.selectedInstruments, s.selectedGenres)
            _uiState.update { it.copy(isSaving = false, errorMessage = result.exceptionOrNull()?.messageRes()) }
            if (result.isSuccess) _effects.send(PreferencesEffect.NavigateHome)
        }
    }

    private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as WAKordApp).container
                PreferencesViewModel(container.authRepository, container.authValidator)
            }
        }
    }
}
