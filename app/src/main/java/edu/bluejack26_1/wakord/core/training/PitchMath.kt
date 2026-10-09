package edu.bluejack26_1.wakord.core.training

import edu.bluejack26_1.wakord.domain.model.PitchReading
import edu.bluejack26_1.wakord.domain.model.PitchStatus
import kotlin.math.log2
import kotlin.math.roundToInt

object PitchMath {
    private const val ON_PITCH_CENTS = 25

    fun reading(frequencyHz: Float?, targetMidi: Int, referencePitch: Float = 440f): PitchReading {
        val target = QuestionGenerator.noteId(targetMidi)
        if (frequencyHz == null || frequencyHz <= 0f) {
            return PitchReading(target, null, null, null, PitchStatus.NO_SIGNAL)
        }
        val midi = 69 + 12 * log2(frequencyHz / referencePitch)
        val cents = ((midi - targetMidi) * 100).roundToInt()
        val status = when {
            cents < -ON_PITCH_CENTS -> PitchStatus.FLAT
            cents > ON_PITCH_CENTS -> PitchStatus.SHARP
            else -> PitchStatus.ON_PITCH
        }
        return PitchReading(target, QuestionGenerator.noteId(midi.roundToInt()), frequencyHz, cents, status)
    }
}
