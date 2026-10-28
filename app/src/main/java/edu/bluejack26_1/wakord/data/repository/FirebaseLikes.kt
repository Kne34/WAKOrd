package edu.bluejack26_1.wakord.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ServerValue
import edu.bluejack26_1.wakord.data.sync.FirebaseBackup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal suspend fun FirebaseBackup.toggleLike(
    uid: String,
    targetId: String,
    counterPath: String,
    liked: Boolean,
    extra: Map<String, Any?> = emptyMap(),
) {
    update(
        mapOf(
            "likes/$targetId/$uid" to (if (liked) true else null),
            "userLikes/$uid/$targetId" to (if (liked) ServerValue.TIMESTAMP else null),
            "$counterPath/likeCount" to ServerValue.increment(if (liked) 1L else -1L),
        ) + extra,
    )
}

internal fun FirebaseBackup.likedFlow(auth: FirebaseAuth, targetId: String): Flow<Boolean> {
    val uid = auth.currentUser?.uid ?: return flowOf(false)
    return observe("userLikes/$uid/$targetId").map { it.exists() }
}
