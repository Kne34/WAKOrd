package edu.bluejack26_1.wakord.ui.feature.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import edu.bluejack26_1.wakord.ui.components.DialogTone
import edu.bluejack26_1.wakord.ui.components.WakButton
import edu.bluejack26_1.wakord.ui.components.WakDialog
import edu.bluejack26_1.wakord.ui.components.WakTextField
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme

@Composable
fun LoginScreen(
    state: LoginUiState,
    onAction: (LoginAction) -> Unit,
    onForgotPassword: () -> Unit,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focus = LocalFocusManager.current
    val submit = {
        focus.clearFocus()
        onAction(LoginAction.Submit)
    }

    AuthScaffold(
        modifier = modifier,
        background = R.drawable.bg_login,
        bottom = {
            WakButton(
                text = stringResource(R.string.log_in),
                onClick = submit,
                modifier = Modifier.fillMaxWidth(),
                loading = state.isLoading,
            )
            Spacer(Modifier.height(18.dp))
            AuthSwitchPrompt(
                prompt = stringResource(R.string.no_account_prompt),
                action = stringResource(R.string.register),
                onClick = onRegister,
            )
        },
    ) {
        Spacer(Modifier.height(20.dp))
        AuthLogoMark()
        Spacer(Modifier.height(18.dp))
        AuthHeadline(stringResource(R.string.login_title), stringResource(R.string.login_subtitle))
        Spacer(Modifier.height(28.dp))
        WakTextField(
            value = state.email,
            onValueChange = { onAction(LoginAction.EmailChanged(it)) },
            label = stringResource(R.string.field_email),
            leadingIcon = painterResource(R.drawable.ic_mail),
            placeholder = stringResource(R.string.email_placeholder),
            error = fieldErrorText(state.emailError, R.string.error_email_required),
            keyboardType = KeyboardType.Email,
        )
        Spacer(Modifier.height(18.dp))
        WakTextField(
            value = state.password,
            onValueChange = { onAction(LoginAction.PasswordChanged(it)) },
            label = stringResource(R.string.field_password),
            leadingIcon = painterResource(R.drawable.ic_lock),
            error = fieldErrorText(state.passwordError, R.string.error_password_required),
            isPassword = true,
            imeAction = ImeAction.Done,
            onImeAction = submit,
        )
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            AuthLinkText(stringResource(R.string.forgot_password), onForgotPassword)
        }
    }

    if (state.showLoginFailed) {
        WakDialog(
            title = stringResource(R.string.login_failed_title),
            message = stringResource(R.string.login_failed_message),
            primaryText = stringResource(R.string.try_again),
            onPrimary = { onAction(LoginAction.DismissLoginFailed) },
            onDismissRequest = { onAction(LoginAction.DismissLoginFailed) },
            secondaryText = stringResource(R.string.reset_password),
            onSecondary = {
                onAction(LoginAction.DismissLoginFailed)
                onForgotPassword()
            },
            icon = painterResource(R.drawable.ic_login_failed),
            tone = DialogTone.Error,
        )
    }
}

@Preview(name = "Login")
@Composable
private fun LoginPreview() {
    WAKordTheme {
        LoginScreen(LoginUiState(email = "keane.wilson@mail.com", password = "wakordpass"), {}, {}, {})
    }
}

@Preview(name = "Login - required errors")
@Composable
private fun LoginErrorPreview() {
    WAKordTheme {
        LoginScreen(LoginUiState(emailError = FieldError.REQUIRED, passwordError = FieldError.REQUIRED), {}, {}, {})
    }
}

@Preview(name = "Login - light")
@Composable
private fun LoginLightPreview() {
    WAKordTheme(darkTheme = false) {
        LoginScreen(LoginUiState(), {}, {}, {})
    }
}
