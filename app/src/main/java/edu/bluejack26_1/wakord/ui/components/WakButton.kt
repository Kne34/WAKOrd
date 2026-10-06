package edu.bluejack26_1.wakord.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import edu.bluejack26_1.wakord.ui.theme.ButtonTextStyle
import edu.bluejack26_1.wakord.ui.theme.WakTheme

enum class WakButtonVariant { Primary, Secondary, Ghost, Destructive, Text }

@Composable
fun WakButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: WakButtonVariant = WakButtonVariant.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: Painter? = null,
) {
    val c = WakTheme.colors
    val transparent = variant == WakButtonVariant.Ghost || variant == WakButtonVariant.Text
    val container = when {
        !enabled && !transparent -> c.s4
        variant == WakButtonVariant.Primary -> c.primary
        variant == WakButtonVariant.Secondary -> c.s3
        variant == WakButtonVariant.Destructive -> c.error.copy(alpha = 0.12f)
        else -> Color.Transparent
    }
    val content = when {
        !enabled -> c.text3
        variant == WakButtonVariant.Primary -> c.onPrimary
        variant == WakButtonVariant.Secondary -> c.text
        variant == WakButtonVariant.Ghost -> c.text2
        variant == WakButtonVariant.Destructive -> c.error
        else -> c.primaryText
    }
    val border = if (variant == WakButtonVariant.Secondary && enabled) BorderStroke(1.dp, c.outline) else null
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 54.dp),
        enabled = enabled && !loading,
        shape = RoundedCornerShape(18.dp),
        color = container,
        contentColor = content,
        border = border,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = content,
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (leadingIcon != null) {
                        Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(text, style = ButtonTextStyle)
                }
            }
        }
    }
}
