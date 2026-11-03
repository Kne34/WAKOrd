package edu.bluejack26_1.wakord.core.analysis

import edu.bluejack26_1.wakord.core.error.AppError
import edu.bluejack26_1.wakord.data.fake.FakeChordAnalyzer
import edu.bluejack26_1.wakord.data.fake.SampleAnalysis
import edu.bluejack26_1.wakord.data.fake.SampleDiscover
import edu.bluejack26_1.wakord.domain.model.AnalysisSource
import edu.bluejack26_1.wakord.domain.model.Catalog
import edu.bluejack26_1.wakord.domain.model.DetectedChord
import edu.bluejack26_1.wakord.domain.model.Difficulty
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

class ChordAnalysisTest {
    private val rate = 16_000

    private fun hz(midi: Int) = 440.0 * 2.0.pow((midi - 69) / 12.0)

    private fun tone(midis: List<Int>, seconds: Double, amplitude: Double = 0.2): FloatArray {
        val n = (rate * seconds).toInt()
        return FloatArray(n) { i ->
            midis.sumOf { m -> amplitude * sin(2 * PI * hz(m) * i / rate) }.toFloat()
        }
    }

    @Test
    fun `names a chord from piano keys and prefers the lowest key as root`() {
        assertEquals("C", ChordRecognizer.fromNotes(listOf(60, 64, 67))?.symbol)
        assertEquals("Am7", ChordRecognizer.fromNotes(listOf(57, 60, 64, 67))?.symbol)
        assertEquals("C6", ChordRecognizer.fromNotes(listOf(60, 64, 67, 69))?.symbol)
        assertEquals("Em", ChordRecognizer.fromNotes(listOf(52, 55, 59, 64))?.symbol)
    }

    @Test
    fun `names chords built from five or more keys`() {
        val cmaj9 = ChordRecognizer.fromNotes(listOf(48, 52, 55, 59, 62))!!
        assertEquals("Cmaj9", cmaj9.symbol)
        assertEquals(listOf("R", "M3", "P5", "M7", "9"), cmaj9.intervals)
        assertEquals("Dm9", ChordRecognizer.fromNotes(listOf(50, 53, 57, 60, 64))?.symbol)
        assertEquals("G9", ChordRecognizer.fromNotes(listOf(43, 47, 50, 53, 57, 67))?.symbol)
        assertEquals("C", ChordRecognizer.fromNotes(listOf(48, 52, 55, 60, 64, 67, 72))?.symbol)
    }

    @Test
    fun `describes a chord from its symbol`() {
        val reading = ChordRecognizer.readingOf("Am7")
        assertEquals("Am7", reading?.symbol)
        assertEquals("minor", reading?.quality)
        assertEquals(listOf("A", "C", "E", "G"), reading?.notes)
        assertEquals("Bbmaj7", ChordRecognizer.readingOf("Bbmaj7")?.symbol)
        assertNull(ChordRecognizer.readingOf("nope"))
    }

    @Test
    fun `needs at least three different notes to name a chord`() {
        assertNull(ChordRecognizer.fromNotes(listOf(60, 64)))
        assertNull(ChordRecognizer.fromNotes(listOf(60, 64, 72, 76)))
    }

    @Test
    fun `recognizes triads and seventh chords from sound`() {
        assertEquals("C", ChordRecognizer.recognize(tone(listOf(48, 60, 64, 67), 0.3), rate)?.symbol)
        assertEquals("Am", ChordRecognizer.recognize(tone(listOf(57, 60, 64, 69), 0.3), rate)?.symbol)
        val g7 = ChordRecognizer.recognize(tone(listOf(55, 59, 62, 65), 0.3), rate)!!
        assertEquals("G7", g7.symbol)
        assertEquals(listOf("G", "B", "D", "F"), g7.notes)
        assertEquals(listOf("R", "M3", "P5", "m7"), g7.intervals)
        assertEquals("major", g7.quality)
        assertEquals(listOf("7"), g7.extensions)
        assertTrue(g7.matchPercent > 80)
        assertTrue(g7.alternative != null && g7.alternative != "G7")
    }

    @Test
    fun `silence gives no chord`() {
        assertNull(ChordRecognizer.recognize(FloatArray(4096), rate))
    }

    @Test
    fun `key detection finds major and minor keys`() {
        val cMajor = FloatArray(12).also { c -> listOf(0, 4, 7, 0, 5, 9, 7, 11, 2).forEach { c[it] += 1f } }
        assertEquals("C", KeyDetector.detect(cMajor).name)
        val aMinor = FloatArray(12).also { c -> listOf(9, 0, 4, 9, 2, 5, 4, 8, 11, 9).forEach { c[it] += 1f } }
        assertEquals("Am", KeyDetector.detect(aMinor).name)
    }

    @Test
    fun `tempo follows a steady click`() {
        val samples = FloatArray(rate * 8)
        var t = 0
        while (t < samples.size) {
            for (i in 0 until 400) if (t + i < samples.size) samples[t + i] = 0.8f * sin(2 * PI * 1000 * i / rate).toFloat()
            t += rate / 2
        }
        assertTrue(abs(TempoEstimator.estimate(samples, rate) - 120) <= 3)
    }

