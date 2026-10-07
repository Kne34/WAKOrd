package edu.bluejack26_1.wakord.core.audio

import edu.bluejack26_1.wakord.domain.model.ChordProgression
import kotlinx.coroutines.flow.StateFlow

data class PlaybackState(
    val progression: ChordProgression? = null,
    val isPlaying: Boolean = false,
    val chordIndex: Int? = null,
)

interface PlaybackController {
    val state: StateFlow<PlaybackState>
    fun play(progression: ChordProgression)
    fun toggle()
    fun pause()
    fun close()
}
