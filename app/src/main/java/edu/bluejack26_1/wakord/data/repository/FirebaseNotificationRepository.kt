package edu.bluejack26_1.wakord.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ServerValue
import edu.bluejack26_1.wakord.data.remote.NodeOrder
import edu.bluejack26_1.wakord.data.remote.NodeQuery
import edu.bluejack26_1.wakord.data.remote.firebaseCall
import edu.bluejack26_1.wakord.data.remote.long
import edu.bluejack26_1.wakord.data.remote.requireUid
import edu.bluejack26_1.wakord.data.remote.string
import edu.bluejack26_1.wakord.data.sync.FirebaseBackup
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

internal suspend fun FirebaseBackup.displayName(uid: String): String =
    firebaseCall { get("users/$uid/fullName").asString() }.getOrNull().orEmpty()

internal suspend fun FirebaseBackup.notify(
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
        val id = newKey("notifications/$recipientId")
        update(
            mapOf(
                "notifications/$recipientId/$id" to mapOf(
                    "type" to type.name,
                    "actorId" to actorId,
                    "actorName" to displayName(actorId),
                    "targetType" to targetType.name,
                    "targetId" to targetId,
                    "targetTitle" to targetTitle,
                    "message" to message.take(MESSAGE_PREVIEW),
                    "createdAt" to ServerValue.TIMESTAMP,
                    "read" to false,
                ),
            ),
        )
    }
}

private const val MESSAGE_PREVIEW = 120

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseNotificationRepository(
    private val auth: FirebaseAuth,
    private val backup: FirebaseBackup,
) : NotificationRepository {
    override val notifications: Flow<List<AppNotification>> = uidFlow().flatMapLatest { uid ->
        if (uid == null) {
            flowOf(emptyList())
        } else {
            backup.observe(NodeQuery("notifications/$uid", NodeOrder.Child("createdAt"), limitToLast = LIMIT)).map { snapshot ->
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
                        isRead = c.child("read").asBoolean() ?: false,
                    )
                }.sortedByDescending { it.createdAt }
            }
        }
    }

    override val unreadCount: Flow<Int> = notifications.map { list -> list.count { !it.isRead } }

    override suspend fun markRead(id: String): Result<Unit> = firebaseCall {
        backup.update(mapOf("notifications/${auth.requireUid()}/$id/read" to true))
    }

    override suspend fun markAllRead(): Result<Unit> = firebaseCall {
        val uid = auth.requireUid()
        val unread = backup.get(NodeQuery("notifications/$uid", NodeOrder.Child("read"), equalTo = false))
        val updates = unread.children.mapNotNull { it.key }.associate { "notifications/$uid/$it/read" to true }
        if (updates.isNotEmpty()) backup.update(updates)
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
