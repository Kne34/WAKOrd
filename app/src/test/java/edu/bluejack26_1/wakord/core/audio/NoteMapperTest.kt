package edu.bluejack26_1.wakord.core.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteMapperTest {

    @Test
    fun `440 Hz is A4 with zero cents`() {
        val reading = NoteMapper.fromFrequency(440f)
        assertEquals("A", reading.name)
        assertEquals(4, reading.octave)
        assertEquals(69, reading.midi)
        assertEquals(0f, reading.cents, 0.01f)
    }

    @Test
    fun `82_41 Hz is E2`() {
        assertEquals("E2", NoteMapper.fromFrequency(82.41f).label)
    }

    @Test
    fun `445 Hz is A4 about 20 cents sharp`() {
        assertEquals(19.56f, NoteMapper.fromFrequency(445f).cents, 0.1f)
    }

    @Test
    fun `reference pitch 442 shifts the reading`() {
        val reading = NoteMapper.fromFrequency(442f, referencePitch = 442f)
        assertEquals("A4", reading.label)
        assertEquals(0f, reading.cents, 0.01f)
    }

    @Test
    fun `parses note names to midi`() {
        assertEquals(40, NoteMapper.midiOf("E2"))
        assertEquals(58, NoteMapper.midiOf("A#3"))
        assertEquals(58, NoteMapper.midiOf("Bb3"))
        assertEquals(60, NoteMapper.midiOf("C4"))
    }

    @Test
    fun `frequency and label round trip`() {
        assertEquals(110f, NoteMapper.frequencyOf(NoteMapper.midiOf("A2")), 0.01f)
        assertEquals("D#3", NoteMapper.labelOf(51))
    }

    @Test
    fun `pretty uses sharp sign`() {
        assertEquals("F♯m7", NoteMapper.pretty("F#m7"))
    }
}
