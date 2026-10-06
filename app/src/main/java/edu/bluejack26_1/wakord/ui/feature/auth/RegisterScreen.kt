package edu.bluejack26_1.wakord.ui.feature.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.domain.validation.FieldError
import edu.bluejack26_1.wakord.ui.components.WakButton
import edu.bluejack26_1.wakord.ui.components.WakDialog
import edu.bluejack26_1.wakord.ui.components.WakTextField
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme

@Composable
fun RegisterScreen(
    state: RegisterUiState,
    onAction: (RegisterAction) -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = WakTheme.colors
    val focus = LocalFocusManager.current
    val submit = {
        focus.clearFocus()
        onAction(RegisterAction.Submit)
    }

    AuthScaffold(
        modifier = modifier,
        background = R.drawable.bg_register,
        backgroundHeight = 380.dp,
        bottom = {
            WakButton(
                text = stringResource(R.string.create_account),
                onClick = submit,
                modifier = Modifier.fillMaxWidth(),
                loading = state.isLoading,
            )
            Spacer(Modifier.height(18.dp))
            AuthSwitchPrompt(
                prompt = stringResource(R.string.have_account_prompt),
                action = stringResource(R.string.log_in),
                onClick = onBackToLogin,
            )
        },
    ) {
        AuthTopBar(onBack = onBackToLogin, end = { StepLabel(step = 1, total = 2) })
        Spacer(Modifier.height(28.dp))
        AuthHeadline(stringResource(R.string.register_title), stringResource(R.string.register_subtitle))
        Spacer(Modifier.height(24.dp))
        WakTextField(
            value = state.fullName,
            onValueChange = { onAction(RegisterAction.FullNameChanged(it)) },
            label = stringResource(R.string.field_full_name),
            leadingIcon = painterResource(R.drawable.ic_user),
            placeholder = stringResource(R.string.full_name_placeholder),
            error = fieldErrorText(state.fullNameError, R.string.error_name_required),
        )
        Spacer(Modifier.height(16.dp))
        WakTextField(
            value = state.email,
            onValueChange = { onAction(RegisterAction.EmailChanged(it)) },
            label = stringResource(R.string.field_email),
            leadingIcon = painterResource(R.drawable.ic_mail),
            placeholder = stringResource(R.string.email_placeholder),
            error = fieldErrorText(state.emailError, R.string.error_email_required),
            keyboardType = KeyboardType.Email,
        )
        Spacer(Modifier.height(16.dp))
        WakTextField(
            value = state.password,
            onValueChange = { onAction(RegisterAction.PasswordChanged(it)) },
            label = stringResource(R.string.field_password),
            leadingIcon = painterResource(R.drawable.ic_lock),
            error = fieldErrorText(state.passwordError, R.string.error_password_required),
            isPassword = true,
        )
        Spacer(Modifier.height(16.dp))
        WakTextField(
            value = state.confirmPassword,
            onValueChange = { onAction(RegisterAction.ConfirmPasswordChanged(it)) },
            label = stringResource(R.string.field_confirm_password),
            leadingIcon = painterResource(R.drawable.ic_lock),
            error = fieldErrorText(state.confirmPasswordError, R.string.error_confirm_required),
            isPassword = true,
            imeAction = ImeAction.Done,
            onImeAction = submit,
        )
        if (state.errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(state.errorMessage), style = MaterialTheme.typography.bodySmall, color = c.error)
        }
    }

    if (state.showAccountCreated) {
        val firstName = state.fullName.trim().substringBefore(' ')
        WakDialog(
            title = stringResource(R.string.account_created_title),
            message = stringResource(R.string.account_created_message, firstName),
            primaryText = stringResource(R.string.account_created_action),
            onPrimary = { onAction(RegisterAction.ContinueAfterCreated) },
            onDismissRequest = { onAction(RegisterAction.ContinueAfterCreated) },
            icon = painterResource(R.drawable.ic_account_created),
        )
    }
}

@Preview(name = "Register")
@Composable
private fun RegisterPreview() {
    WAKordTheme {
        RegisterScreen(
            state = RegisterUiState(fullName = "Keane Wilson", email = "keane.wilson@mail.com", password = "wakordpass", confirmPassword = "wakordpass"),
            onAction = {},
            onBackToLogin = {},
        )
    }
}

@Preview(name = "Register - errors")
@Composable
private fun RegisterErrorPreview() {
    WAKordTheme {
        RegisterScreen(
            state = RegisterUiState(
                fullNameError = FieldError.REQUIRED,
                email = "keane.wilson@mail",
                emailError = FieldError.INVALID_EMAIL,
                password = "wakord",
                passwordError = FieldError.PASSWORD_TOO_SHORT,
                confirmPasswordError = FieldError.REQUIRED,
            ),
            onAction = {},
            onBackToLogin = {},
        )
    }
}
