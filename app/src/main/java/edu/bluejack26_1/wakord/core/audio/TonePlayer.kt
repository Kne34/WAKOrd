package edu.bluejack26_1.wakord.core.audio

interface TonePlayer {
    suspend fun playNote(midi: Int, durationMs: Long = 1000, referencePitch: Float = 440f)

    suspend fun playChord(midis: List<Int>, durationMs: Long = 1500, referencePitch: Float = 440f) {
        midis.forEach { playNote(it, durationMs / midis.size.coerceAtLeast(1), referencePitch) }
    }

    fun stop()
}
