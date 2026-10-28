package edu.bluejack26_1.wakord.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ServerValue
import edu.bluejack26_1.wakord.data.remote.firebaseCall
import edu.bluejack26_1.wakord.data.remote.requireUid
import edu.bluejack26_1.wakord.data.remote.string
import edu.bluejack26_1.wakord.data.sync.FirebaseBackup
import edu.bluejack26_1.wakord.domain.model.ChordProgression
import edu.bluejack26_1.wakord.domain.model.NotificationType
import edu.bluejack26_1.wakord.domain.model.ProgressionDraft
import edu.bluejack26_1.wakord.domain.model.TargetType
import edu.bluejack26_1.wakord.domain.repository.ProgressionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirebaseProgressionRepository(
    private val auth: FirebaseAuth,
    private val backup: FirebaseBackup,
) : ProgressionRepository {
    override fun progression(id: String): Flow<ChordProgression?> =
        backup.observe("progressions/$id").map { if (it.exists()) it.toProgression() else null }

    override fun isLiked(id: String): Flow<Boolean> = backup.likedFlow(auth, id)

    override suspend fun setLiked(id: String, liked: Boolean): Result<Unit> = firebaseCall {
        val uid = auth.requireUid()
        backup.toggleLike(uid, id, "progressions/$id", liked)
        if (liked) {
            val progression = backup.get("progressions/$id")
            backup.notify(
                recipientId = progression.string("authorId"),
                actorId = uid,
                type = NotificationType.LIKE,
                targetType = TargetType.PROGRESSION,
                targetId = id,
                targetTitle = progression.string("title"),
            )
        }
        Unit
    }

    override suspend fun recordPlay(id: String): Result<Unit> = firebaseCall {
        val updates = mutableMapOf<String, Any?>("progressions/$id/playCount" to ServerValue.increment(1L))
        auth.currentUser?.uid?.let { updates["plays/$it/$id"] = ServerValue.TIMESTAMP }
        backup.update(updates)
    }

    override suspend fun create(draft: ProgressionDraft): Result<String> = firebaseCall {
        val uid = auth.requireUid()
        val authorName = backup.displayName(uid)
        val id = backup.newKey("progressions")
        backup.update(
            mapOf(
                "progressions/$id" to mapOf(
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
            ),
        )
        id
    }
}
