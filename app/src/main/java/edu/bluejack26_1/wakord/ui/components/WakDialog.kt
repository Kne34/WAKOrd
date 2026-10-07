package edu.bluejack26_1.wakord.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme

enum class DialogTone { Default, Error, Destructive }

@Composable
fun WakDialog(
    title: String,
    message: String,
    primaryText: String,
    onPrimary: () -> Unit,
    onDismissRequest: () -> Unit,
    secondaryText: String? = null,
    onSecondary: () -> Unit = onDismissRequest,
    icon: Painter? = null,
    tone: DialogTone = DialogTone.Default,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        WakDialogContent(title, message, primaryText, onPrimary, secondaryText, onSecondary, icon, tone)
    }
}

@Composable
private fun WakDialogContent(
    title: String,
    message: String,
    primaryText: String,
    onPrimary: () -> Unit,
    secondaryText: String?,
    onSecondary: () -> Unit,
    icon: Painter?,
    tone: DialogTone,
) {
    val c = WakTheme.colors
    val accent = if (tone == DialogTone.Default) c.primary else c.error
    Surface(
        modifier = Modifier.widthIn(max = 326.dp),
        shape = RoundedCornerShape(30.dp),
        color = c.s2,
        border = BorderStroke(1.dp, c.text2.copy(alpha = 0.10f)),
    ) {
        Column(
            modifier = Modifier.padding(start = 23.dp, end = 23.dp, top = 27.dp, bottom = 21.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(accent.copy(alpha = 0.12f), RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(14.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = c.text,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = c.text2,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            WakButton(
                text = primaryText,
                onClick = onPrimary,
                modifier = Modifier.fillMaxWidth(),
                variant = if (tone == DialogTone.Destructive) WakButtonVariant.Destructive else WakButtonVariant.Primary,
            )
            if (secondaryText != null) {
                Spacer(Modifier.height(8.dp))
                WakButton(
                    text = secondaryText,
                    onClick = onSecondary,
                    modifier = Modifier.fillMaxWidth(),
                    variant = WakButtonVariant.Ghost,
                )
            }
        }
    }
}

@Preview(name = "Dialog - login failed")
@Composable
private fun WakDialogPreview() {
    WAKordTheme {
        WakDialogContent(
            title = "Couldn't log you in",
            message = "The email or password you entered is incorrect. Please try again.",
            primaryText = "Try again",
            onPrimary = {},
            secondaryText = "Reset password",
            onSecondary = {},
            icon = painterResource(R.drawable.ic_login_failed),
            tone = DialogTone.Error,
        )
    }
}
