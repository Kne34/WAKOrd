package edu.bluejack26_1.wakord.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import edu.bluejack26_1.wakord.data.remote.firebaseCall
import edu.bluejack26_1.wakord.data.remote.int
import edu.bluejack26_1.wakord.data.remote.requireUid
import edu.bluejack26_1.wakord.data.remote.string
import edu.bluejack26_1.wakord.data.remote.strings
import edu.bluejack26_1.wakord.data.remote.valueFlow
import edu.bluejack26_1.wakord.domain.model.ChordProgression
import edu.bluejack26_1.wakord.domain.repository.ProgressionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirebaseProgressionRepository(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
) : ProgressionRepository {
    override fun progression(id: String): Flow<ChordProgression?> =
        database.getReference("progressions/$id").valueFlow().map { if (it.exists()) it.toProgression() else null }

    override fun isLiked(id: String): Flow<Boolean> = database.likedFlow(auth, id)

    override suspend fun setLiked(id: String, liked: Boolean): Result<Unit> = firebaseCall {
        database.toggleLike(auth.requireUid(), id, "progressions/$id", liked)
    }

    override suspend fun recordPlay(id: String): Result<Unit> = firebaseCall {
        database.getReference("progressions/$id/playCount").setValue(ServerValue.increment(1L)).await()
        Unit
    }

    private fun DataSnapshot.toProgression() = ChordProgression(
        id = key.orEmpty(),
        title = string("title"),
        key = string("key"),
        bpm = int("bpm").takeIf { it > 0 } ?: 90,
        genre = string("genre"),
        difficulty = difficulty(),
        chords = strings("chords"),
        authorId = string("authorId"),
        authorName = string("authorName"),
        description = string("description"),
        playCount = int("playCount"),
        likeCount = int("likeCount"),
        saveCount = int("saveCount"),
    )
}
