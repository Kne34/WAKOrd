package edu.bluejack26_1.wakord.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import edu.bluejack26_1.wakord.core.error.AppError
import edu.bluejack26_1.wakord.data.remote.firebaseCall
import edu.bluejack26_1.wakord.data.remote.long
import edu.bluejack26_1.wakord.data.remote.requireUid
import edu.bluejack26_1.wakord.data.remote.string
import edu.bluejack26_1.wakord.data.remote.valueFlow
import edu.bluejack26_1.wakord.domain.model.Comment
import edu.bluejack26_1.wakord.domain.model.NotificationType
import edu.bluejack26_1.wakord.domain.model.TargetType
import edu.bluejack26_1.wakord.domain.repository.CommentModerator
import edu.bluejack26_1.wakord.domain.repository.CommentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirebaseCommentRepository(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
    private val moderator: CommentModerator,
) : CommentRepository {
    override fun comments(targetId: String): Flow<List<Comment>> {
        val uid = auth.currentUser?.uid
        return database.getReference("comments/$targetId").orderByChild("createdAt").valueFlow().map { snapshot ->
            snapshot.children.map { c ->
                Comment(
                    id = c.key.orEmpty(),
                    authorId = c.string("authorId"),
                    authorName = c.string("authorName"),
                    text = c.string("text"),
                    parentId = c.child("parentId").getValue(String::class.java),
                    likeCount = c.child("likes").childrenCount.toInt(),
                    likedByMe = uid != null && c.child("likes/$uid").exists(),
                    createdAt = c.long("createdAt"),
                )
            }
        }
    }

    override suspend fun post(targetId: String, text: String, parentId: String?): Result<Unit> = firebaseCall {
        val uid = auth.requireUid()
        if (!moderator.isAllowed(text)) throw AppError.CommentBlocked
        val authorName = database.getReference("users/$uid/fullName").get().await().getValue(String::class.java).orEmpty()
        database.getReference("comments/$targetId").push().setValue(
            mapOf(
                "authorId" to uid,
                "authorName" to authorName,
                "text" to text.trim(),
                "parentId" to parentId,
                "createdAt" to ServerValue.TIMESTAMP,
            ),
        ).await()
        notifyAuthor(uid, targetId, text, parentId)
    }

    private suspend fun notifyAuthor(uid: String, targetId: String, text: String, parentId: String?) {
        val progression = database.getReference("progressions/$targetId").get().await()
        val targetType = if (progression.exists()) TargetType.PROGRESSION else TargetType.SONG
        val title = (if (progression.exists()) progression else database.getReference("songs/$targetId").get().await())
            .child("title").getValue(String::class.java).orEmpty()
        val recipient = if (parentId != null) {
            database.getReference("comments/$targetId/$parentId/authorId").get().await().getValue(String::class.java).orEmpty()
        } else {
            progression.child("authorId").getValue(String::class.java).orEmpty()
        }
        val type = if (parentId != null) NotificationType.REPLY else NotificationType.COMMENT
        database.notify(recipient, uid, type, targetType, targetId, title, text.trim())
    }

    override suspend fun setLiked(targetId: String, commentId: String, liked: Boolean): Result<Unit> = firebaseCall {
        val uid = auth.requireUid()
        val ref = database.getReference("comments/$targetId/$commentId/likes/$uid")
        if (liked) ref.setValue(true).await() else ref.removeValue().await()
        Unit
    }
}
