package edu.bluejack26_1.wakord.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import edu.bluejack26_1.wakord.ui.theme.WakTheme

enum class DialogTone { Default, Destructive }

@Composable
fun WakDialog(
    title: String,
    message: String,
    primaryText: String,
    onPrimary: () -> Unit,
    onDismissRequest: () -> Unit,
    secondaryText: String? = null,
    onSecondary: () -> Unit = onDismissRequest,
    icon: ImageVector? = null,
    tone: DialogTone = DialogTone.Default,
) {
    val c = WakTheme.colors
    val accent = if (tone == DialogTone.Destructive) c.error else c.primary
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(shape = RoundedCornerShape(24.dp), color = c.s2) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(accent.copy(alpha = 0.16f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, contentDescription = null, tint = accent)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Text(title, style = MaterialTheme.typography.titleLarge, color = c.text, textAlign = TextAlign.Center)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = c.text2, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                WakButton(
                    text = primaryText,
                    onClick = onPrimary,
                    modifier = Modifier.fillMaxWidth(),
                    variant = if (tone == DialogTone.Destructive) WakButtonVariant.Destructive else WakButtonVariant.Primary,
                )
                if (secondaryText != null) {
                    WakButton(
                        text = secondaryText,
                        onClick = onSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        variant = WakButtonVariant.Secondary,
                    )
                }
            }
        }
    }
}
