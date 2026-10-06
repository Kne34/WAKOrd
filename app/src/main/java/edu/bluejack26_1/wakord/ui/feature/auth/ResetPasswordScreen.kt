package edu.bluejack26_1.wakord.ui.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.ui.components.WakButton
import edu.bluejack26_1.wakord.ui.components.WakTextField
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme

@Composable
fun ResetPasswordScreen(
    state: ResetPasswordUiState,
    onAction: (ResetPasswordAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = WakTheme.colors
    AuthScaffold(
        modifier = modifier,
        background = R.drawable.bg_login,
        bottom = {
            if (state.isSent) {
                WakButton(
                    text = stringResource(R.string.back_to_login),
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                WakButton(
                    text = stringResource(R.string.send_reset_link),
                    onClick = { onAction(ResetPasswordAction.Send) },
                    modifier = Modifier.fillMaxWidth(),
                    loading = state.isSending,
                )
            }
        },
    ) {
        AuthTopBar(onBack = onBack)
        Spacer(Modifier.height(28.dp))
        if (state.isSent) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(c.primary.copy(alpha = 0.12f), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_mail), contentDescription = null, tint = c.primary, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(20.dp))
            AuthHeadline(stringResource(R.string.check_your_email), stringResource(R.string.reset_link_sent, state.email))
        } else {
            AuthHeadline(stringResource(R.string.reset_password), stringResource(R.string.reset_password_subtitle))
            Spacer(Modifier.height(28.dp))
            WakTextField(
                value = state.email,
                onValueChange = { onAction(ResetPasswordAction.EmailChanged(it)) },
                label = stringResource(R.string.field_email),
                leadingIcon = painterResource(R.drawable.ic_mail),
                placeholder = stringResource(R.string.email_placeholder),
                error = fieldErrorText(state.emailError, R.string.error_email_required),
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
                onImeAction = { onAction(ResetPasswordAction.Send) },
            )
            if (state.errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Text(stringResource(state.errorMessage), style = MaterialTheme.typography.bodySmall, color = c.error)
            }
        }
    }
}

@Preview(name = "Reset password")
@Composable
private fun ResetPasswordPreview() {
    WAKordTheme {
        ResetPasswordScreen(ResetPasswordUiState(email = "keane.wilson@mail.com"), {}, {})
    }
}

@Preview(name = "Reset password - sent")
@Composable
private fun ResetPasswordSentPreview() {
    WAKordTheme {
        ResetPasswordScreen(ResetPasswordUiState(email = "keane.wilson@mail.com", isSent = true), {}, {})
    }
}
