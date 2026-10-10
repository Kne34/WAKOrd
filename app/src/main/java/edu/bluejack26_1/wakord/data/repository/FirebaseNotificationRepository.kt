package edu.bluejack26_1.wakord.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import edu.bluejack26_1.wakord.data.remote.firebaseCall
import edu.bluejack26_1.wakord.data.remote.long
import edu.bluejack26_1.wakord.data.remote.requireUid
import edu.bluejack26_1.wakord.data.remote.string
import edu.bluejack26_1.wakord.data.remote.valueFlow
import edu.bluejack26_1.wakord.domain.model.AppNotification
import edu.bluejack26_1.wakord.domain.model.NotificationType
import edu.bluejack26_1.wakord.domain.model.TargetType
import edu.bluejack26_1.wakord.domain.repository.NotificationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

internal suspend fun FirebaseDatabase.notify(
    recipientId: String,
    actorId: String,
    type: NotificationType,
    targetType: TargetType,
    targetId: String,
    targetTitle: String = "",
    message: String = "",
) {
    if (recipientId.isEmpty() || recipientId == actorId) return
    firebaseCall {
        val actorName = getReference("users/$actorId/fullName").get().await().getValue(String::class.java).orEmpty()
        getReference("notifications/$recipientId").push().setValue(
            mapOf(
                "type" to type.name,
                "actorId" to actorId,
                "actorName" to actorName,
                "targetType" to targetType.name,
                "targetId" to targetId,
                "targetTitle" to targetTitle,
                "message" to message.take(MESSAGE_PREVIEW),
                "createdAt" to ServerValue.TIMESTAMP,
                "read" to false,
            ),
        ).await()
    }
}

private const val MESSAGE_PREVIEW = 120

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseNotificationRepository(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
) : NotificationRepository {
    override val notifications: Flow<List<AppNotification>> = uidFlow().flatMapLatest { uid ->
        if (uid == null) {
            flowOf(emptyList())
        } else {
            database.getReference("notifications/$uid").orderByChild("createdAt").limitToLast(LIMIT).valueFlow().map { snapshot ->
                snapshot.children.mapNotNull { c ->
                    AppNotification(
                        id = c.key ?: return@mapNotNull null,
                        type = NotificationType.entries.firstOrNull { it.name == c.string("type") } ?: return@mapNotNull null,
                        actorId = c.string("actorId"),
                        actorName = c.string("actorName"),
                        targetType = TargetType.entries.firstOrNull { it.name == c.string("targetType") } ?: TargetType.PROFILE,
                        targetId = c.string("targetId"),
                        targetTitle = c.string("targetTitle"),
                        message = c.string("message"),
                        createdAt = c.long("createdAt"),
                        isRead = c.child("read").getValue(Boolean::class.java) ?: false,
                    )
                }.sortedByDescending { it.createdAt }
            }
        }
    }

    override val unreadCount: Flow<Int> = notifications.map { list -> list.count { !it.isRead } }

    override suspend fun markRead(id: String): Result<Unit> = firebaseCall {
        database.getReference("notifications/${auth.requireUid()}/$id/read").setValue(true).await()
        Unit
    }

    override suspend fun markAllRead(): Result<Unit> = firebaseCall {
        val uid = auth.requireUid()
        val unread = database.getReference("notifications/$uid").orderByChild("read").equalTo(false).get().await()
        val updates = unread.children.mapNotNull { it.key }.associate { "notifications/$uid/$it/read" to true }
        if (updates.isNotEmpty()) database.reference.updateChildren(updates).await()
        Unit
    }

    private fun uidFlow(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    private companion object {
        const val LIMIT = 100
    }
}
