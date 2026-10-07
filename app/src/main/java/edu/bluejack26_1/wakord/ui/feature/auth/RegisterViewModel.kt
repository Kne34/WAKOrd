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

class RegisterViewModel(
    private val authRepository: AuthRepository,
    private val validator: AuthValidator,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _effects = Channel<RegisterEffect>(Channel.BUFFERED)
    val effects: Flow<RegisterEffect> = _effects.receiveAsFlow()

    fun onAction(action: RegisterAction) {
        when (action) {
            is RegisterAction.FullNameChanged -> _uiState.update { it.copy(fullName = action.value, fullNameError = null) }
            is RegisterAction.EmailChanged -> _uiState.update { it.copy(email = action.value, emailError = null, errorMessage = null) }
            is RegisterAction.PasswordChanged -> _uiState.update { it.copy(password = action.value, passwordError = null) }
            is RegisterAction.ConfirmPasswordChanged -> _uiState.update { it.copy(confirmPassword = action.value, confirmPasswordError = null) }
            RegisterAction.Submit -> submit()
            RegisterAction.ContinueAfterCreated -> {
                _uiState.update { it.copy(showAccountCreated = false) }
                viewModelScope.launch { _effects.send(RegisterEffect.NavigateToPreferences) }
            }
        }
    }

    private fun submit() {
        val s = _uiState.value
        if (s.isLoading) return
        val checked = s.copy(
            fullNameError = validator.fullName(s.fullName),
            emailError = validator.email(s.email),
            passwordError = validator.password(s.password),
            confirmPasswordError = validator.confirmPassword(s.password, s.confirmPassword),
            errorMessage = null,
        )
        _uiState.value = checked
        val hasError = listOf(checked.fullNameError, checked.emailError, checked.passwordError, checked.confirmPasswordError)
            .any { it != null }
        if (hasError) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authRepository.register(s.fullName, s.email, s.password)
            _uiState.update { state ->
                result.fold(
                    onSuccess = { state.copy(isLoading = false, showAccountCreated = true) },
                    onFailure = { state.copy(isLoading = false, errorMessage = it.messageRes()) },
                )
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as WAKordApp).container
                RegisterViewModel(container.authRepository, container.authValidator)
            }
        }
    }
}
