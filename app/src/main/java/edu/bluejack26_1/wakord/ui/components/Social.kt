package edu.bluejack26_1.wakord.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.ui.theme.WakColors
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.spaceGrotesk
import java.util.Locale
import kotlin.math.abs

val AvatarSmall = 32.dp
val AvatarLarge = 38.dp

fun compactCount(value: Int): String = when {
    value < 1_000 -> value.toString()
    value < 1_000_000 -> trimmed(value / 1_000f) + "k"
    else -> trimmed(value / 1_000_000f) + "M"
}

private fun trimmed(value: Float): String {
    val text = String.format(Locale.US, "%.1f", value)
    return if (text.endsWith(".0")) text.dropLast(2) else text
}

@Composable
fun relativeTime(createdAt: Long, now: Long = System.currentTimeMillis()): String {
    val minutes = ((now - createdAt).coerceAtLeast(0) / 60_000).toInt()
    return when {
        minutes < 1 -> stringResource(R.string.time_now)
        minutes < 60 -> stringResource(R.string.time_minutes, minutes)
        minutes < 60 * 24 -> stringResource(R.string.time_hours, minutes / 60)
        minutes < 60 * 24 * 7 -> stringResource(R.string.time_days, minutes / (60 * 24))
        else -> stringResource(R.string.time_weeks, minutes / (60 * 24 * 7))
    }
}

fun initials(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

private fun avatarColors(seed: String, c: WakColors): Pair<Color, Color> {
    val dark = listOf(
        Color(0xFF00363B) to c.primary,
        Color(0xFF2F2C62) to c.lavender,
        Color(0xFF581D00) to c.peach,
        c.s4 to c.text,
    )
    val light = listOf(
        c.primaryText.copy(alpha = 0.14f) to c.primaryText,
        c.lavender.copy(alpha = 0.14f) to c.lavender,
        c.peachText.copy(alpha = 0.14f) to c.peachText,
        c.s4 to c.text,
    )
    val palette = if (c.isDark) dark else light
    return palette[abs(seed.hashCode()) % palette.size]
}

@Composable
fun InitialsAvatar(name: String, seed: String, size: Dp, modifier: Modifier = Modifier, fontSize: TextUnit = 12.sp) {
    val (container, content) = avatarColors(seed, WakTheme.colors)
    Box(
        modifier = modifier
            .size(size)
            .background(container, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials(name), style = spaceGrotesk(fontSize), color = content)
    }
}
