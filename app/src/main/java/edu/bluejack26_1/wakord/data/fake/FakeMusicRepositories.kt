package edu.bluejack26_1.wakord.data.fake

import edu.bluejack26_1.wakord.core.error.AppError
import edu.bluejack26_1.wakord.core.music.ChordProParser
import edu.bluejack26_1.wakord.domain.model.ChordProgression
import edu.bluejack26_1.wakord.domain.model.Comment
import edu.bluejack26_1.wakord.domain.model.Difficulty
import edu.bluejack26_1.wakord.domain.model.SavedType
import edu.bluejack26_1.wakord.domain.model.Song
import edu.bluejack26_1.wakord.domain.model.SongSection
import edu.bluejack26_1.wakord.domain.repository.CommentRepository
import edu.bluejack26_1.wakord.domain.repository.ProgressionRepository
import edu.bluejack26_1.wakord.domain.repository.SavedItemsRepository
import edu.bluejack26_1.wakord.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

object SampleMusic {
    val song = Song(
        id = "subtle-resonance",
        title = "Subtle Resonance",
        artist = "Alex Vance ft. Koro",
        key = "Eb",
        capo = 1,
        tempo = 84,
        difficulty = Difficulty.INTERMEDIATE,
        genre = "Neo-Soul",
        likeCount = 2400,
        saveCount = 310,
        sections = listOf(
            SongSection("Intro", listOf(ChordProParser.parseLine("[Ebmaj7] [Bb7sus4] [Cm7] [Abmaj7]"))),
            SongSection(
                "Verse 1",
                listOf(
                    ChordProParser.parseLine("[Ebmaj7]Streetlights hum a [Bb7sus4]quiet tune"),
                    ChordProParser.parseLine("[Cm7]Coffee cooling [Abmaj7]in the room"),
                ),
            ),
        ),
    )

    val progression = ChordProgression(
        id = "neo-soul-04",
        title = "Neo-Soul Progression #04",
        key = "C",
        bpm = 84,
        genre = "Neo-Soul",
        difficulty = Difficulty.ADVANCED,
        chords = listOf("Dm9", "G13", "Cmaj9"),
        authorId = "julian",
        authorName = "Julian Keys",
        playCount = 1800,
        likeCount = 342,
        saveCount = 89,
    )
}

class FakeSongRepository(initial: Song? = SampleMusic.song) : SongRepository {
    val songs = MutableStateFlow(listOfNotNull(initial).associateBy { it.id })
    val liked = MutableStateFlow(emptySet<String>())
    var likeResult: Result<Unit> = Result.success(Unit)

    override fun song(id: String): Flow<Song?> = songs.map { it[id] }
    override fun isLiked(id: String): Flow<Boolean> = liked.map { id in it }

    override suspend fun setLiked(id: String, liked: Boolean): Result<Unit> = likeResult.onSuccess {
        this.liked.update { if (liked) it + id else it - id }
    }
}

class FakeProgressionRepository(initial: ChordProgression? = SampleMusic.progression) : ProgressionRepository {
    val progressions = MutableStateFlow(listOfNotNull(initial).associateBy { it.id })
    val liked = MutableStateFlow(emptySet<String>())
    val plays = mutableListOf<String>()

    override fun progression(id: String): Flow<ChordProgression?> = progressions.map { it[id] }
    override fun isLiked(id: String): Flow<Boolean> = liked.map { id in it }

    override suspend fun setLiked(id: String, liked: Boolean): Result<Unit> {
        this.liked.update { if (liked) it + id else it - id }
        return Result.success(Unit)
    }

    override suspend fun recordPlay(id: String): Result<Unit> {
        plays += id
        return Result.success(Unit)
    }
}

class FakeCommentRepository : CommentRepository {
    private val comments = MutableStateFlow<Map<String, List<Comment>>>(emptyMap())
    var blockedWord: String? = null

    override fun comments(targetId: String): Flow<List<Comment>> = comments.map { it[targetId].orEmpty() }

    override suspend fun post(targetId: String, text: String, parentId: String?): Result<Unit> {
        val word = blockedWord
        if (word != null && text.contains(word, ignoreCase = true)) return Result.failure(AppError.CommentBlocked)
        comments.update { all ->
            val list = all[targetId].orEmpty()
            val comment = Comment(id = "c${list.size + 1}", authorId = "me", authorName = "Me", text = text, parentId = parentId)
            all + (targetId to list + comment)
        }
        return Result.success(Unit)
    }

    override suspend fun setLiked(targetId: String, commentId: String, liked: Boolean): Result<Unit> {
        comments.update { all ->
            all + (targetId to all[targetId].orEmpty().map {
                if (it.id != commentId || it.likedByMe == liked) it
                else it.copy(likedByMe = liked, likeCount = it.likeCount + if (liked) 1 else -1)
            })
        }
        return Result.success(Unit)
    }
}

class FakeSavedItemsRepository : SavedItemsRepository {
    val saved = MutableStateFlow(emptySet<Pair<SavedType, String>>())

    override fun isSaved(type: SavedType, id: String): Flow<Boolean> = saved.map { (type to id) in it }

    override suspend fun setSaved(type: SavedType, id: String, saved: Boolean): Result<Unit> {
        this.saved.update { if (saved) it + (type to id) else it - (type to id) }
        return Result.success(Unit)
    }
}
