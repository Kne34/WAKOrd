package edu.bluejack26_1.wakord.core.music

import edu.bluejack26_1.wakord.data.fake.SampleMusic
import edu.bluejack26_1.wakord.domain.model.ChordSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChordEngineTest {
    @Test
    fun `parses root, suffix and bass`() {
        assertEquals(Chord("Bb", "7sus4"), Chord.parse("B♭7sus4"))
        assertEquals(Chord("C", "", "G"), Chord.parse("C/G"))
        assertEquals(Chord("F#", "m7(9)"), Chord.parse("F♯m7(9)"))
        assertNull(Chord.parse("H7"))
    }

    @Test
    fun `chord tones`() {
        assertEquals(setOf(0, 4, 7), ChordTheory.pitchClasses(Chord.parse("C")!!))
        assertEquals(setOf(2, 5, 9, 0, 4), ChordTheory.pitchClasses(Chord.parse("Dm9")!!))
    }

    @Test
    fun `pretty uses music symbols`() {
        assertEquals("B♭7sus4", NoteNames.pretty("Bb7sus4"))
        assertEquals("F♯m7(9)", NoteNames.pretty("F#m7(9)"))
    }

    @Test
    fun `Eb up two is F`() {
        assertEquals("F", ChordTransposer.transposeKey("Eb", 2))
        assertEquals("C7sus4", ChordTransposer.transposeChord("Bb7sus4", 2, preferFlats = false))
    }

    @Test
    fun `spelling follows the new key`() {
        assertEquals("Db", ChordTransposer.transposeKey("C", 1))
        assertEquals("Ab/Eb", ChordTransposer.transposeChord("G/D", 1, preferFlats = true))
        assertEquals("F#m", ChordTransposer.transposeKey("Fm", 1))
        assertEquals("Bbm", ChordTransposer.transposeKey("Am", 1))
    }

    @Test
    fun `twelve semitones returns the same key`() {
        assertEquals("E", ChordTransposer.transposeKey("E", 12))
        assertEquals(SampleMusic.song, ChordTransposer.transposeSong(SampleMusic.song, -12))
    }

    @Test
    fun `whole song is transposed`() {
        val song = ChordTransposer.transposeSong(SampleMusic.song, 2)
        assertEquals("F", song.key)
        val chords = song.sections.flatMap { s -> s.lines.flatMap { l -> l.segments.mapNotNull { it.chord } } }
        assertEquals(listOf("Fmaj7", "C7sus4", "Dm7", "Bbmaj7", "Fmaj7", "C7sus4", "Dm7", "Bbmaj7"), chords)
    }

    @Test
    fun `parses chordpro line`() {
        val line = ChordProParser.parseLine("Hi [C]there [G]friend")
        assertEquals(
            listOf(ChordSegment(null, "Hi "), ChordSegment("C", "there "), ChordSegment("G", "friend")),
            line.segments,
        )
        assertEquals("Hi [C]there [G]friend", ChordProParser.toChordPro(line))
    }

    @Test
    fun `chord only line keeps every chord`() {
        val chords = ChordProParser.parseLine("[G] [D] [Em] [C]").segments.map { it.chord }
        assertEquals(listOf("G", "D", "Em", "C"), chords)
    }

    @Test
    fun `open C is x32010`() {
        val open = VoicingGenerator.voicings("C").first()
        assertEquals(0, open.position)
        assertEquals(listOf(-1, 3, 2, 0, 1, 0), open.frets)
        assertEquals(listOf(0, 3, 2, 0, 1, 0), open.fingers)
    }

    @Test
    fun `open G and Am`() {
        assertEquals(listOf(3, 2, 0, 0, 0, 3), VoicingGenerator.voicings("G").first().frets)
        assertEquals(listOf(-1, 0, 2, 2, 1, 0), VoicingGenerator.voicings("Am").first().frets)
    }

    @Test
    fun `every voicing is playable and correct`() {
        listOf("C", "G", "Am", "F", "Dm9", "G13", "Cmaj9", "Bb7sus4", "C/G", "F#m7(9)").forEach { symbol ->
            val chord = Chord.parse(symbol)!!
            val tones = ChordTheory.pitchClasses(chord)
            val bass = NoteNames.pitchClass(chord.bass ?: chord.root)
            val voicings = VoicingGenerator.voicings(symbol)
            assertTrue("no voicing for $symbol", voicings.isNotEmpty())
            voicings.forEach { v ->
                val notes = VoicingGenerator.midiNotes(v)
                assertEquals("bass of $symbol ${v.frets}", bass, notes.min() % 12)
                assertTrue("wrong note in $symbol ${v.frets}", notes.all { it % 12 in tones })
                assertTrue("too many fingers in $symbol ${v.frets}", (v.fingers.maxOrNull() ?: 0) <= 4)
                assertTrue("fret span too wide in $symbol ${v.frets}", v.frets.filter { it > 0 }.let { f -> f.isEmpty() || f.max() - f.min() <= 3 })
            }
        }
    }

    @Test
    fun `piano notes start from the root in the third octave and stay on the keyboard`() {
        assertEquals(listOf(48, 52, 55), VoicingGenerator.pianoNotes("C"))
        assertEquals(listOf(54, 57, 61, 64), VoicingGenerator.pianoNotes("F#m7"))
        assertEquals(listOf(59, 64, 68, 71), VoicingGenerator.pianoNotes("E/B"))
        assertTrue(VoicingGenerator.pianoNotes("Badd9").all { it in 48..71 })
        assertTrue(VoicingGenerator.pianoNotes("nope").isEmpty())
    }

    @Test
    fun `ukulele C is 0003`() {
        val c = VoicingGenerator.voicings("C", VoicingGenerator.UKULELE_STANDARD).first()
        assertEquals(listOf(0, 0, 0, 3), c.frets)
    }
}
