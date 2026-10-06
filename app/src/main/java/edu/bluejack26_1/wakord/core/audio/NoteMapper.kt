package edu.bluejack26_1.wakord.core.audio

import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

data class NoteReading(
    val name: String,
    val octave: Int,
    val midi: Int,
    val cents: Float,
    val frequencyHz: Float,
) {
    val label: String get() = "$name$octave"
}

object NoteMapper {
    private val SHARP_NAMES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    private val BASE = mapOf('C' to 0, 'D' to 2, 'E' to 4, 'F' to 5, 'G' to 7, 'A' to 9, 'B' to 11)
    private val NOTE_REGEX = Regex("^([A-Ga-g])([#b]?)(-?\\d)$")

    fun fromFrequency(frequencyHz: Float, referencePitch: Float = 440f): NoteReading {
        val exact = 69 + 12 * log2(frequencyHz / referencePitch)
        val midi = exact.roundToInt()
        return NoteReading(
            name = nameOf(midi),
            octave = octaveOf(midi),
            midi = midi,
            cents = (exact - midi) * 100f,
            frequencyHz = frequencyHz,
        )
    }

    fun frequencyOf(midi: Int, referencePitch: Float = 440f): Float =
        (referencePitch * 2.0.pow((midi - 69) / 12.0)).toFloat()

    fun centsBetween(frequencyHz: Float, targetHz: Float): Float =
        1200f * log2(frequencyHz / targetHz)

    fun nameOf(midi: Int): String = SHARP_NAMES[Math.floorMod(midi, 12)]

    fun octaveOf(midi: Int): Int = Math.floorDiv(midi, 12) - 1

    fun labelOf(midi: Int): String = "${nameOf(midi)}${octaveOf(midi)}"

    fun midiOf(note: String): Int {
        val match = requireNotNull(NOTE_REGEX.matchEntire(note.trim())) { "Format nada tidak valid: $note" }
        val (letter, accidental, octave) = match.destructured
        val semitone = BASE.getValue(letter.uppercase()[0]) + when (accidental) {
            "#" -> 1
            "b" -> -1
            else -> 0
        }
        return (octave.toInt() + 1) * 12 + semitone
    }

    fun pretty(note: String): String = note.replace("#", "♯")
}
