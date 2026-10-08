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
