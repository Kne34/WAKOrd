package edu.bluejack26_1.wakord.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import edu.bluejack26_1.wakord.data.remote.firebaseCall
import edu.bluejack26_1.wakord.data.remote.requireUid
import edu.bluejack26_1.wakord.data.remote.valueFlow
import edu.bluejack26_1.wakord.domain.model.ProfileStats
import edu.bluejack26_1.wakord.domain.repository.FollowRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirebaseFollowRepository(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
) : FollowRepository {
    override fun isFollowing(uid: String): Flow<Boolean> {
        val me = auth.currentUser?.uid ?: return flowOf(false)
        return database.getReference("follows/$me/$uid").valueFlow().map { it.exists() }
    }

    override fun stats(uid: String): Flow<ProfileStats> = combine(
        database.getReference("followers/$uid").valueFlow(),
        database.getReference("follows/$uid").valueFlow(),
    ) { followers, following -> ProfileStats(followers.childrenCount.toInt(), following.childrenCount.toInt()) }

    override suspend fun setFollowing(uid: String, follow: Boolean): Result<Unit> = firebaseCall {
        val me = auth.requireUid()
        require(me != uid)
        val value = if (follow) ServerValue.TIMESTAMP else null
        database.reference.updateChildren(
            mapOf(
                "follows/$me/$uid" to value,
                "followers/$uid/$me" to value,
            ),
        ).await()
        Unit
    }
}
