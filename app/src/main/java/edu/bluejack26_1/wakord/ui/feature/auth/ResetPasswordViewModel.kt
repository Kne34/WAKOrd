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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ResetPasswordViewModel(
    private val authRepository: AuthRepository,
    private val validator: AuthValidator,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ResetPasswordUiState())
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

    fun onAction(action: ResetPasswordAction) {
        when (action) {
            is ResetPasswordAction.EmailChanged -> _uiState.update {
                it.copy(email = action.value, emailError = null, errorMessage = null)
            }
            ResetPasswordAction.Send -> send()
        }
    }

    private fun send() {
        val s = _uiState.value
        if (s.isSending) return
        val emailError = validator.email(s.email)
        _uiState.update { it.copy(emailError = emailError) }
        if (emailError != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, errorMessage = null) }
            val result = authRepository.sendPasswordReset(s.email)
            _uiState.update {
                it.copy(
                    isSending = false,
                    isSent = result.isSuccess,
                    errorMessage = result.exceptionOrNull()?.messageRes(),
                )
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as WAKordApp).container
                ResetPasswordViewModel(container.authRepository, container.authValidator)
            }
        }
    }
}
