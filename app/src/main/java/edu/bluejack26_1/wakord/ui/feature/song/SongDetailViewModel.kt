package edu.bluejack26_1.wakord.ui.feature.song

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bluejack26_1.wakord.WAKordApp
import edu.bluejack26_1.wakord.core.audio.TonePlayer
import edu.bluejack26_1.wakord.core.error.AppError
import edu.bluejack26_1.wakord.core.error.messageRes
import edu.bluejack26_1.wakord.core.music.ChordTransposer
import edu.bluejack26_1.wakord.core.music.VoicingGenerator
import edu.bluejack26_1.wakord.domain.model.Comment
import edu.bluejack26_1.wakord.domain.model.SavedType
import edu.bluejack26_1.wakord.domain.model.Song
import edu.bluejack26_1.wakord.domain.repository.CommentRepository
import edu.bluejack26_1.wakord.domain.repository.SavedItemsRepository
import edu.bluejack26_1.wakord.domain.repository.SongRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SongDetailViewModel(
    private val songId: String,
    private val songRepository: SongRepository,
    private val commentRepository: CommentRepository,
    private val savedItemsRepository: SavedItemsRepository,
    private val tonePlayer: TonePlayer,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SongDetailUiState())
    val uiState: StateFlow<SongDetailUiState> = _uiState.asStateFlow()

    private var original: Song? = null
    private var playJob: Job? = null
    private var previewJob: Job? = null
    private val voicingCache = mutableMapOf<String, List<Int>>()

    private var loadJob: Job? = null

    init {
        load()
        viewModelScope.launch { songRepository.isLiked(songId).catch { }.collect { v -> _uiState.update { it.copy(isLiked = v) } } }
        viewModelScope.launch {
            savedItemsRepository.isSaved(SavedType.SONG, songId).catch { }.collect { v -> _uiState.update { it.copy(isSaved = v) } }
        }
        viewModelScope.launch { commentRepository.comments(songId).catch { }.collect { v -> _uiState.update { it.copy(comments = v) } } }
    }

    private fun load() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        loadJob = viewModelScope.launch {
            songRepository.song(songId).catch { e -> _uiState.update { it.copy(isLoading = false, loadError = e.messageRes()) } }.collect { song ->
                original = song
                _uiState.update { s ->
                    s.copy(
                        isLoading = false,
                        song = song?.let { ChordTransposer.transposeSong(it, s.transposeSteps) },
                        originalKey = song?.key.orEmpty(),
                        errorMessage = if (song == null) AppError.NotFound.messageRes() else s.errorMessage,
                    )
                }
            }
        }
    }

    fun onAction(action: SongDetailAction) {
        when (action) {
            is SongDetailAction.Transpose -> transpose(_uiState.value.transposeSteps + action.steps)
            SongDetailAction.ResetTranspose -> transpose(0)
            SongDetailAction.TogglePlay -> if (_uiState.value.isPlaying) pausePlayback() else startPlayback(currentIndex().coerceAtLeast(0))
            is SongDetailAction.SetSpeed -> _uiState.update { it.copy(playbackSpeed = action.speed) }
            is SongDetailAction.SkipChord -> skipChord(action.steps)
            SongDetailAction.ToggleLoop -> _uiState.update { it.copy(isLooping = !it.isLooping) }
            SongDetailAction.ToggleLike -> toggleLike()
            SongDetailAction.ToggleSave -> toggleSave()
            is SongDetailAction.CommentChanged -> _uiState.update { it.copy(commentDraft = action.text) }
            is SongDetailAction.ReplyTo -> _uiState.update { it.copy(replyingTo = action.comment) }
            SongDetailAction.PostComment -> postComment()
            is SongDetailAction.ToggleCommentLike -> toggleCommentLike(action.comment)
            SongDetailAction.DismissCommentBlocked -> _uiState.update { it.copy(showCommentBlocked = false) }
            SongDetailAction.Retry -> load()
            SongDetailAction.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun transpose(steps: Int) {
        val normalized = steps % 12
        stopPlayback()
        _uiState.update { s ->
            s.copy(transposeSteps = normalized, song = original?.let { ChordTransposer.transposeSong(it, normalized) })
        }
    }

    private fun chordMs(song: Song): Long =
        (60_000L * BEATS_PER_CHORD / song.tempo.coerceAtLeast(1) / _uiState.value.playbackSpeed).toLong()

    private fun currentIndex(): Int {
        val song = _uiState.value.song ?: return -1
        return chordPositions(song).indexOf(_uiState.value.playingPosition)
    }

    private fun nextOf(positions: List<ChordPosition>, index: Int): ChordPosition? =
        positions.getOrNull(index + 1) ?: if (_uiState.value.isLooping) positions.firstOrNull() else null

    private fun chordAt(song: Song, position: ChordPosition): String =
        song.sections[position.section].lines[position.line].segments[position.segment].chord.orEmpty()

    private fun startPlayback(from: Int) {
        val song = _uiState.value.song ?: return
        val positions = chordPositions(song)
        if (positions.isEmpty()) return
        cancelJobs()
        playJob = viewModelScope.launch {
            val chordMs = chordMs(song)
            var index = from.coerceIn(positions.indices)
            _uiState.update { it.copy(isPlaying = true, elapsedMs = chordMs * index, totalMs = chordMs * positions.size) }
            while (true) {
                val position = positions[index]
                _uiState.update {
                    it.copy(playingPosition = position, nextPosition = nextOf(positions, index), elapsedMs = chordMs * index)
                }
                tonePlayer.playChord(midisFor(chordAt(song, position)), chordMs)
                index++
                if (index >= positions.size) {
                    if (_uiState.value.isLooping) index = 0 else break
                }
            }
            _uiState.update { it.copy(isPlaying = false, playingPosition = null, nextPosition = null, elapsedMs = it.totalMs) }
        }
    }

    private fun pausePlayback() {
        cancelJobs()
        _uiState.update { it.copy(isPlaying = false) }
    }

    private fun stopPlayback() {
        cancelJobs()
        _uiState.update { it.copy(isPlaying = false, playingPosition = null, nextPosition = null, elapsedMs = 0) }
    }

    private fun cancelJobs() {
        playJob?.cancel()
        playJob = null
        previewJob?.cancel()
        previewJob = null
        tonePlayer.stop()
    }

    private fun skipChord(steps: Int) {
        val song = _uiState.value.song ?: return
        val positions = chordPositions(song)
        if (positions.isEmpty() || steps == 0) return
        val current = currentIndex()
        val raw = if (current < 0) (if (steps > 0) 0 else positions.lastIndex) else current + steps
        val target = if (_uiState.value.isLooping) Math.floorMod(raw, positions.size) else raw.coerceIn(positions.indices)
        if (_uiState.value.isPlaying) {
            startPlayback(target)
            return
        }
        cancelJobs()
        val chordMs = chordMs(song)
        val position = positions[target]
        _uiState.update {
            it.copy(
                playingPosition = position,
                nextPosition = nextOf(positions, target),
                elapsedMs = chordMs * target,
                totalMs = chordMs * positions.size,
            )
        }
        previewJob = viewModelScope.launch { tonePlayer.playChord(midisFor(chordAt(song, position)), chordMs) }
    }

    private suspend fun midisFor(chord: String): List<Int> = voicingCache.getOrPut(chord) {
        withContext(computeDispatcher) {
            VoicingGenerator.voicings(chord, limit = 1).firstOrNull()?.let { VoicingGenerator.midiNotes(it) }.orEmpty()
        }
    }

    private fun toggleLike() {
        val liked = !_uiState.value.isLiked
        _uiState.update { it.copy(isLiked = liked) }
        viewModelScope.launch {
            songRepository.setLiked(songId, liked).onFailure { e ->
                _uiState.update { it.copy(isLiked = !liked, errorMessage = e.messageRes()) }
            }
        }
    }

    private fun toggleSave() {
        val saved = !_uiState.value.isSaved
        _uiState.update { it.copy(isSaved = saved) }
        viewModelScope.launch {
            savedItemsRepository.setSaved(SavedType.SONG, songId, saved).onFailure { e ->
                _uiState.update { it.copy(isSaved = !saved, errorMessage = e.messageRes()) }
            }
        }
    }

    private fun postComment() {
        val s = _uiState.value
        val text = s.commentDraft.trim()
        if (text.isEmpty() || s.isPosting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isPosting = true) }
            val result = commentRepository.post(songId, text, s.replyingTo?.id)
            _uiState.update { state ->
                result.fold(
                    onSuccess = { state.copy(isPosting = false, commentDraft = "", replyingTo = null) },
                    onFailure = { e ->
                        if (e is AppError.CommentBlocked) state.copy(isPosting = false, showCommentBlocked = true)
                        else state.copy(isPosting = false, errorMessage = e.messageRes())
                    },
                )
            }
        }
    }

    private fun toggleCommentLike(comment: Comment) {
        viewModelScope.launch {
            commentRepository.setLiked(songId, comment.id, !comment.likedByMe).onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.messageRes()) }
            }
        }
    }

    override fun onCleared() {
        tonePlayer.stop()
    }

    companion object {
        const val SONG_ID = "songId"
        private const val BEATS_PER_CHORD = 2

        fun chordPositions(song: Song): List<ChordPosition> = buildList {
            song.sections.forEachIndexed { s, section ->
                section.lines.forEachIndexed { l, line ->
                    line.segments.forEachIndexed { g, segment -> if (segment.chord != null) add(ChordPosition(s, l, g)) }
                }
            }
        }

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as WAKordApp).container
                val handle: SavedStateHandle = createSavedStateHandle()
                SongDetailViewModel(
                    songId = requireNotNull(handle[SONG_ID]),
                    songRepository = container.songRepository,
                    commentRepository = container.commentRepository,
                    savedItemsRepository = container.savedItemsRepository,
                    tonePlayer = container.tonePlayer,
                )
            }
        }
    }
}
