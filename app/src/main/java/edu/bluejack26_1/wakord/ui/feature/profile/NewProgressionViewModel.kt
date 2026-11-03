package edu.bluejack26_1.wakord.ui.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bluejack26_1.wakord.WAKordApp
import edu.bluejack26_1.wakord.core.audio.TonePlayer
import edu.bluejack26_1.wakord.core.error.messageRes
import edu.bluejack26_1.wakord.core.music.Chord
import edu.bluejack26_1.wakord.core.music.DiatonicChords
import edu.bluejack26_1.wakord.core.music.VoicingGenerator
import edu.bluejack26_1.wakord.domain.model.ProgressionDraft
import edu.bluejack26_1.wakord.domain.repository.DraftRepository
import edu.bluejack26_1.wakord.domain.repository.ProgressionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NewProgressionViewModel(
    private val progressionRepository: ProgressionRepository,
    private val draftRepository: DraftRepository,
    private val tonePlayer: TonePlayer,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NewProgressionUiState(suggestedChords = DiatonicChords.of("C")))
    val uiState: StateFlow<NewProgressionUiState> = _uiState.asStateFlow()

    private var draftJob: Job? = null
    private var previewJob: Job? = null

    init {
        viewModelScope.launch {
            draftRepository.draft.first()?.let { d ->
                _uiState.update {
                    it.copy(
                        title = d.title,
                        key = d.key,
                        genre = d.genre.ifEmpty { it.genre },
                        difficulty = d.difficulty,
                        bpm = d.bpm.toString(),
                        chords = d.chords,
                        description = d.description,
                        suggestedChords = DiatonicChords.of(d.key),
                    )
                }
            }
        }
    }

    fun onAction(action: NewProgressionAction) {
        when (action) {
            is NewProgressionAction.TitleChanged -> edit { it.copy(title = action.value, errors = it.errors - DraftError.TITLE_REQUIRED) }
            is NewProgressionAction.KeyChanged -> edit { it.copy(key = action.value, suggestedChords = DiatonicChords.of(action.value)) }
            is NewProgressionAction.GenreChanged -> edit { it.copy(genre = action.value) }
            is NewProgressionAction.DifficultyChanged -> edit { it.copy(difficulty = action.value) }
            is NewProgressionAction.BpmChanged -> edit { it.copy(bpm = action.value.filter(Char::isDigit).take(3), errors = it.errors - DraftError.INVALID_BPM) }
            is NewProgressionAction.AddChord -> Chord.parse(action.symbol)?.let { chord ->
                edit { it.copy(chords = it.chords + chord.symbol, errors = it.errors - DraftError.TOO_FEW_CHORDS) }
            }
            is NewProgressionAction.RemoveChord -> edit { s -> s.copy(chords = s.chords.filterIndexed { i, _ -> i != action.index }) }
            is NewProgressionAction.DescriptionChanged -> edit { it.copy(description = action.value.take(MAX_DESCRIPTION)) }
            NewProgressionAction.TogglePreview -> if (_uiState.value.isPreviewing) stopPreview() else preview()
            NewProgressionAction.Upload -> upload()
            NewProgressionAction.SaveDraft -> saveDraft()
            NewProgressionAction.DismissUploaded -> _uiState.update { it.copy(uploadedId = null) }
            NewProgressionAction.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun edit(change: (NewProgressionUiState) -> NewProgressionUiState) {
        _uiState.update(change)
        draftJob?.cancel()
        draftJob = viewModelScope.launch {
            delay(DRAFT_DELAY_MS)
            draftRepository.saveDraft(_uiState.value.toDraft())
        }
    }

    private fun saveDraft() {
        draftJob?.cancel()
        stopPreview()
        viewModelScope.launch {
            draftRepository.saveDraft(_uiState.value.toDraft())
            _uiState.update { it.copy(isDraftSaved = true) }
        }
    }

    private fun NewProgressionUiState.toDraft() = ProgressionDraft(
        title = title,
        key = key,
        bpm = bpm.toIntOrNull() ?: 0,
        genre = genre,
        difficulty = difficulty,
        chords = chords,
        description = description,
    )

    private fun validate(s: NewProgressionUiState): Set<DraftError> = buildSet {
        if (s.title.isBlank()) add(DraftError.TITLE_REQUIRED)
        if (s.chords.size < MIN_CHORDS) add(DraftError.TOO_FEW_CHORDS)
        if ((s.bpm.toIntOrNull() ?: 0) !in BPM_RANGE) add(DraftError.INVALID_BPM)
    }

    private fun upload() {
        val s = _uiState.value
        if (s.isUploading) return
        val errors = validate(s)
        _uiState.update { it.copy(errors = errors) }
        if (errors.isNotEmpty()) return
        stopPreview()
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true) }
            progressionRepository.create(s.toDraft())
                .onSuccess { id ->
                    draftJob?.cancel()
                    draftRepository.clearDraft()
                    _uiState.value = NewProgressionUiState(suggestedChords = DiatonicChords.of("C"), uploadedId = id)
                }
                .onFailure { e -> _uiState.update { it.copy(isUploading = false, errorMessage = e.messageRes()) } }
        }
    }

    private fun preview() {
        val s = _uiState.value
        if (s.chords.isEmpty()) return
        val bpm = (s.bpm.toIntOrNull() ?: 0).takeIf { it in BPM_RANGE } ?: DEFAULT_BPM
        previewJob = viewModelScope.launch {
            _uiState.update { it.copy(isPreviewing = true) }
            s.chords.forEachIndexed { index, chord ->
                _uiState.update { it.copy(previewIndex = index) }
                val midis = withContext(computeDispatcher) {
                    VoicingGenerator.voicings(chord, limit = 1).firstOrNull()?.let { VoicingGenerator.midiNotes(it) }.orEmpty()
                }
                tonePlayer.playChord(midis, 60_000L * 4 / bpm)
            }
            _uiState.update { it.copy(isPreviewing = false, previewIndex = null) }
        }
    }

    private fun stopPreview() {
        previewJob?.cancel()
        previewJob = null
        tonePlayer.stop()
        _uiState.update { it.copy(isPreviewing = false, previewIndex = null) }
    }

    override fun onCleared() {
        tonePlayer.stop()
    }

    companion object {
        private const val MIN_CHORDS = 2
        private const val MAX_DESCRIPTION = 300
        private const val DEFAULT_BPM = 90
        private const val DRAFT_DELAY_MS = 500L
        private val BPM_RANGE = 40..240

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as WAKordApp).container
                NewProgressionViewModel(container.progressionRepository, container.draftRepository, container.tonePlayer)
            }
        }
    }
}
