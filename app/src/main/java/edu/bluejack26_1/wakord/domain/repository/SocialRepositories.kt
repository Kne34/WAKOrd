package edu.bluejack26_1.wakord.domain.repository

import edu.bluejack26_1.wakord.domain.model.AppNotification
import edu.bluejack26_1.wakord.domain.model.ChordProgression
import edu.bluejack26_1.wakord.domain.model.ProfileStats
import edu.bluejack26_1.wakord.domain.model.ProfileUpdate
import edu.bluejack26_1.wakord.domain.model.ProgressionDraft
import edu.bluejack26_1.wakord.domain.model.SavedEntry
import edu.bluejack26_1.wakord.domain.model.SavedType
import edu.bluejack26_1.wakord.domain.model.User
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun user(uid: String): Flow<User?>
    fun progressionsBy(uid: String): Flow<List<ChordProgression>>
    suspend fun updateProfile(update: ProfileUpdate): Result<Unit>
    suspend fun uploadPhoto(uri: String): Result<String>
}

interface FollowRepository {
    fun isFollowing(uid: String): Flow<Boolean>
    fun stats(uid: String): Flow<ProfileStats>
    suspend fun setFollowing(uid: String, follow: Boolean): Result<Unit>
}

interface NotificationRepository {
    val notifications: Flow<List<AppNotification>>
    val unreadCount: Flow<Int>
    suspend fun markRead(id: String): Result<Unit>
    suspend fun markAllRead(): Result<Unit>
}

interface SavedLibraryRepository {
    fun entries(type: SavedType): Flow<List<SavedEntry>>
    val pendingCount: Flow<Int>
    val lastSyncedAt: Flow<Long?>
    suspend fun syncNow(): Result<Int>
}

interface DraftRepository {
    val draft: Flow<ProgressionDraft?>
    suspend fun saveDraft(draft: ProgressionDraft)
    suspend fun clearDraft()
}
