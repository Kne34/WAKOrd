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
    private val voicingCache = mutableMapOf<String, List<Int>>()

    init {
        viewModelScope.launch {
            songRepository.song(songId).collect { song ->
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
        viewModelScope.launch { songRepository.isLiked(songId).collect { v -> _uiState.update { it.copy(isLiked = v) } } }
        viewModelScope.launch {
            savedItemsRepository.isSaved(SavedType.SONG, songId).collect { v -> _uiState.update { it.copy(isSaved = v) } }
        }
        viewModelScope.launch { commentRepository.comments(songId).collect { v -> _uiState.update { it.copy(comments = v) } } }
    }

    fun onAction(action: SongDetailAction) {
        when (action) {
            is SongDetailAction.Transpose -> transpose(_uiState.value.transposeSteps + action.steps)
            SongDetailAction.ResetTranspose -> transpose(0)
            SongDetailAction.TogglePlay -> if (_uiState.value.isPlaying) stopPlayback() else startPlayback()
            is SongDetailAction.SetSpeed -> _uiState.update { it.copy(playbackSpeed = action.speed) }
            SongDetailAction.ToggleLike -> toggleLike()
            SongDetailAction.ToggleSave -> toggleSave()
            is SongDetailAction.CommentChanged -> _uiState.update { it.copy(commentDraft = action.text) }
            is SongDetailAction.ReplyTo -> _uiState.update { it.copy(replyingTo = action.comment) }
            SongDetailAction.PostComment -> postComment()
            is SongDetailAction.ToggleCommentLike -> toggleCommentLike(action.comment)
            SongDetailAction.DismissCommentBlocked -> _uiState.update { it.copy(showCommentBlocked = false) }
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

    private fun startPlayback() {
        val song = _uiState.value.song ?: return
        val positions = chordPositions(song)
        if (positions.isEmpty()) return
        playJob = viewModelScope.launch {
            val chordMs = (60_000L * BEATS_PER_CHORD / song.tempo / _uiState.value.playbackSpeed).toLong()
            _uiState.update { it.copy(isPlaying = true, elapsedMs = 0, totalMs = chordMs * positions.size) }
            positions.forEachIndexed { index, position ->
                val chord = song.sections[position.section].lines[position.line].segments[position.segment].chord.orEmpty()
                _uiState.update {
                    it.copy(playingPosition = position, nextPosition = positions.getOrNull(index + 1), elapsedMs = chordMs * index)
                }
                tonePlayer.playChord(midisFor(chord), chordMs)
            }
            _uiState.update { it.copy(isPlaying = false, playingPosition = null, nextPosition = null, elapsedMs = it.totalMs) }
        }
    }

    private fun stopPlayback() {
        playJob?.cancel()
        playJob = null
        tonePlayer.stop()
        _uiState.update { it.copy(isPlaying = false, playingPosition = null, nextPosition = null) }
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
