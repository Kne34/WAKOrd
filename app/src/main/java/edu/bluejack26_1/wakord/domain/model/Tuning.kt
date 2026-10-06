package edu.bluejack26_1.wakord.domain.model

import edu.bluejack26_1.wakord.core.audio.NoteMapper
import kotlin.math.abs

enum class TunerInstrument(val stringCount: Int, val isPaired: Boolean = false) {
    GUITAR(6),
    UKULELE(4),
    BASS(4),
    MANDOLIN(4, isPaired = true),
    VIOLIN(4),
}

data class Tuning(
    val id: String,
    val name: String,
    val instrument: TunerInstrument,
    val notes: List<String>,
    val isCustom: Boolean = false,
    val shortName: String = name,
)

object TuningPresets {
    val all = listOf(
        Tuning("guitar_standard", "Standard E", TunerInstrument.GUITAR, listOf("E2", "A2", "D3", "G3", "B3", "E4"), shortName = "Standard"),
        Tuning("guitar_drop_d", "Drop D", TunerInstrument.GUITAR, listOf("D2", "A2", "D3", "G3", "B3", "E4")),
        Tuning("guitar_dadgad", "DADGAD", TunerInstrument.GUITAR, listOf("D2", "A2", "D3", "G3", "A3", "D4")),
        Tuning("guitar_open_g", "Open G", TunerInstrument.GUITAR, listOf("D2", "G2", "D3", "G3", "B3", "D4")),
        Tuning("ukulele_standard", "Standard GCEA", TunerInstrument.UKULELE, listOf("G4", "C4", "E4", "A4"), shortName = "Standard"),
        Tuning("bass_standard", "Standard EADG", TunerInstrument.BASS, listOf("E1", "A1", "D2", "G2"), shortName = "Standard"),
        Tuning("mandolin_standard", "Standard GDAE", TunerInstrument.MANDOLIN, listOf("G3", "D4", "A4", "E5"), shortName = "Standard"),
        Tuning("violin_standard", "Standard GDAE", TunerInstrument.VIOLIN, listOf("G3", "D4", "A4", "E5"), shortName = "Standard"),
    )

    fun forInstrument(instrument: TunerInstrument): List<Tuning> = all.filter { it.instrument == instrument }

    fun default(instrument: TunerInstrument): Tuning = forInstrument(instrument).first()
}

enum class TuneStatus { IDLE, FLAT, IN_TUNE, SHARP }

object StringMatcher {
    const val IN_TUNE_CENTS = 5f

    fun closestString(frequencyHz: Float, tuning: Tuning, referencePitch: Float = 440f): Int =
        tuning.notes.indices.minBy { abs(centsToString(frequencyHz, tuning, it, referencePitch)) }

    fun centsToString(frequencyHz: Float, tuning: Tuning, index: Int, referencePitch: Float = 440f): Float {
        val target = NoteMapper.frequencyOf(NoteMapper.midiOf(tuning.notes[index]), referencePitch)
        return NoteMapper.centsBetween(frequencyHz, target)
    }

    fun statusOf(cents: Float): TuneStatus = when {
        abs(cents) <= IN_TUNE_CENTS -> TuneStatus.IN_TUNE
        cents < 0 -> TuneStatus.FLAT
        else -> TuneStatus.SHARP
    }
}
