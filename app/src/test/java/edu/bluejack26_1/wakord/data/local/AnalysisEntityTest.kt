package edu.bluejack26_1.wakord.data.local

import edu.bluejack26_1.wakord.data.fake.SampleAnalysis
import edu.bluejack26_1.wakord.domain.model.AnalysisSource
import edu.bluejack26_1.wakord.domain.model.DetectedChord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AnalysisEntityTest {
    @Test
    fun `analysis survives a round trip through the database row`() {
        val entity = AnalysisEntity.from(SampleAnalysis.result, "u1", synced = false)
        assertEquals("C|0|3000;Am|3000|6000;F|6000|9000;G|9000|12000", entity.chords)
        assertFalse(entity.synced)
        assertEquals(SampleAnalysis.result, entity.toDomain())
    }

    @Test
    fun `slash chords and broken rows are handled`() {
        val chords = listOf(DetectedChord("C/G", 0, 500), DetectedChord("F#m7b5", 500, 900))
        assertEquals(chords, AnalysisEntity.decodeChords(AnalysisEntity.encodeChords(chords)))
        assertEquals(emptyList<DetectedChord>(), AnalysisEntity.decodeChords(""))
        assertEquals(1, AnalysisEntity.decodeChords("G|0|10;broken").size)
    }

    @Test
    fun `unknown source falls back to algorithm`() {
        val entity = AnalysisEntity.from(SampleAnalysis.result, "u1", synced = true).copy(source = "MAGIC")
        assertEquals(AnalysisSource.ALGORITHM, entity.toDomain().source)
    }
}
