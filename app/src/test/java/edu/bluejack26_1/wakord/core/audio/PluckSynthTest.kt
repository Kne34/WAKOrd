package edu.bluejack26_1.wakord.core.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PluckSynthTest {
    private val sampleRate = 44_100

    @Test
    fun `length matches duration`() {
        val samples = PluckSynth.render(listOf(220f), durationMs = 500, sampleRate = sampleRate)
        assertEquals(sampleRate / 2, samples.size)
    }

    @Test
    fun `output stays inside -1 to 1 and is not silent`() {
        val samples = PluckSynth.render(listOf(110f, 165f, 220f, 277f), durationMs = 800, sampleRate = sampleRate)
        val peak = samples.maxOf { abs(it) }
        assertTrue(peak <= 1f)
        assertTrue(peak > 0.5f)
    }

    @Test
    fun `fades out to silence at the end`() {
        val samples = PluckSynth.render(listOf(220f), durationMs = 300, sampleRate = sampleRate)
        assertEquals(0f, samples.last(), 0.01f)
    }

    @Test
    fun `plucked A2 is heard as 110 Hz by the tuner`() {
        val samples = PluckSynth.render(listOf(PluckSynth.frequencyOf(45)), durationMs = 600, sampleRate = sampleRate)
        val window = samples.copyOfRange(sampleRate / 5, sampleRate / 5 + 4096)
        val result = YinPitchDetector().detect(window, sampleRate)
        assertNotNull(result)
        assertEquals(110f, result!!.frequencyHz, 1f)
    }

    @Test
    fun `midi to frequency`() {
        assertEquals(440f, PluckSynth.frequencyOf(69), 0.01f)
        assertEquals(261.63f, PluckSynth.frequencyOf(60), 0.01f)
        assertEquals(442f, PluckSynth.frequencyOf(69, referencePitch = 442f), 0.01f)
    }
}
