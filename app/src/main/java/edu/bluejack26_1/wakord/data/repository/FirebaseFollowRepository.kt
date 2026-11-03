package edu.bluejack26_1.wakord.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ServerValue
import edu.bluejack26_1.wakord.data.remote.firebaseCall
import edu.bluejack26_1.wakord.data.remote.requireUid
import edu.bluejack26_1.wakord.data.sync.FirebaseBackup
import edu.bluejack26_1.wakord.domain.model.NotificationType
import edu.bluejack26_1.wakord.domain.model.ProfileStats
import edu.bluejack26_1.wakord.domain.model.TargetType
import edu.bluejack26_1.wakord.domain.repository.FollowRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FirebaseFollowRepository(
    private val auth: FirebaseAuth,
    private val backup: FirebaseBackup,
) : FollowRepository {
    override fun isFollowing(uid: String): Flow<Boolean> {
        val me = auth.currentUser?.uid ?: return flowOf(false)
        return backup.observe("follows/$me/$uid").map { it.exists() }
    }

    override fun stats(uid: String): Flow<ProfileStats> = combine(
        backup.observe("followers/$uid"),
        backup.observe("follows/$uid"),
    ) { followers, following -> ProfileStats(followers.childrenCount, following.childrenCount) }

    override suspend fun setFollowing(uid: String, follow: Boolean): Result<Unit> = firebaseCall {
        val me = auth.requireUid()
        require(me != uid)
        val value = if (follow) ServerValue.TIMESTAMP else null
        backup.update(
            mapOf(
                "follows/$me/$uid" to value,
                "followers/$uid/$me" to value,
            ),
        )
        if (follow) backup.notify(uid, me, NotificationType.FOLLOW, TargetType.PROFILE, me)
        Unit
    }
}
