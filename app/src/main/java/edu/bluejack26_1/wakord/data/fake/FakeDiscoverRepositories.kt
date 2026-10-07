package edu.bluejack26_1.wakord.data.fake

import edu.bluejack26_1.wakord.core.audio.PlaybackController
import edu.bluejack26_1.wakord.core.audio.PlaybackState
import edu.bluejack26_1.wakord.core.error.AppError
import edu.bluejack26_1.wakord.domain.model.ChordProgression
import edu.bluejack26_1.wakord.domain.model.Difficulty
import edu.bluejack26_1.wakord.domain.model.FeedPage
import edu.bluejack26_1.wakord.domain.model.Song
import edu.bluejack26_1.wakord.domain.model.User
import edu.bluejack26_1.wakord.domain.model.UserActivity
import edu.bluejack26_1.wakord.domain.repository.FeedRepository
import edu.bluejack26_1.wakord.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object SampleDiscover {
    val songs = listOf(
        SampleMusic.song,
        Song(id = "golden-hour-glow", title = "Golden Hour Glow", artist = "Koro Acoustic Trio", key = "G", genre = "Acoustic", likeCount = 980),
        Song(id = "midnight-waves", title = "Midnight Waves", artist = "Neon Mirage", key = "E", genre = "Pop", likeCount = 1830),
    )

    val progressions = (1..25).map { n ->
        ChordProgression(
            id = "p$n",
            title = "Progression $n",
            key = "C",
            bpm = 80,
            genre = listOf("Neo-Soul", "Lo-fi", "Jazz", "Pop")[n % 4],
            difficulty = Difficulty.entries[n % 3],
            chords = listOf("Cmaj7", "Am7", "Dm7", "G7"),
            authorId = "seed",
            authorName = "Julian Keys",
            likeCount = n * 3,
            createdAt = 1_790_000_000_000L + n * 60_000L,
        )
    }
}

class FakeFeedRepository(
    var songList: List<Song> = SampleDiscover.songs,
    var progressionList: List<ChordProgression> = SampleDiscover.progressions,
    var userActivity: UserActivity = UserActivity(),
) : FeedRepository {
    var failNextPage = false
    val requestedCursors = mutableListOf<Long?>()

    override suspend fun songs(): Result<List<Song>> = Result.success(songList)

    override suspend fun progressionsPage(before: Long?, pageSize: Int): Result<FeedPage> {
        requestedCursors += before
        if (failNextPage) {
            failNextPage = false
            return Result.failure(AppError.Network)
        }
        val page = progressionList
            .sortedByDescending { it.createdAt }
            .filter { before == null || it.createdAt < before }
            .take(pageSize)
        val cursor = if (page.size == pageSize) page.last().createdAt else null
        return Result.success(FeedPage(page, cursor))
    }

    override suspend fun commentCounts(ids: List<String>): Result<Map<String, Int>> =
        Result.success(ids.associateWith { 2 })

    override suspend fun activity(): Result<UserActivity> = Result.success(userActivity)
}

class FakeUserRepository(initial: User? = User(uid = "demo", fullName = "Keane Wilson", email = "keane.wilson@mail.com")) : UserRepository {
    val user = MutableStateFlow(initial)
    override fun currentUser(): Flow<User?> = user
}

class FakePlaybackController : PlaybackController {
    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()
    val played = mutableListOf<String>()

    override fun play(progression: ChordProgression) {
        played += progression.id
        _state.value = PlaybackState(progression, isPlaying = true, chordIndex = 0)
    }

    override fun toggle() {
        _state.update { if (it.progression == null) it else it.copy(isPlaying = !it.isPlaying) }
    }

    override fun pause() {
        _state.update { it.copy(isPlaying = false, chordIndex = null) }
    }

    override fun close() {
        _state.value = PlaybackState()
    }
}
