package edu.bluejack26_1.wakord.ui.feature.progression

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bluejack26_1.wakord.WAKordApp
import edu.bluejack26_1.wakord.core.audio.PlaybackController
import edu.bluejack26_1.wakord.core.audio.TonePlayer
import edu.bluejack26_1.wakord.core.error.AppError
import edu.bluejack26_1.wakord.core.error.messageRes
import edu.bluejack26_1.wakord.core.music.VoicingGenerator
import edu.bluejack26_1.wakord.domain.model.ChordProgression
import edu.bluejack26_1.wakord.domain.model.ChordVoicing
import edu.bluejack26_1.wakord.domain.model.Comment
import edu.bluejack26_1.wakord.domain.model.SavedType
import edu.bluejack26_1.wakord.domain.repository.CommentRepository
import edu.bluejack26_1.wakord.domain.repository.ProgressionRepository
import edu.bluejack26_1.wakord.domain.repository.SavedItemsRepository
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

class ProgressionDetailViewModel(
    private val progressionId: String,
    private val progressionRepository: ProgressionRepository,
    private val commentRepository: CommentRepository,
    private val savedItemsRepository: SavedItemsRepository,
    private val tonePlayer: TonePlayer,
    private val playbackController: PlaybackController,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProgressionDetailUiState())
    val uiState: StateFlow<ProgressionDetailUiState> = _uiState.asStateFlow()

    private var chordSavedJob: Job? = null

    private var loadJob: Job? = null

    init {
        load()
        viewModelScope.launch { progressionRepository.isLiked(progressionId).catch { }.collect { v -> _uiState.update { it.copy(isLiked = v) } } }
        viewModelScope.launch {
            savedItemsRepository.isSaved(SavedType.PROGRESSION, progressionId).catch { }.collect { v -> _uiState.update { it.copy(isSaved = v) } }
        }
        viewModelScope.launch { commentRepository.comments(progressionId).catch { }.collect { v -> _uiState.update { it.copy(comments = v) } } }
        viewModelScope.launch {
            playbackController.state.collect { playback ->
                val mine = playback.progression?.id == progressionId
                _uiState.update { it.copy(isPlaying = mine && playback.isPlaying, playingIndex = if (mine) playback.chordIndex else null) }
            }
        }
    }

    private fun load() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        loadJob = viewModelScope.launch {
            progressionRepository.progression(progressionId)
                .catch { e -> _uiState.update { it.copy(isLoading = false, loadError = e.messageRes()) } }
                .collect { progression ->
                val chords = progression?.chords.orEmpty()
                val (guitar, ukulele) = withContext(computeDispatcher) {
                    diagrams(chords, VoicingGenerator.GUITAR_STANDARD) to diagrams(chords, VoicingGenerator.UKULELE_STANDARD)
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        progression = progression,
                        chordDiagrams = guitar,
                        ukuleleDiagrams = ukulele,
                        errorMessage = if (progression == null) AppError.NotFound.messageRes() else it.errorMessage,
                    )
                }
            }
        }
    }

    fun onAction(action: ProgressionDetailAction) {
        when (action) {
            ProgressionDetailAction.TogglePlay -> if (_uiState.value.isPlaying) stopPlayback() else startPlayback()
            is ProgressionDetailAction.OpenChord -> openChord(action.symbol, action.tuning)
            ProgressionDetailAction.CloseChord -> {
                chordSavedJob?.cancel()
                _uiState.update {
                    it.copy(selectedChord = null, voicings = emptyList(), selectedVoicing = 0, chordTuning = VoicingGenerator.GUITAR_STANDARD)
                }
            }
            is ProgressionDetailAction.SelectVoicing -> _uiState.update { it.copy(selectedVoicing = action.index) }
            ProgressionDetailAction.PreviewChord -> previewChord()
            ProgressionDetailAction.ToggleChordSaved -> toggleChordSaved()
            ProgressionDetailAction.ToggleLike -> toggleLike()
            ProgressionDetailAction.ToggleSave -> toggleSave()
            is ProgressionDetailAction.CommentChanged -> _uiState.update { it.copy(commentDraft = action.text) }
            ProgressionDetailAction.PostComment -> postComment()
            is ProgressionDetailAction.ToggleCommentLike -> toggleCommentLike(action.comment)
            ProgressionDetailAction.DismissCommentBlocked -> _uiState.update { it.copy(showCommentBlocked = false) }
            ProgressionDetailAction.Retry -> load()
            ProgressionDetailAction.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun startPlayback() {
        val progression: ChordProgression = _uiState.value.progression ?: return
        playbackController.play(progression)
        viewModelScope.launch { progressionRepository.recordPlay(progressionId) }
    }

    private fun stopPlayback() {
        playbackController.pause()
    }

    private fun diagrams(chords: List<String>, tuning: List<Int>): List<ChordVoicing?> =
        chords.map { VoicingGenerator.voicings(it, tuning, limit = 1).firstOrNull() }

    private fun openChord(symbol: String, tuning: List<Int>) {
        viewModelScope.launch {
            val voicings = withContext(computeDispatcher) { VoicingGenerator.voicings(symbol, tuning) }
            _uiState.update { it.copy(selectedChord = symbol, voicings = voicings, selectedVoicing = 0, chordTuning = tuning) }
        }
        chordSavedJob?.cancel()
        chordSavedJob = viewModelScope.launch {
            savedItemsRepository.isSaved(SavedType.CHORD, symbol).catch { }.collect { v -> _uiState.update { it.copy(isChordSaved = v) } }
        }
    }

    private fun previewChord() {
        val s = _uiState.value
        val voicing = s.voicings.getOrNull(s.selectedVoicing) ?: return
        viewModelScope.launch { tonePlayer.playChord(VoicingGenerator.midiNotes(voicing, s.chordTuning), 1500) }
    }

    private fun toggleChordSaved() {
        val symbol = _uiState.value.selectedChord ?: return
        val saved = !_uiState.value.isChordSaved
        _uiState.update { it.copy(isChordSaved = saved) }
        viewModelScope.launch {
            savedItemsRepository.setSaved(SavedType.CHORD, symbol, saved).onFailure { e ->
                _uiState.update { it.copy(isChordSaved = !saved, errorMessage = e.messageRes()) }
            }
        }
    }

    private fun toggleLike() {
        val liked = !_uiState.value.isLiked
        _uiState.update { it.copy(isLiked = liked) }
        viewModelScope.launch {
            progressionRepository.setLiked(progressionId, liked).onFailure { e ->
                _uiState.update { it.copy(isLiked = !liked, errorMessage = e.messageRes()) }
            }
        }
    }

    private fun toggleSave() {
        val saved = !_uiState.value.isSaved
        _uiState.update { it.copy(isSaved = saved) }
        viewModelScope.launch {
            savedItemsRepository.setSaved(SavedType.PROGRESSION, progressionId, saved).onFailure { e ->
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
            val result = commentRepository.post(progressionId, text)
            _uiState.update { state ->
                result.fold(
                    onSuccess = { state.copy(isPosting = false, commentDraft = "") },
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
            commentRepository.setLiked(progressionId, comment.id, !comment.likedByMe).onFailure { e ->
                _uiState.update { it.copy(errorMessage = e.messageRes()) }
            }
        }
    }

    companion object {
        const val PROGRESSION_ID = "progressionId"

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as WAKordApp).container
                ProgressionDetailViewModel(
                    progressionId = requireNotNull(createSavedStateHandle()[PROGRESSION_ID]),
                    progressionRepository = container.progressionRepository,
                    commentRepository = container.commentRepository,
                    savedItemsRepository = container.savedItemsRepository,
                    tonePlayer = container.tonePlayer,
                    playbackController = container.playbackController,
                )
            }
        }
    }
}
