package edu.bluejack26_1.wakord.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import edu.bluejack26_1.wakord.ui.theme.WakColors
import edu.bluejack26_1.wakord.ui.theme.WakTheme

fun glassColor(c: WakColors) = c.s2.copy(alpha = if (c.isDark) 0.62f else 0.74f)

fun glassBorder(c: WakColors) = BorderStroke(1.dp, c.text.copy(alpha = if (c.isDark) 0.09f else 0.08f))

fun alertContainer(c: WakColors) = if (c.isDark) Color(0xFF893200).copy(alpha = 0.38f) else c.peachText.copy(alpha = 0.12f)

@Composable
fun GlassIconButton(
    painter: Painter,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = WakTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        color = glassColor(c),
        border = glassBorder(c),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painter, contentDescription = contentDescription, tint = c.text, modifier = Modifier.size(20.dp))
        }
    }
}
