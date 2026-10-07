package edu.bluejack26_1.wakord.data.repository

import com.google.firebase.database.FirebaseDatabase
import edu.bluejack26_1.wakord.core.discover.SearchEngine
import edu.bluejack26_1.wakord.data.remote.firebaseCall
import edu.bluejack26_1.wakord.domain.model.Catalog
import edu.bluejack26_1.wakord.domain.model.ChordProgression
import edu.bluejack26_1.wakord.domain.model.Song
import edu.bluejack26_1.wakord.domain.repository.ExploreRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

class FirebaseExploreRepository(private val database: FirebaseDatabase) : ExploreRepository {
    override suspend fun trendingSongs(limit: Int): Result<List<Song>> = firebaseCall {
        database.getReference("songs").orderByChild("likeCount").limitToLast(limit)
            .get().await().children.map { it.toSong() }.sortedByDescending { it.likeCount }
    }

    override suspend fun trendingProgressions(limit: Int): Result<List<ChordProgression>> = firebaseCall {
        database.getReference("progressions").orderByChild("likeCount").limitToLast(limit)
            .get().await().children.map { it.toProgression() }.sortedByDescending { it.likeCount }
    }

    override suspend fun topSongsThisWeek(limit: Int): Result<List<Song>> = firebaseCall {
        val week = SearchEngine.weekKey(System.currentTimeMillis())
        val ranked = database.getReference("weeklyLikes/$week").orderByValue().limitToLast(limit)
            .get().await().children
            .mapNotNull { c -> c.key?.let { it to (c.getValue(Long::class.java) ?: 0L) } }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
        coroutineScope {
            ranked.map { (id, _) -> async { database.getReference("songs/$id").get().await() } }
                .awaitAll()
                .filter { it.exists() }
                .map { it.toSong() }
        }
    }

    override suspend fun catalog(): Result<Catalog> = firebaseCall {
        coroutineScope {
            val songs = async { database.getReference("songs").get().await().children.map { it.toSong() } }
            val progressions = async { database.getReference("progressions").get().await().children.map { it.toProgression() } }
            Catalog(songs.await(), progressions.await())
        }
    }
}
