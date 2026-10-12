package edu.bluejack26_1.wakord.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bluejack26_1.wakord.WAKordApp
import edu.bluejack26_1.wakord.core.audio.PlaybackController
import edu.bluejack26_1.wakord.core.discover.FeedRanker
import edu.bluejack26_1.wakord.core.discover.Recommender
import edu.bluejack26_1.wakord.core.error.messageRes
import edu.bluejack26_1.wakord.core.music.VoicingGenerator
import edu.bluejack26_1.wakord.core.training.TrainingStatsCalculator
import edu.bluejack26_1.wakord.domain.model.ChordProgression
import edu.bluejack26_1.wakord.domain.model.FeedItem
import edu.bluejack26_1.wakord.domain.model.FeedPage
import edu.bluejack26_1.wakord.domain.model.SavedType
import edu.bluejack26_1.wakord.domain.model.Song
import edu.bluejack26_1.wakord.domain.model.User
import edu.bluejack26_1.wakord.domain.model.UserActivity
import edu.bluejack26_1.wakord.domain.repository.FeedRepository
import edu.bluejack26_1.wakord.domain.repository.NotificationRepository
import edu.bluejack26_1.wakord.domain.repository.ProgressionRepository
import edu.bluejack26_1.wakord.domain.repository.SavedItemsRepository
import edu.bluejack26_1.wakord.domain.repository.TrainingRepository
import edu.bluejack26_1.wakord.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId
import kotlin.math.roundToInt

