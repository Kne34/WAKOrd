package edu.bluejack26_1.wakord.ui.feature.song

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import edu.bluejack26_1.wakord.R
import java.util.Locale

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

private val WideCovers = mapOf(
    "subtle-resonance" to R.drawable.wide_subtle_resonance,
    "golden-hour-glow" to R.drawable.wide_golden_hour_glow,
    "midnight-waves" to R.drawable.wide_midnight_waves,
)

private val CoverOrder = Covers.keys.toList()

private fun fallbackKey(id: String) = CoverOrder[Math.floorMod(id.hashCode(), CoverOrder.size)]

@DrawableRes
fun songCover(id: String): Int = Covers[id] ?: Covers.getValue(fallbackKey(id))

@DrawableRes
fun songWideCover(id: String): Int = WideCovers[id] ?: WideCovers.getValue(fallbackKey(id))

@DrawableRes
fun songBackground(id: String): Int = Backgrounds[id] ?: Backgrounds.getValue(fallbackKey(id))

fun formatClock(ms: Long): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)
}

fun signedSteps(steps: Int): String = if (steps > 0) "+$steps" else steps.toString()

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
