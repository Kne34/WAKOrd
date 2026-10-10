package edu.bluejack26_1.wakord.data.fake

import edu.bluejack26_1.wakord.domain.model.AppNotification
import edu.bluejack26_1.wakord.domain.model.ChordProgression
import edu.bluejack26_1.wakord.domain.model.NotificationType
import edu.bluejack26_1.wakord.domain.model.ProfileStats
import edu.bluejack26_1.wakord.domain.model.ProfileUpdate
import edu.bluejack26_1.wakord.domain.model.ProgressionDraft
import edu.bluejack26_1.wakord.domain.model.SavedEntry
import edu.bluejack26_1.wakord.domain.model.SavedType
import edu.bluejack26_1.wakord.domain.model.TargetType
import edu.bluejack26_1.wakord.domain.model.User
import edu.bluejack26_1.wakord.domain.repository.DraftRepository
import edu.bluejack26_1.wakord.domain.repository.FollowRepository
import edu.bluejack26_1.wakord.domain.repository.NotificationRepository
import edu.bluejack26_1.wakord.domain.repository.ProfileRepository
import edu.bluejack26_1.wakord.domain.repository.SavedLibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

object SampleSocial {
    val me = User(uid = "me", fullName = "Keane Wilson", email = "keane@mail.com", username = "keane", genres = listOf("Jazz", "Pop", "Lo-fi"))
    val julian = User(uid = "julian", fullName = "Julian Keys", email = "julian@mail.com", username = "julian")

    val notifications = listOf(
        AppNotification("n1", NotificationType.LIKE, "julian", "Julian Keys", TargetType.PROGRESSION, "p1", "Progression 1", createdAt = 1_791_099_000_000L),
        AppNotification("n2", NotificationType.FOLLOW, "julian", "Julian Keys", TargetType.PROFILE, "julian", createdAt = 1_790_900_000_000L, isRead = true),
        AppNotification("n3", NotificationType.COMMENT, "julian", "Julian Keys", TargetType.PROGRESSION, "p1", "Progression 1", "Nice voicing", 1_790_000_000_000L),
    )
}

class FakeProfileRepository(
    users: List<User> = listOf(SampleSocial.me, SampleSocial.julian),
    progressions: List<ChordProgression> = SampleDiscover.progressions.take(3).map { it.copy(authorId = "julian") },
) : ProfileRepository {
    val users = MutableStateFlow(users.associateBy { it.uid })
    val progressions = MutableStateFlow(progressions)
    var updateResult: Result<Unit> = Result.success(Unit)
    var lastUpdate: ProfileUpdate? = null

    override fun user(uid: String): Flow<User?> = users.map { it[uid] }

    override fun progressionsBy(uid: String): Flow<List<ChordProgression>> =
        progressions.map { list -> list.filter { it.authorId == uid } }

    override suspend fun updateProfile(update: ProfileUpdate): Result<Unit> = updateResult.onSuccess {
        lastUpdate = update
        users.update { all ->
            val me = all.getValue("me")
            all + ("me" to me.copy(fullName = update.fullName, username = update.username, bio = update.bio, instruments = update.instruments.toList(), genres = update.genres.toList()))
        }
    }

    override suspend fun uploadPhoto(uri: String): Result<String> {
        val url = "data:image/jpeg;base64,AAAA"
        users.update { all -> all + ("me" to all.getValue("me").copy(photoUrl = url)) }
        return Result.success(url)
    }
}

class FakeFollowRepository : FollowRepository {
    val following = MutableStateFlow(emptySet<String>())

    override fun isFollowing(uid: String): Flow<Boolean> = following.map { uid in it }

    override fun stats(uid: String): Flow<ProfileStats> =
        following.map { ProfileStats(followers = if (uid in it) 1 else 0, following = if (uid == "me") it.size else 0) }

    override suspend fun setFollowing(uid: String, follow: Boolean): Result<Unit> {
        following.update { if (follow) it + uid else it - uid }
        return Result.success(Unit)
    }
}

class FakeNotificationRepository(initial: List<AppNotification> = SampleSocial.notifications) : NotificationRepository {
    override val notifications = MutableStateFlow(initial)
    override val unreadCount: Flow<Int> = notifications.map { list -> list.count { !it.isRead } }

    override suspend fun markRead(id: String): Result<Unit> {
        notifications.update { list -> list.map { if (it.id == id) it.copy(isRead = true) else it } }
        return Result.success(Unit)
    }

    override suspend fun markAllRead(): Result<Unit> {
        notifications.update { list -> list.map { it.copy(isRead = true) } }
        return Result.success(Unit)
    }
}

class FakeSavedLibraryRepository : SavedLibraryRepository {
    val entries = MutableStateFlow(
        listOf(
            SavedEntry(SavedType.SONG, "golden-hour-glow", "Golden Hour Glow", "Koro Acoustic Trio", 2),
            SavedEntry(SavedType.CHORD, "Cmaj7", "Cmaj7", "", 1),
        ),
    )
    override val pendingCount = MutableStateFlow(0)
    override val lastSyncedAt = MutableStateFlow<Long?>(null)
    var syncCalls = 0

    override fun entries(type: SavedType): Flow<List<SavedEntry>> =
        entries.map { list -> list.filter { it.type == type }.sortedByDescending { it.savedAt } }

    override suspend fun syncNow(): Result<Int> {
        syncCalls++
        return Result.success(0)
    }
}

class FakeDraftRepository : DraftRepository {
    override val draft = MutableStateFlow<ProgressionDraft?>(null)

    override suspend fun saveDraft(draft: ProgressionDraft) {
        this.draft.value = draft
    }

    override suspend fun clearDraft() {
        draft.value = null
    }
}
