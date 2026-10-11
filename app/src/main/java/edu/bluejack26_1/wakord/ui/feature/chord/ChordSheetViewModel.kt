package edu.bluejack26_1.wakord.ui.feature.chord

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bluejack26_1.wakord.WAKordApp
import edu.bluejack26_1.wakord.core.audio.TonePlayer
import edu.bluejack26_1.wakord.core.error.messageRes
import edu.bluejack26_1.wakord.core.music.VoicingGenerator
import edu.bluejack26_1.wakord.domain.model.ChordVoicing
import edu.bluejack26_1.wakord.domain.model.SavedType
import edu.bluejack26_1.wakord.domain.repository.SavedItemsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChordSheetUiState(
    val symbol: String? = null,
    val voicings: List<ChordVoicing> = emptyList(),
    val selectedVoicing: Int = 0,
    val isSaved: Boolean = false,
    @param:StringRes val errorMessage: Int? = null,
)

sealed interface ChordSheetAction {
    data class Open(val symbol: String) : ChordSheetAction
    data object Close : ChordSheetAction
    data class SelectVoicing(val index: Int) : ChordSheetAction
    data object Preview : ChordSheetAction
    data class PlayNote(val midi: Int) : ChordSheetAction
    data class PlayChord(val midis: List<Int>) : ChordSheetAction
    data object ToggleSaved : ChordSheetAction
    data object DismissError : ChordSheetAction
}

class ChordSheetViewModel(
    private val savedItemsRepository: SavedItemsRepository,
    private val tonePlayer: TonePlayer,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChordSheetUiState())
    val uiState: StateFlow<ChordSheetUiState> = _uiState.asStateFlow()

    private var openJob: Job? = null
    private var savedJob: Job? = null

    fun onAction(action: ChordSheetAction) {
        when (action) {
            is ChordSheetAction.Open -> open(action.symbol)
            ChordSheetAction.Close -> close()
            is ChordSheetAction.SelectVoicing -> _uiState.update {
                it.copy(selectedVoicing = action.index.coerceIn(0, (it.voicings.size - 1).coerceAtLeast(0)))
            }
            ChordSheetAction.Preview -> preview()
            is ChordSheetAction.PlayNote -> viewModelScope.launch { tonePlayer.playNote(action.midi, NOTE_MS) }
            is ChordSheetAction.PlayChord -> viewModelScope.launch { tonePlayer.playChord(action.midis, PREVIEW_MS) }
            ChordSheetAction.ToggleSaved -> toggleSaved()
            ChordSheetAction.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun open(symbol: String) {
        openJob?.cancel()
        savedJob?.cancel()
        _uiState.update { ChordSheetUiState(symbol = symbol) }
        openJob = viewModelScope.launch {
            val voicings = withContext(computeDispatcher) { VoicingGenerator.voicings(symbol) }
            _uiState.update { if (it.symbol == symbol) it.copy(voicings = voicings, selectedVoicing = 0) else it }
        }
        savedJob = viewModelScope.launch {
            savedItemsRepository.isSaved(SavedType.CHORD, symbol).collect { saved ->
                _uiState.update { if (it.symbol == symbol) it.copy(isSaved = saved) else it }
            }
        }
    }

    private fun close() {
        openJob?.cancel()
        savedJob?.cancel()
        tonePlayer.stop()
        _uiState.update { ChordSheetUiState() }
    }

    private fun preview() {
        val s = _uiState.value
        val voicing = s.voicings.getOrNull(s.selectedVoicing) ?: return
        viewModelScope.launch { tonePlayer.playChord(VoicingGenerator.midiNotes(voicing), PREVIEW_MS) }
    }

    private fun toggleSaved() {
        val symbol = _uiState.value.symbol ?: return
        val saved = !_uiState.value.isSaved
        _uiState.update { it.copy(isSaved = saved) }
        viewModelScope.launch {
            savedItemsRepository.setSaved(SavedType.CHORD, symbol, saved).onFailure { e ->
                _uiState.update { it.copy(isSaved = !saved, errorMessage = e.messageRes()) }
            }
        }
    }

    override fun onCleared() {
        tonePlayer.stop()
    }

    companion object {
        private const val PREVIEW_MS = 1500L
        private const val NOTE_MS = 900L

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as WAKordApp).container
                ChordSheetViewModel(container.savedItemsRepository, container.tonePlayer)
            }
        }
    }
}
