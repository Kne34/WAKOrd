package edu.bluejack26_1.wakord.testutil

import edu.bluejack26_1.wakord.core.audio.TonePlayer

class RecordingTonePlayer : TonePlayer {
    val chords = mutableListOf<List<Int>>()
    var stopped = 0

    override suspend fun playNote(midi: Int, durationMs: Long, referencePitch: Float) {
        chords += listOf(midi)
    }

    override suspend fun playChord(midis: List<Int>, durationMs: Long, referencePitch: Float) {
        chords += midis
    }

    override fun stop() {
        stopped++
    }
}