    @Test
    fun `algorithm finds the chord sequence and key of a recording`() = runTest {
        val samples = tone(listOf(48, 60, 64, 67), 2.0) +
            tone(listOf(45, 57, 60, 64), 2.0) +
            tone(listOf(41, 57, 60, 65), 2.0) +
            tone(listOf(43, 55, 59, 62), 2.0)
        val result = AlgorithmChordAnalyzer { 42L }.analyze(samples, rate).getOrThrow()
        assertEquals(listOf("C", "Am", "F", "G"), result.chords.map { it.symbol })
        assertEquals("C", result.key)
        assertEquals(AnalysisSource.ALGORITHM, result.source)
        assertEquals(8_000L, result.durationMs)
        assertEquals(42L, result.createdAt)
    }

    @Test
    fun `algorithm reports nothing for silence`() = runTest {
        val result = AlgorithmChordAnalyzer().analyze(FloatArray(rate * 2), rate)
        assertEquals(AppError.NoChordsHeard, result.exceptionOrNull())
    }

    @Test
    fun `fallback runs only when the ai fails`() = runTest {
        val algorithm = FakeChordAnalyzer(Result.success(SampleAnalysis.result.copy(source = AnalysisSource.ALGORITHM)))
        val failing = FallbackChordAnalyzer(FakeChordAnalyzer(Result.failure(AppError.Network)), algorithm)
        assertEquals(AnalysisSource.ALGORITHM, failing.analyze(FloatArray(10), rate).getOrThrow().source)
        val empty = FallbackChordAnalyzer(FakeChordAnalyzer(Result.success(SampleAnalysis.result.copy(chords = emptyList()))), algorithm)
        assertEquals(AppError.NoChordsHeard, empty.analyze(FloatArray(10), rate).exceptionOrNull())
        val working = FallbackChordAnalyzer(FakeChordAnalyzer(), algorithm)
        assertEquals(AnalysisSource.AI, working.analyze(FloatArray(10), rate).getOrThrow().source)
    }

    @Test
    fun `cleaner removes offset, normalizes and gates noise`() {
        val cleaned = AudioCleaner.clean(floatArrayOf(0.5f, 0.501f, 0.9f, 0.1f))
        assertEquals(1f, cleaned.maxOf { abs(it) })
        assertEquals(0f, cleaned[1])
    }

    @Test
    fun `roman numerals follow the key`() {
        assertEquals("V7", RomanNumerals.of("G7", "C"))
        assertEquals("vi", RomanNumerals.of("Am", "C"))
        assertEquals("vii°", RomanNumerals.of("Bdim", "C"))
        assertEquals("viiø7", RomanNumerals.of("Bm7b5", "C"))
        assertEquals("IVmaj7", RomanNumerals.of("Fmaj7", "C"))
        assertEquals("III", RomanNumerals.of("C", "Am"))
        assertEquals("V", RomanNumerals.of("E", "Am"))
        assertEquals("bVII", RomanNumerals.of("Bb", "C"))
    }

    @Test
    fun `chord track splits long chords into bars`() {
        val result = SampleAnalysis.result.copy(bpm = 120)
        val track = AnalysisInsights.chordTrack(result)
        assertEquals(listOf("C", "C", "Am", "Am", "F", "F", "G", "G"), track.map { it.symbol })
        assertEquals(listOf("I", "I", "vi", "vi", "IV", "IV", "V", "V"), track.map { it.roman })
    }

    @Test
    fun `difficulty grows with chord complexity`() {
        assertEquals(Difficulty.EASY, AnalysisInsights.difficulty(listOf("C", "G", "Am")))
        assertEquals(Difficulty.INTERMEDIATE, AnalysisInsights.difficulty(listOf("Cmaj7", "G7")))
        assertEquals(Difficulty.ADVANCED, AnalysisInsights.difficulty(listOf("Dm9", "G13")))
    }

    @Test
    fun `recommends progressions and songs that share the chords`() {
        val result = SampleAnalysis.result.copy(
            chords = listOf("Cmaj7", "Am7", "Dm7", "G7").mapIndexed { i, s -> DetectedChord(s, i * 1000L, i * 1000L + 1000) },
        )
        val recommendations = AnalysisInsights.recommend(result, Catalog(SampleDiscover.songs, SampleDiscover.progressions))
        assertEquals(5, recommendations.progressions.size)
        assertTrue(recommendations.songs.isEmpty())

        val eb = SampleAnalysis.result.copy(
            key = "Eb",
            chords = listOf("Ebmaj7", "Cm7").mapIndexed { i, s -> DetectedChord(s, i * 1000L, i * 1000L + 1000) },
        )
        val songs = AnalysisInsights.recommend(eb, Catalog(SampleDiscover.songs, emptyList()))
        assertEquals(listOf("subtle-resonance"), songs.songs.map { it.id })
        assertEquals(listOf("Alex Vance ft. Koro"), songs.artists)
    }
}
