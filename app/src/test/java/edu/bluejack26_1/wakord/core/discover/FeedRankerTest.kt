package edu.bluejack26_1.wakord.core.discover

import edu.bluejack26_1.wakord.data.fake.SampleDiscover
import edu.bluejack26_1.wakord.domain.model.FeedItem
import edu.bluejack26_1.wakord.domain.model.Instrument
import edu.bluejack26_1.wakord.domain.model.Song
import edu.bluejack26_1.wakord.domain.model.User
import edu.bluejack26_1.wakord.domain.model.UserActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedRankerTest {
    private val songs = listOf(
        Song(id = "a", title = "A", artist = "Neon Mirage", key = "C", genre = "Pop", likeCount = 10),
        Song(id = "b", title = "B", artist = "Koro", key = "C", genre = "Jazz", likeCount = 5000),
        Song(id = "c", title = "C", artist = "Neon Mirage", key = "C", genre = "Rock", likeCount = 1),
        Song(id = "d", title = "D", artist = "Vance", key = "C", genre = "Neo-Soul", likeCount = 3),
    )

    private fun user(genres: List<String> = emptyList(), instruments: List<Instrument> = emptyList()) =
        User(uid = "u", fullName = "U", email = "u@mail.com", genres = genres, instruments = instruments)

    @Test
    fun `affinity weights saves over likes over plays`() {
        val affinity = Recommender.genreAffinity(
            songs,
            emptyList(),
            UserActivity(likedIds = setOf("a"), savedIds = setOf("b"), playedIds = setOf("d")),
        )
        assertEquals(mapOf("pop" to 2, "jazz" to 3, "neo-soul" to 1), affinity)
    }

    @Test
    fun `without any signal the most liked song comes first`() {
        val result = Recommender.recommendSongs(songs, null, UserActivity(), emptyMap())
        assertEquals("b", result.first().id)
    }

    @Test
    fun `favorite genre beats popularity`() {
        val result = Recommender.recommendSongs(songs, user(genres = listOf("Neo-Soul")), UserActivity(), emptyMap())
        assertEquals("d", result.first().id)
    }

    @Test
    fun `liked songs are not recommended again but their artist is`() {
        val activity = UserActivity(likedIds = setOf("a"))
        val affinity = Recommender.genreAffinity(songs, emptyList(), activity)
        val result = Recommender.recommendSongs(songs, null, activity, affinity)
        assertFalse(result.any { it.id == "a" })
        assertTrue(result.indexOfFirst { it.id == "c" } < result.indexOfFirst { it.id == "d" })
    }

    @Test
    fun `instruments decide between songs and progressions`() {
        val items = listOf(FeedItem.SongItem(songs[3]), FeedItem.ProgressionItem(SampleDiscover.progressions[0].copy(genre = "Neo-Soul", likeCount = 3)))
        assertTrue(FeedRanker.rank(items, user(instruments = listOf(Instrument.VOCALS)), emptyMap()).first() is FeedItem.SongItem)
        assertTrue(FeedRanker.rank(items, user(instruments = listOf(Instrument.PIANO)), emptyMap()).first() is FeedItem.ProgressionItem)
    }
}
