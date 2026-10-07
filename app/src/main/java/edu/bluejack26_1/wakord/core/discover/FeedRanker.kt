package edu.bluejack26_1.wakord.core.discover

import edu.bluejack26_1.wakord.domain.model.ChordProgression
import edu.bluejack26_1.wakord.domain.model.FeedItem
import edu.bluejack26_1.wakord.domain.model.Instrument
import edu.bluejack26_1.wakord.domain.model.Song
import edu.bluejack26_1.wakord.domain.model.User
import edu.bluejack26_1.wakord.domain.model.UserActivity
import kotlin.math.ln

object Recommender {
    private const val LIKE_WEIGHT = 2
    private const val SAVE_WEIGHT = 3
    private const val PLAY_WEIGHT = 1

    fun genreAffinity(songs: List<Song>, progressions: List<ChordProgression>, activity: UserActivity): Map<String, Int> {
        val genres = songs.associate { it.id to it.genre } + progressions.associate { it.id to it.genre }
        val affinity = mutableMapOf<String, Int>()
        fun add(ids: Set<String>, weight: Int) = ids.forEach { id ->
            genres[id]?.lowercase()?.takeIf { it.isNotEmpty() }?.let { affinity[it] = (affinity[it] ?: 0) + weight }
        }
        add(activity.likedIds, LIKE_WEIGHT)
        add(activity.savedIds, SAVE_WEIGHT)
        add(activity.playedIds, PLAY_WEIGHT)
        return affinity
    }

    fun recommendSongs(
        songs: List<Song>,
        user: User?,
        activity: UserActivity,
        affinity: Map<String, Int>,
        limit: Int = 5,
    ): List<Song> {
        val known = activity.likedIds + activity.savedIds
        val favoriteArtists = songs.filter { it.id in known }.map { it.artist.lowercase() }.toSet()
        val profileGenres = user?.genres.orEmpty().map { it.lowercase() }.toSet()
        return songs
            .filter { it.id !in known }
            .sortedByDescending { song ->
                val genre = song.genre.lowercase()
                (if (genre in profileGenres) 3.0 else 0.0) +
                    (affinity[genre] ?: 0) +
                    (if (song.artist.lowercase() in favoriteArtists) 2.0 else 0.0) +
                    popularity(song.likeCount)
            }
            .take(limit)
    }

    internal fun popularity(likes: Int): Double = ln(1.0 + likes.coerceAtLeast(0)) / ln(10.0) * 0.5
}

object FeedRanker {
    private val PROGRESSION_PLAYERS = setOf(Instrument.GUITAR, Instrument.UKULELE, Instrument.BASS, Instrument.PIANO)

    fun rank(items: List<FeedItem>, user: User?, affinity: Map<String, Int>): List<FeedItem> {
        val profileGenres = user?.genres.orEmpty().map { it.lowercase() }.toSet()
        val instruments = user?.instruments.orEmpty().toSet()
        return items.sortedByDescending { item ->
            val genre = item.genre.lowercase()
            val instrumentBonus = when (item) {
                is FeedItem.SongItem -> if (Instrument.VOCALS in instruments) 1.0 else 0.0
                is FeedItem.ProgressionItem -> if (instruments.any { it in PROGRESSION_PLAYERS }) 1.0 else 0.0
            }
            (if (genre in profileGenres) 3.0 else 0.0) +
                (affinity[genre] ?: 0) +
                instrumentBonus +
                Recommender.popularity(item.likeCount)
        }
    }
}
