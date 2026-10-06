package edu.bluejack26_1.wakord.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class StringMatcherTest {

    private val standard = TuningPresets.default(TunerInstrument.GUITAR)
    private val dropD = TuningPresets.forInstrument(TunerInstrument.GUITAR).first { it.name == "Drop D" }

    @Test
    fun `auto detects A string in standard tuning`() {
        assertEquals(1, StringMatcher.closestString(109.9f, standard))
    }

    @Test
    fun `auto detects low D in drop D`() {
        assertEquals(0, StringMatcher.closestString(73.42f, dropD))
    }

    @Test
    fun `cents to target string`() {
        assertEquals(-14f, StringMatcher.centsToString(145.6f, standard, 2), 1f)
    }

    @Test
    fun `status thresholds`() {
        assertEquals(TuneStatus.IN_TUNE, StringMatcher.statusOf(-4.9f))
        assertEquals(TuneStatus.FLAT, StringMatcher.statusOf(-14f))
        assertEquals(TuneStatus.SHARP, StringMatcher.statusOf(12f))
    }

    @Test
    fun `every preset has the right string count`() {
        TuningPresets.all.forEach { tuning ->
            assertEquals(tuning.name, tuning.instrument.stringCount, tuning.notes.size)
        }
    }

    @Test
    fun `only mandolin uses paired strings`() {
        assertEquals(listOf(TunerInstrument.MANDOLIN), TunerInstrument.entries.filter { it.isPaired })
    }

    @Test
    fun `standard presets use a short label`() {
        assertEquals("Standard", TuningPresets.default(TunerInstrument.GUITAR).shortName)
        assertEquals("Standard E", TuningPresets.default(TunerInstrument.GUITAR).name)
        assertEquals("Drop D", dropD.shortName)
    }
}
