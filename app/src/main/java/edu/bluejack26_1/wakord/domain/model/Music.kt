package edu.bluejack26_1.wakord.domain.model

enum class Difficulty { EASY, INTERMEDIATE, ADVANCED }

data class ChordSegment(val chord: String?, val lyric: String)

data class SongLine(val segments: List<ChordSegment>)

data class SongSection(val name: String, val lines: List<SongLine>)

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val key: String,
    val capo: Int = 0,
    val tempo: Int = 90,
    val difficulty: Difficulty = Difficulty.EASY,
    val genre: String = "",
    val likeCount: Int = 0,
    val saveCount: Int = 0,
    val sections: List<SongSection> = emptyList(),
)

data class ChordProgression(
    val id: String,
    val title: String,
    val key: String,
    val bpm: Int,
    val genre: String,
    val difficulty: Difficulty,
    val chords: List<String>,
    val authorId: String,
    val authorName: String,
    val description: String = "",
    val playCount: Int = 0,
    val likeCount: Int = 0,
    val saveCount: Int = 0,
)

data class Comment(
    val id: String,
    val authorId: String,
    val authorName: String,
    val text: String,
    val parentId: String? = null,
    val likeCount: Int = 0,
    val likedByMe: Boolean = false,
    val createdAt: Long = 0,
)

data class ChordVoicing(
    val frets: List<Int>,
    val fingers: List<Int>,
    val baseFret: Int,
    val position: Int,
)

enum class SavedType(val path: String) {
    SONG("songs"),
    CHORD("chords"),
    PROGRESSION("progressions"),
}