class HomeViewModel(
    private val userRepository: UserRepository,
    private val feedRepository: FeedRepository,
    private val savedItemsRepository: SavedItemsRepository,
    private val progressionRepository: ProgressionRepository,
    private val playbackController: PlaybackController,
    private val trainingRepository: TrainingRepository,
    private val notificationRepository: NotificationRepository,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var user: User? = null
    private var songs: List<Song> = emptyList()
    private var affinity: Map<String, Int> = emptyMap()
    private var cursor: Long? = null
    private val shownSongIds = mutableSetOf<String>()
    private var loadJob: Job? = null
    private val baseLikes = mutableMapOf<String, Pair<Int, Boolean>>()

    init {
        viewModelScope.launch {
            userRepository.currentUser().collect { u ->
                user = u
                _uiState.update { it.copy(userName = u?.fullName.orEmpty(), userId = u?.uid.orEmpty(), photoUrl = u?.photoUrl) }
            }
        }
        viewModelScope.launch {
            savedItemsRepository.saved(SavedType.CHORD).collect { symbols ->
                val chords = withContext(computeDispatcher) {
                    symbols.take(RECENT_CHORDS).map { SavedChord(it, VoicingGenerator.voicings(it, limit = 1).firstOrNull()) }
                }
                _uiState.update { it.copy(savedChords = chords) }
            }
        }
        viewModelScope.launch {
            feedRepository.likedIds().collect { ids -> applyLikes(ids) }
        }
        viewModelScope.launch {
            notificationRepository.unreadCount.collect { count -> _uiState.update { it.copy(unreadNotifications = count) } }
        }
        viewModelScope.launch {
            trainingRepository.sessions.collect { sessions ->
                val w = TrainingStatsCalculator.calculate(sessions, clock(), zone).workout
                val workout = DailyWorkout(
                    minutesDone = w.minutesToday,
                    minutesGoal = w.goalMinutes,
                    streakDays = w.streakDays,
                    level = w.currentLevel,
                    accuracy = if (w.trialsToday > 0) (w.accuracyToday * 100).roundToInt() else null,
                )
                _uiState.update { it.copy(workout = workout) }
            }
        }
        refresh()
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.LoadMore -> loadMore()
            HomeAction.Refresh -> refresh()
            is HomeAction.PlayProgression -> play(action.progression)
            is HomeAction.ToggleProgressionLike -> toggleLike(action.progression)
            HomeAction.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, feed = emptyList(), endReached = false, errorMessage = null) }
            cursor = null
            shownSongIds.clear()
            baseLikes.clear()
            user = userRepository.currentUser().first()
            val result = feedRepository.songs().mapCatching { all ->
                songs = all
                all to feedRepository.progressionsPage(null, PAGE_SIZE).getOrThrow()
            }
            result.onSuccess { (all, firstPage) ->
                val activity = feedRepository.activity().getOrDefault(UserActivity())
                affinity = Recommender.genreAffinity(all, firstPage.progressions, activity)
                _uiState.update { it.copy(recommended = Recommender.recommendSongs(all, user, activity, affinity)) }
                appendPage(firstPage)
            }.onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.messageRes()) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun loadMore() {
        val s = _uiState.value
        if (s.isLoading || s.isLoadingMore || s.endReached) return
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            feedRepository.progressionsPage(cursor, PAGE_SIZE)
                .onSuccess { appendPage(it) }
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.messageRes()) } }
            _uiState.update { it.copy(isLoadingMore = false) }
        }
    }

    private suspend fun appendPage(page: FeedPage) {
        val isLast = page.nextCursor == null
        val remainingSongs = songs.filter { it.id !in shownSongIds }.map { FeedItem.SongItem(it) }
        val pageSongs = FeedRanker.rank(remainingSongs, user, affinity).let { if (isLast) it else it.take(SONGS_PER_PAGE) }
        shownSongIds += pageSongs.map { it.id }
        val items: List<FeedItem> = page.progressions.map { FeedItem.ProgressionItem(it) } + pageSongs
        val counts = feedRepository.commentCounts(items.map { it.id }).getOrDefault(emptyMap())
        val withCounts = items.map { item ->
            val count = counts[item.id] ?: 0
            when (item) {
                is FeedItem.SongItem -> item.copy(commentCount = count)
                is FeedItem.ProgressionItem -> item.copy(commentCount = count)
            }
        }
        cursor = page.nextCursor
        val liked = _uiState.value.likedIds
        withCounts.forEach { item -> baseLikes.getOrPut(item.id) { item.likeCount to (item.id in liked) } }
        _uiState.update { it.copy(feed = it.feed + FeedRanker.rank(withCounts, user, affinity), endReached = isLast) }
    }

    private fun applyLikes(ids: Set<String>) {
        _uiState.update { state ->
            state.copy(
                likedIds = ids,
                feed = state.feed.map { item ->
                    if (item !is FeedItem.ProgressionItem) return@map item
                    val (count, likedAtLoad) = baseLikes[item.id] ?: return@map item
                    val adjusted = count + (if (item.id in ids) 1 else 0) - (if (likedAtLoad) 1 else 0)
                    item.copy(progression = item.progression.copy(likeCount = adjusted.coerceAtLeast(0)))
                },
            )
        }
    }

    private fun toggleLike(progression: ChordProgression) {
        val before = _uiState.value.likedIds
        val liked = progression.id !in before
        applyLikes(if (liked) before + progression.id else before - progression.id)
        viewModelScope.launch {
            progressionRepository.setLiked(progression.id, liked).onFailure { e ->
                applyLikes(before)
                _uiState.update { it.copy(errorMessage = e.messageRes()) }
            }
        }
    }

    private fun play(progression: ChordProgression) {
        playbackController.play(progression)
        viewModelScope.launch { progressionRepository.recordPlay(progression.id) }
    }

    companion object {
        const val PAGE_SIZE = 10
        private const val SONGS_PER_PAGE = 2
        private const val RECENT_CHORDS = 6

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as WAKordApp).container
                HomeViewModel(
                    userRepository = container.userRepository,
                    feedRepository = container.feedRepository,
                    savedItemsRepository = container.savedItemsRepository,
                    progressionRepository = container.progressionRepository,
                    playbackController = container.playbackController,
                    trainingRepository = container.trainingRepository,
                    notificationRepository = container.notificationRepository,
                )
            }
        }
    }
}
