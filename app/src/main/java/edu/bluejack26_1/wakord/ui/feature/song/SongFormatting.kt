package edu.bluejack26_1.wakord.ui.feature.song

import androidx.annotation.DrawableRes
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
import edu.bluejack26_1.wakord.core.music.NoteNames
import edu.bluejack26_1.wakord.domain.model.Difficulty
import edu.bluejack26_1.wakord.ui.theme.WakColors
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.spaceGrotesk
import java.util.Locale
import kotlin.math.abs

private val Covers = mapOf(
    "subtle-resonance" to R.drawable.cover_subtle_resonance,
    "golden-hour-glow" to R.drawable.cover_golden_hour_glow,
    "midnight-waves" to R.drawable.cover_midnight_waves,
)

private val Backgrounds = mapOf(
    "subtle-resonance" to R.drawable.bg_song_subtle_resonance,
    "golden-hour-glow" to R.drawable.bg_song_golden_hour_glow,
    "midnight-waves" to R.drawable.bg_song_midnight_waves,
)

private val CoverOrder = Covers.keys.toList()

private fun fallbackKey(id: String) = CoverOrder[Math.floorMod(id.hashCode(), CoverOrder.size)]

@DrawableRes
fun songCover(id: String): Int = Covers[id] ?: Covers.getValue(fallbackKey(id))

@DrawableRes
fun songBackground(id: String): Int = Backgrounds[id] ?: Backgrounds.getValue(fallbackKey(id))

fun compactCount(value: Int): String = when {
    value < 1_000 -> value.toString()
    value < 1_000_000 -> trimmed(value / 1_000f) + "k"
    else -> trimmed(value / 1_000_000f) + "M"
}

private fun trimmed(value: Float): String {
    val text = String.format(Locale.US, "%.1f", value)
    return if (text.endsWith(".0")) text.dropLast(2) else text
}

fun formatClock(ms: Long): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)
}

fun prettyChord(symbol: String): String = NoteNames.pretty(symbol)

fun signedSteps(steps: Int): String = if (steps > 0) "+$steps" else steps.toString()

@Composable
fun keyLabel(key: String): String {
    val isMinor = key.endsWith("m") && !key.endsWith("maj")
    val root = prettyChord(if (isMinor) key.dropLast(1) else key)
    return stringResource(if (isMinor) R.string.key_minor else R.string.key_major, root)
}

@Composable
fun capoLabel(capo: Int): String {
    if (capo <= 0) return stringResource(R.string.song_capo_none)
    val res = when {
        capo % 100 in 11..13 -> R.string.ordinal_th
        capo % 10 == 1 -> R.string.ordinal_st
        capo % 10 == 2 -> R.string.ordinal_nd
        capo % 10 == 3 -> R.string.ordinal_rd
        else -> R.string.ordinal_th
    }
    return stringResource(res, capo)
}

@Composable
fun levelLabel(difficulty: Difficulty): String = stringResource(
    when (difficulty) {
        Difficulty.EASY -> R.string.level_easy
        Difficulty.INTERMEDIATE -> R.string.level_intermediate
        Difficulty.ADVANCED -> R.string.level_advanced
    },
)

fun levelColor(difficulty: Difficulty, c: WakColors): Color = when (difficulty) {
    Difficulty.EASY -> c.primaryText
    Difficulty.INTERMEDIATE -> c.lavender
    Difficulty.ADVANCED -> c.peachText
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

val AvatarSmall = 32.dp
val AvatarLarge = 38.dp
