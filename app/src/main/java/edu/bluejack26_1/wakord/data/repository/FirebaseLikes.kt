package edu.bluejack26_1.wakord.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import edu.bluejack26_1.wakord.data.remote.valueFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

internal suspend fun FirebaseDatabase.toggleLike(
    uid: String,
    targetId: String,
    counterPath: String,
    liked: Boolean,
    extra: Map<String, Any?> = emptyMap(),
) {
    reference.updateChildren(
        mapOf(
            "likes/$targetId/$uid" to (if (liked) true else null),
            "userLikes/$uid/$targetId" to (if (liked) ServerValue.TIMESTAMP else null),
            "$counterPath/likeCount" to ServerValue.increment(if (liked) 1L else -1L),
        ) + extra,
    ).await()
}

internal fun FirebaseDatabase.likedFlow(auth: FirebaseAuth, targetId: String): Flow<Boolean> {
    val uid = auth.currentUser?.uid ?: return flowOf(false)
    return getReference("likes/$targetId/$uid").valueFlow().map { it.exists() }
}
