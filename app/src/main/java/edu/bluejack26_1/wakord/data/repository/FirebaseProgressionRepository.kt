package edu.bluejack26_1.wakord.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import edu.bluejack26_1.wakord.data.remote.firebaseCall
import edu.bluejack26_1.wakord.data.remote.requireUid
import edu.bluejack26_1.wakord.data.remote.valueFlow
import edu.bluejack26_1.wakord.domain.model.ChordProgression
import edu.bluejack26_1.wakord.domain.model.ProgressionDraft
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
        val updates = mutableMapOf<String, Any>("progressions/$id/playCount" to ServerValue.increment(1L))
        auth.currentUser?.uid?.let { updates["plays/$it/$id"] = ServerValue.TIMESTAMP }
        database.reference.updateChildren(updates).await()
        Unit
    }

    override suspend fun create(draft: ProgressionDraft): Result<String> = firebaseCall {
        val uid = auth.requireUid()
        val authorName = database.getReference("users/$uid/fullName").get().await().getValue(String::class.java).orEmpty()
        val ref = database.getReference("progressions").push()
        ref.setValue(
            mapOf(
                "title" to draft.title.trim(),
                "key" to draft.key,
                "bpm" to draft.bpm,
                "genre" to draft.genre,
                "difficulty" to draft.difficulty.name,
                "chords" to draft.chords,
                "description" to draft.description.trim(),
                "authorId" to uid,
                "authorName" to authorName,
                "playCount" to 0,
                "likeCount" to 0,
                "saveCount" to 0,
                "createdAt" to ServerValue.TIMESTAMP,
            ),
        ).await()
        requireNotNull(ref.key)
    }
}
