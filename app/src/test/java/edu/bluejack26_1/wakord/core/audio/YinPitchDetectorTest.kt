package edu.bluejack26_1.wakord.core.audio

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.sin

class YinPitchDetectorTest {

    private val detector = YinPitchDetector()

    @Test
    fun `detects bass low E1`() = assertDetects(41.20f)

    @Test
    fun `detects guitar low E2`() = assertDetects(82.41f)

    @Test
    fun `detects guitar A2 from the figma example`() = assertDetects(109.9f)

    @Test
    fun `detects ukulele G4`() = assertDetects(392.0f)

    @Test
    fun `detects A4 reference`() = assertDetects(440.0f)

    @Test
    fun `detects violin and mandolin high E5`() = assertDetects(659.26f)

    @Test
    fun `works at 48 kHz sample rate`() = assertDetects(110.0f, sampleRate = 48_000)

    @Test
    fun `detects guitar G3 with harmonics`() = assertDetects(196.0f, harmonics = true)

    @Test
    fun `silence returns null`() {
        assertNull(detector.detect(FloatArray(4096), 44_100))
    }

    @Test
    fun `very quiet noise returns null`() {
        val noise = FloatArray(4096) { ((it * 7919) % 13 - 6) / 6000f }
        assertNull(detector.detect(noise, 44_100))
    }

    private fun assertDetects(frequency: Float, sampleRate: Int = 44_100, harmonics: Boolean = false) {
        val result = detector.detect(sine(frequency, sampleRate, harmonics), sampleRate)
        assertNotNull("no pitch for $frequency Hz", result)
        val centsOff = abs(1200 * log2(result!!.frequencyHz / frequency))
        assertTrue("$frequency Hz detected as ${result.frequencyHz} Hz ($centsOff cents off)", centsOff < 2f)
        assertTrue("low confidence for $frequency Hz", result.probability > 0.8f)
    }

    private fun sine(frequency: Float, sampleRate: Int, harmonics: Boolean, size: Int = 4096) = FloatArray(size) { i ->
        val t = i.toDouble() / sampleRate
        var v = sin(2 * PI * frequency * t)
        if (harmonics) v = 0.6 * v + 0.3 * sin(4 * PI * frequency * t) + 0.1 * sin(6 * PI * frequency * t)
        (0.5 * v).toFloat()
    }
}
