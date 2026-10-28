package edu.bluejack26_1.wakord.data.repository

import edu.bluejack26_1.wakord.core.discover.SearchEngine
import edu.bluejack26_1.wakord.data.remote.NodeOrder
import edu.bluejack26_1.wakord.data.remote.NodeQuery
import edu.bluejack26_1.wakord.data.remote.firebaseCall
import edu.bluejack26_1.wakord.data.sync.FirebaseBackup
import edu.bluejack26_1.wakord.domain.model.Catalog
import edu.bluejack26_1.wakord.domain.model.ChordProgression
import edu.bluejack26_1.wakord.domain.model.Song
import edu.bluejack26_1.wakord.domain.repository.ExploreRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class FirebaseExploreRepository(private val backup: FirebaseBackup) : ExploreRepository {
    override suspend fun trendingSongs(limit: Int): Result<List<Song>> = firebaseCall {
        backup.get(NodeQuery("songs", NodeOrder.Child("likeCount"), limitToLast = limit))
            .children.map { it.toSong() }.sortedByDescending { it.likeCount }
    }

    override suspend fun trendingProgressions(limit: Int): Result<List<ChordProgression>> = firebaseCall {
        backup.get(NodeQuery("progressions", NodeOrder.Child("likeCount"), limitToLast = limit))
            .children.map { it.toProgression() }.sortedByDescending { it.likeCount }
    }

    override suspend fun topSongsThisWeek(limit: Int): Result<List<Song>> = firebaseCall {
        val week = SearchEngine.weekKey(System.currentTimeMillis())
        val ranked = backup.get(NodeQuery("weeklyLikes/$week", NodeOrder.Value, limitToLast = limit)).children
            .mapNotNull { c -> c.key?.let { it to (c.asLong() ?: 0L) } }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
        coroutineScope {
            ranked.map { (id, _) -> async { backup.get("songs/$id") } }
                .awaitAll()
                .filter { it.exists() }
                .map { it.toSong() }
        }
    }

    override suspend fun catalog(): Result<Catalog> = firebaseCall {
        coroutineScope {
            val songs = async { backup.get("songs").children.map { it.toSong() } }
            val progressions = async { backup.get("progressions").children.map { it.toProgression() } }
            Catalog(songs.await(), progressions.await())
        }
    }
}
