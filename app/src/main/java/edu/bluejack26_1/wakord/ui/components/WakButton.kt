package edu.bluejack26_1.wakord.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import edu.bluejack26_1.wakord.ui.theme.WakTheme

enum class WakButtonVariant { Primary, Secondary, Destructive, Text }

@Composable
fun WakButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: WakButtonVariant = WakButtonVariant.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    val c = WakTheme.colors
    val shape = RoundedCornerShape(14.dp)
    val sized = modifier.heightIn(min = 52.dp)
    val content: @Composable RowScope.() -> Unit = {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current,
            )
        } else {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
    when (variant) {
        WakButtonVariant.Primary -> Button(
            onClick = onClick,
            modifier = sized,
            enabled = enabled && !loading,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = c.primary,
                contentColor = c.onPrimary,
                disabledContainerColor = c.s4,
                disabledContentColor = c.text3,
            ),
            content = content,
        )
        WakButtonVariant.Secondary -> OutlinedButton(
            onClick = onClick,
            modifier = sized,
            enabled = enabled && !loading,
            shape = shape,
            border = BorderStroke(1.dp, c.outline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = c.text, disabledContentColor = c.text3),
            content = content,
        )
        WakButtonVariant.Destructive -> Button(
            onClick = onClick,
            modifier = sized,
            enabled = enabled && !loading,
            shape = shape,
            colors = ButtonDefaults.buttonColors(containerColor = c.error, contentColor = c.onError),
            content = content,
        )
        WakButtonVariant.Text -> TextButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled && !loading,
            colors = ButtonDefaults.textButtonColors(contentColor = c.primaryText),
            content = content,
        )
    }
}
