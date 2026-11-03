package edu.bluejack26_1.wakord.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ServerValue
import edu.bluejack26_1.wakord.core.discover.SearchEngine
import edu.bluejack26_1.wakord.data.remote.firebaseCall
import edu.bluejack26_1.wakord.data.remote.requireUid
import edu.bluejack26_1.wakord.data.sync.FirebaseBackup
import edu.bluejack26_1.wakord.domain.model.Song
import edu.bluejack26_1.wakord.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirebaseSongRepository(
    private val auth: FirebaseAuth,
    private val backup: FirebaseBackup,
) : SongRepository {
    override fun song(id: String): Flow<Song?> =
        backup.observe("songs/$id").map { if (it.exists()) it.toSong() else null }

    override fun isLiked(id: String): Flow<Boolean> = backup.likedFlow(auth, id)

    override suspend fun setLiked(id: String, liked: Boolean): Result<Unit> = firebaseCall {
        val week = SearchEngine.weekKey(System.currentTimeMillis())
        backup.toggleLike(
            auth.requireUid(),
            id,
            "songs/$id",
            liked,
            mapOf("weeklyLikes/$week/$id" to ServerValue.increment(if (liked) 1L else -1L)),
        )
    }
}
