package edu.bluejack26_1.wakord.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ServerValue
import edu.bluejack26_1.wakord.core.error.AppError
import edu.bluejack26_1.wakord.data.remote.NodeOrder
import edu.bluejack26_1.wakord.data.remote.NodeQuery
import edu.bluejack26_1.wakord.data.remote.firebaseCall
import edu.bluejack26_1.wakord.data.remote.long
import edu.bluejack26_1.wakord.data.remote.requireUid
import edu.bluejack26_1.wakord.data.remote.string
import edu.bluejack26_1.wakord.data.sync.FirebaseBackup
import edu.bluejack26_1.wakord.domain.model.Comment
import edu.bluejack26_1.wakord.domain.model.NotificationType
import edu.bluejack26_1.wakord.domain.model.TargetType
import edu.bluejack26_1.wakord.domain.repository.CommentModerator
import edu.bluejack26_1.wakord.domain.repository.CommentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirebaseCommentRepository(
    private val auth: FirebaseAuth,
    private val backup: FirebaseBackup,
    private val moderator: CommentModerator,
) : CommentRepository {
    override fun comments(targetId: String): Flow<List<Comment>> {
        val uid = auth.currentUser?.uid
        return backup.observe(NodeQuery("comments/$targetId", NodeOrder.Child("createdAt"))).map { snapshot ->
            snapshot.children.map { c ->
                Comment(
                    id = c.key.orEmpty(),
                    authorId = c.string("authorId"),
                    authorName = c.string("authorName"),
                    text = c.string("text"),
                    parentId = c.child("parentId").asString(),
                    likeCount = c.child("likes").childrenCount,
                    likedByMe = uid != null && c.child("likes/$uid").exists(),
                    createdAt = c.long("createdAt"),
                )
            }
        }
    }

    override suspend fun post(targetId: String, text: String, parentId: String?): Result<Unit> = firebaseCall {
        val uid = auth.requireUid()
        if (!moderator.isAllowed(text)) throw AppError.CommentBlocked
        val id = backup.newKey("comments/$targetId")
        backup.update(
            mapOf(
                "comments/$targetId/$id" to mapOf(
                    "authorId" to uid,
                    "authorName" to backup.displayName(uid),
                    "text" to text.trim(),
                    "parentId" to parentId,
                    "createdAt" to ServerValue.TIMESTAMP,
                ),
            ),
        )
        notifyAuthor(uid, targetId, text, parentId)
    }

    private suspend fun notifyAuthor(uid: String, targetId: String, text: String, parentId: String?) {
        val progression = firebaseCall { backup.get("progressions/$targetId") }.getOrNull()
        val isProgression = progression?.exists() == true
        val targetType = if (isProgression) TargetType.PROGRESSION else TargetType.SONG
        val target = if (isProgression) progression else firebaseCall { backup.get("songs/$targetId") }.getOrNull()
        val recipient = if (parentId != null) {
            firebaseCall { backup.get("comments/$targetId/$parentId/authorId").asString() }.getOrNull().orEmpty()
        } else {
            progression?.string("authorId").orEmpty()
        }
        val type = if (parentId != null) NotificationType.REPLY else NotificationType.COMMENT
        backup.notify(recipient, uid, type, targetType, targetId, target?.string("title").orEmpty(), text.trim())
    }

    override suspend fun setLiked(targetId: String, commentId: String, liked: Boolean): Result<Unit> = firebaseCall {
        val uid = auth.requireUid()
        backup.update(mapOf("comments/$targetId/$commentId/likes/$uid" to (if (liked) true else null)))
    }
}
