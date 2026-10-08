package edu.bluejack26_1.wakord.ui.feature.player

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.bluejack26_1.wakord.ui.components.MiniPlayer
import edu.bluejack26_1.wakord.ui.components.prettyChord
import edu.bluejack26_1.wakord.ui.feature.progression.progressionCover

@Composable
fun MiniPlayerHost(
    visible: Boolean,
    onOpenProgression: (String) -> Unit,
    viewModel: MiniPlayerViewModel = viewModel(factory = MiniPlayerViewModel.Factory),
) {
    val playback by viewModel.state.collectAsStateWithLifecycle()
    val progression = playback.progression
    if (!visible || progression == null || progression.chords.isEmpty()) return
    val index = (playback.chordIndex ?: 0).coerceIn(progression.chords.indices)
    val upcoming = progression.chords.drop(index + 1).take(2).joinToString("") { " → ${prettyChord(it)}" }
    val detail = (upcoming.trim() + " · " + progression.authorName).trimStart(' ', '·')
    MiniPlayer(
        title = progression.title,
        currentChord = prettyChord(progression.chords[index]),
        detail = detail,
        isPlaying = playback.isPlaying,
        progress = (index + 1f) / progression.chords.size,
        onTogglePlay = { viewModel.onAction(MiniPlayerAction.TogglePlay) },
        onClick = { onOpenProgression(progression.id) },
        onClose = { viewModel.onAction(MiniPlayerAction.Close) },
        cover = painterResource(progressionCover(progression.id)),
        modifier = Modifier.padding(bottom = 8.dp),
    )
}
