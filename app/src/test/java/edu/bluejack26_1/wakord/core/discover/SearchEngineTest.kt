package edu.bluejack26_1.wakord.core.discover

import edu.bluejack26_1.wakord.data.fake.SampleDiscover
import edu.bluejack26_1.wakord.data.fake.SampleMusic
import edu.bluejack26_1.wakord.domain.model.Catalog
import edu.bluejack26_1.wakord.domain.model.Difficulty
import edu.bluejack26_1.wakord.domain.model.ExploreFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchEngineTest {
    private val catalog = Catalog(SampleDiscover.songs, SampleDiscover.progressions.take(4))

    @Test
    fun `finds songs by title and artist ignoring case`() {
        assertEquals(listOf("midnight-waves"), SearchEngine.search("MIDNIGHT", catalog).songs.map { it.id })
        assertEquals(listOf("golden-hour-glow"), SearchEngine.search("koro acoustic", catalog).songs.map { it.id })
    }

    @Test
    fun `finds lyrics and returns a snippet`() {
        val result = SearchEngine.search("coffee", catalog)
        assertEquals(SampleMusic.song.id, result.lyricMatches.single().song.id)
        assertTrue(result.lyricMatches.single().snippet.contains("Coffee cooling"))
    }

    @Test
    fun `chord search matches root and suffix prefix across spellings`() {
        val result = SearchEngine.search("d#maj", catalog)
        assertEquals(listOf("Ebmaj7"), result.chords)
        assertEquals(listOf("Cmaj7"), SearchEngine.search("cmaj", catalog).chords)
    }

    @Test
    fun `artists are grouped with song count`() {
        val artists = SearchEngine.search("neon", catalog).artists
        assertEquals("Neon Mirage", artists.single().name)
        assertEquals(1, artists.single().songCount)
    }

    @Test
    fun `filters by genre and level`() {
        val pop = SearchEngine.search("o", catalog, ExploreFilter(genre = "pop"))
        assertTrue(pop.songs.all { it.genre == "Pop" })
        val hard = SearchEngine.search("progression", catalog, ExploreFilter(difficulty = Difficulty.ADVANCED))
        assertTrue(hard.progressions.isNotEmpty())
        assertTrue(hard.progressions.all { it.difficulty == Difficulty.ADVANCED })
    }

    @Test
    fun `blank query gives empty results`() {
        assertTrue(SearchEngine.search("   ", catalog).isEmpty)
    }

    @Test
    fun `week key follows iso weeks`() {
        assertEquals("2026-W40", SearchEngine.weekKey(1_791_072_000_000L))
        assertEquals("2026-W53", SearchEngine.weekKey(1_798_675_200_000L))
    }
}
