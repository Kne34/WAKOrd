package edu.bluejack26_1.wakord.domain.model

sealed interface FeedItem {
    val id: String
    val genre: String
    val likeCount: Int
    val commentCount: Int

    data class SongItem(val song: Song, override val commentCount: Int = 0) : FeedItem {
        override val id: String get() = song.id
        override val genre: String get() = song.genre
        override val likeCount: Int get() = song.likeCount
    }

    data class ProgressionItem(val progression: ChordProgression, override val commentCount: Int = 0) : FeedItem {
        override val id: String get() = progression.id
        override val genre: String get() = progression.genre
        override val likeCount: Int get() = progression.likeCount
    }
}

data class FeedPage(val progressions: List<ChordProgression>, val nextCursor: Long?)

data class UserActivity(
    val likedIds: Set<String> = emptySet(),
    val savedIds: Set<String> = emptySet(),
    val playedIds: Set<String> = emptySet(),
)
