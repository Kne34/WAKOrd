package edu.bluejack26_1.wakord.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.core.music.NoteNames
import edu.bluejack26_1.wakord.domain.model.Difficulty
import edu.bluejack26_1.wakord.ui.theme.WakColors

fun prettyChord(symbol: String): String = NoteNames.pretty(symbol)

@Composable
fun keyLabel(key: String): String {
    val isMinor = key.endsWith("m") && !key.endsWith("maj")
    val root = prettyChord(if (isMinor) key.dropLast(1) else key)
    return stringResource(if (isMinor) R.string.key_minor else R.string.key_major, root)
}

@Composable
fun levelLabel(difficulty: Difficulty, short: Boolean = false): String = stringResource(
    when (difficulty) {
        Difficulty.EASY -> if (short) R.string.level_easy else R.string.explore_level_easy
        Difficulty.INTERMEDIATE -> if (short) R.string.level_intermediate else R.string.explore_level_intermediate
        Difficulty.ADVANCED -> if (short) R.string.level_advanced else R.string.explore_level_advanced
    },
)

fun levelContainer(difficulty: Difficulty, c: WakColors): Color = when (difficulty) {
    Difficulty.EASY -> c.primary.copy(alpha = 0.12f)
    Difficulty.INTERMEDIATE -> c.lavender.copy(alpha = 0.14f)
    Difficulty.ADVANCED -> alertContainer(c)
}

fun levelColor(difficulty: Difficulty, c: WakColors): Color = when (difficulty) {
    Difficulty.EASY -> c.primaryText
    Difficulty.INTERMEDIATE -> c.lavender
    Difficulty.ADVANCED -> c.peachText
}
