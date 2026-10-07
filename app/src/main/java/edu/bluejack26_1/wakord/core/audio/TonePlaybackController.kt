package edu.bluejack26_1.wakord.core.audio

import edu.bluejack26_1.wakord.core.music.VoicingGenerator
import edu.bluejack26_1.wakord.domain.model.ChordProgression
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TonePlaybackController(
    private val tonePlayer: TonePlayer,
    private val scope: CoroutineScope,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : PlaybackController {
    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var job: Job? = null
    private val midiCache = mutableMapOf<String, List<Int>>()

    override fun play(progression: ChordProgression) {
        stopJob()
        _state.value = PlaybackState(progression = progression, isPlaying = true)
        job = scope.launch {
            val barMs = 60_000L * BEATS_PER_BAR / progression.bpm.coerceAtLeast(1)
            progression.chords.forEachIndexed { index, chord ->
                _state.update { it.copy(chordIndex = index) }
                tonePlayer.playChord(midisFor(chord), barMs)
            }
            _state.update { it.copy(isPlaying = false, chordIndex = null) }
        }
    }

    override fun toggle() {
        val current = _state.value
        val progression = current.progression ?: return
        if (current.isPlaying) pause() else play(progression)
    }

    override fun pause() {
        stopJob()
        _state.update { it.copy(isPlaying = false, chordIndex = null) }
    }

    override fun close() {
        stopJob()
        _state.value = PlaybackState()
    }

    private fun stopJob() {
        job?.cancel()
        job = null
        tonePlayer.stop()
    }

    private suspend fun midisFor(chord: String): List<Int> = midiCache.getOrPut(chord) {
        withContext(computeDispatcher) {
            VoicingGenerator.voicings(chord, limit = 1).firstOrNull()?.let { VoicingGenerator.midiNotes(it) }.orEmpty()
        }
    }

    private companion object {
        const val BEATS_PER_BAR = 4
    }
}
