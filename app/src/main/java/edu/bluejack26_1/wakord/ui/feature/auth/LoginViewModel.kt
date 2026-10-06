package edu.bluejack26_1.wakord.ui.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bluejack26_1.wakord.WAKordApp
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

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val validator: AuthValidator,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _effects = Channel<LoginEffect>(Channel.BUFFERED)
    val effects: Flow<LoginEffect> = _effects.receiveAsFlow()

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.EmailChanged -> _uiState.update { it.copy(email = action.value, emailError = null) }
            is LoginAction.PasswordChanged -> _uiState.update { it.copy(password = action.value, passwordError = null) }
            LoginAction.DismissLoginFailed -> _uiState.update { it.copy(showLoginFailed = false) }
            LoginAction.Submit -> submit()
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (state.isLoading) return
        val emailError = validator.email(state.email)
        val passwordError = validator.loginPassword(state.password)
        _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
        if (emailError != null || passwordError != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authRepository.login(state.email, state.password)
            _uiState.update { it.copy(isLoading = false, showLoginFailed = result.isFailure) }
            if (result.isSuccess) _effects.send(LoginEffect.NavigateHome)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as WAKordApp).container
                LoginViewModel(container.authRepository, container.authValidator)
            }
        }
    }
}
