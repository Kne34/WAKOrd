package edu.bluejack26_1.wakord.data.remote

import edu.bluejack26_1.wakord.domain.model.AnalysisSource
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GeminiAnalysisParserTest {
    @Test
    fun `parses chords, key and tempo from the model reply`() {
        val json = """
            ```json
            {"key":"A minor","confidence":0.92,"bpm":96,
             "chords":[{"symbol":"Am","startMs":0,"endMs":2000},{"symbol":"F♯m7","startMs":2000,"endMs":4000},
                       {"symbol":"not a chord","startMs":4000,"endMs":5000},{"symbol":"G","startMs":5000,"endMs":99000}]}
            ```
        """.trimIndent()
        val result = GeminiAnalysisParser.parse(json, "id", 7L, 12_000)
        assertEquals(listOf("Am", "F#m7", "G"), result.chords.map { it.symbol })
        assertEquals(12_000L, result.chords.last().endMs)
        assertEquals("Am", result.key)
        assertEquals(0.92f, result.confidence)
        assertEquals(96, result.bpm)
        assertEquals(AnalysisSource.AI, result.source)
    }

    @Test
    fun `missing fields use safe defaults`() {
        val result = GeminiAnalysisParser.parse("{\"key\":\"Eb\"}", "id", 0L, 5_000)
        assertEquals("Eb", result.key)
        assertEquals(90, result.bpm)
        assertEquals(0, result.chords.size)
    }

    @Test
    fun `wav header describes 16 bit mono audio`() {
        val wav = WavEncoder.encode(floatArrayOf(0f, 1f, -1f), 16_000)
        assertEquals(44 + 6, wav.size)
        assertEquals("RIFF", String(wav, 0, 4))
        assertEquals("WAVE", String(wav, 8, 4))
        val buffer = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(16_000, buffer.getInt(24))
        assertEquals(Short.MAX_VALUE, buffer.getShort(46))
    }
}
