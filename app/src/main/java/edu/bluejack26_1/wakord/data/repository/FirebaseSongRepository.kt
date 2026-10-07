package edu.bluejack26_1.wakord.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import edu.bluejack26_1.wakord.core.discover.SearchEngine
import edu.bluejack26_1.wakord.data.remote.firebaseCall
import edu.bluejack26_1.wakord.data.remote.requireUid
import edu.bluejack26_1.wakord.data.remote.valueFlow
import edu.bluejack26_1.wakord.domain.model.Song
import edu.bluejack26_1.wakord.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirebaseSongRepository(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
) : SongRepository {
    override fun song(id: String): Flow<Song?> =
        database.getReference("songs/$id").valueFlow().map { if (it.exists()) it.toSong() else null }

    override fun isLiked(id: String): Flow<Boolean> = database.likedFlow(auth, id)

    override suspend fun setLiked(id: String, liked: Boolean): Result<Unit> = firebaseCall {
        val week = SearchEngine.weekKey(System.currentTimeMillis())
        database.toggleLike(
            auth.requireUid(),
            id,
            "songs/$id",
            liked,
            mapOf("weeklyLikes/$week/$id" to ServerValue.increment(if (liked) 1L else -1L)),
        )
    }
}
