package edu.bluejack26_1.wakord.ui.feature.auth

import androidx.annotation.StringRes
import edu.bluejack26_1.wakord.domain.model.Instrument
import edu.bluejack26_1.wakord.domain.validation.FieldError

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: FieldError? = null,
    val passwordError: FieldError? = null,
    val isLoading: Boolean = false,
    val showLoginFailed: Boolean = false,
)

sealed interface LoginAction {
    data class EmailChanged(val value: String) : LoginAction
    data class PasswordChanged(val value: String) : LoginAction
    data object Submit : LoginAction
    data object DismissLoginFailed : LoginAction
}

sealed interface LoginEffect {
    data object NavigateHome : LoginEffect
}

data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val fullNameError: FieldError? = null,
    val emailError: FieldError? = null,
    val passwordError: FieldError? = null,
    val confirmPasswordError: FieldError? = null,
    val isLoading: Boolean = false,
    @param:StringRes val errorMessage: Int? = null,
    val showAccountCreated: Boolean = false,
)

sealed interface RegisterAction {
    data class FullNameChanged(val value: String) : RegisterAction
    data class EmailChanged(val value: String) : RegisterAction
    data class PasswordChanged(val value: String) : RegisterAction
    data class ConfirmPasswordChanged(val value: String) : RegisterAction
    data object Submit : RegisterAction
    data object ContinueAfterCreated : RegisterAction
}

sealed interface RegisterEffect {
    data object NavigateToPreferences : RegisterEffect
}

val DefaultGenres = listOf(
    "Pop", "Acoustic", "Rock", "Neo-Soul", "Jazz", "Indie Folk",
    "R&B", "Worship", "Dangdut", "Blues", "Lo-fi", "Metal",
)

data class PreferencesUiState(
    val selectedInstruments: Set<Instrument> = emptySet(),
    val availableGenres: List<String> = DefaultGenres,
    val selectedGenres: Set<String> = emptySet(),
    val genreError: FieldError? = null,
    val isSaving: Boolean = false,
    @param:StringRes val errorMessage: Int? = null,
)

sealed interface PreferencesAction {
    data class ToggleInstrument(val instrument: Instrument) : PreferencesAction
    data class ToggleGenre(val genre: String) : PreferencesAction
    data object Continue : PreferencesAction
    data object Skip : PreferencesAction
}

sealed interface PreferencesEffect {
    data object NavigateHome : PreferencesEffect
}

data class ResetPasswordUiState(
    val email: String = "",
    val emailError: FieldError? = null,
    val isSending: Boolean = false,
    val isSent: Boolean = false,
    @param:StringRes val errorMessage: Int? = null,
)

sealed interface ResetPasswordAction {
    data class EmailChanged(val value: String) : ResetPasswordAction
    data object Send : ResetPasswordAction
}
