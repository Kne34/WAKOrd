package edu.bluejack26_1.wakord.domain.model

data class ProfileStats(val followers: Int = 0, val following: Int = 0)

data class ProfileUpdate(
    val fullName: String,
    val username: String,
    val bio: String,
    val instruments: Set<Instrument>,
    val genres: Set<String>,
)

data class SavedEntry(
    val type: SavedType,
    val id: String,
    val title: String,
    val subtitle: String,
    val savedAt: Long,
    val isPending: Boolean = false,
)

enum class NotificationType { LIKE, COMMENT, REPLY, FOLLOW }

enum class TargetType { SONG, PROGRESSION, PROFILE }

data class AppNotification(
    val id: String,
    val type: NotificationType,
    val actorId: String,
    val actorName: String,
    val targetType: TargetType,
    val targetId: String,
    val targetTitle: String = "",
    val message: String = "",
    val createdAt: Long = 0,
    val isRead: Boolean = false,
)
