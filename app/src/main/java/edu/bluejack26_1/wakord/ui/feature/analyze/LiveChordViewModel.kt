package edu.bluejack26_1.wakord.ui.feature.analyze

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bluejack26_1.wakord.WAKordApp
import edu.bluejack26_1.wakord.core.analysis.AnalysisInsights
import edu.bluejack26_1.wakord.core.analysis.ChordRecognizer
import edu.bluejack26_1.wakord.core.analysis.Chromagram
import edu.bluejack26_1.wakord.core.analysis.RomanNumerals
import edu.bluejack26_1.wakord.core.audio.AudioCapture
import edu.bluejack26_1.wakord.core.audio.TonePlayer
import edu.bluejack26_1.wakord.core.error.AppError
import edu.bluejack26_1.wakord.core.error.messageRes
import edu.bluejack26_1.wakord.core.music.Chord
import edu.bluejack26_1.wakord.core.music.ChordTheory
import edu.bluejack26_1.wakord.core.music.VoicingGenerator
import edu.bluejack26_1.wakord.domain.model.ChordTrackBar
import edu.bluejack26_1.wakord.domain.model.ProgressionDraft
import edu.bluejack26_1.wakord.domain.repository.ProgressionRepository
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

class LiveChordViewModel(
    private val audioCapture: AudioCapture,
    private val tonePlayer: TonePlayer,
    private val progressionRepository: ProgressionRepository,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LiveChordUiState())
    val uiState: StateFlow<LiveChordUiState> = _uiState.asStateFlow()

    private var listenJob: Job? = null
    private var playJob: Job? = null

    fun onAction(action: LiveChordAction) {
        when (action) {
            LiveChordAction.ToggleListening -> when {
                _uiState.value.isBuilding -> finishBuilding()
                listenJob == null -> start()
                else -> stop()
            }
            LiveChordAction.StartListening -> if (listenJob == null && !_uiState.value.isBuilding) start()
            LiveChordAction.StopListening -> stop()
            LiveChordAction.ToggleLock -> _uiState.update {
                if (it.isBuilding) it else it.copy(lockedReading = if (it.lockedReading == null) it.reading else null)
            }
            LiveChordAction.StartBuilding -> startBuilding()
            is LiveChordAction.ToggleKey -> _uiState.update {
                if (!it.isBuilding) it else built(it, if (action.midi in it.builtNotes) it.builtNotes - action.midi else it.builtNotes + action.midi)
            }
            LiveChordAction.ClearKeys -> _uiState.update { if (it.isBuilding) built(it, emptySet()) else it }
            LiveChordAction.FinishBuilding -> finishBuilding()
            is LiveChordAction.AddBar -> addBar(action.symbol)
            is LiveChordAction.RemoveBar -> setBars(_uiState.value.bars.filterIndexed { i, _ -> i != action.index }.map { it.symbol })
            is LiveChordAction.SetBpm -> _uiState.update { it.copy(bpm = action.bpm.coerceIn(MIN_TRACK_BPM, MAX_TRACK_BPM)) }
            LiveChordAction.PlayTrack -> if (playJob?.isActive == true) stopTrack() else playTrack()
            LiveChordAction.ToggleLoop -> _uiState.update { it.copy(isLooping = !it.isLooping) }
            is LiveChordAction.SaveAsProgression -> saveAsProgression(action.title)
            LiveChordAction.DismissError -> _uiState.update { it.copy(errorMessage = null) }
            LiveChordAction.MicrophoneDenied -> {
                stop()
                _uiState.update { it.copy(microphoneOff = true) }
            }
        }
    }

    private fun start() {
        _uiState.update { it.copy(isListening = true, microphoneOff = false) }
        var silentFrames = 0
        listenJob = viewModelScope.launch {
            audioCapture.frames(FRAME_SIZE)
                .catch { e ->
                    if (e is AppError.MicrophoneOff) _uiState.update { it.copy(microphoneOff = true) }
                }
                .collect { frame ->
                    val reading = withContext(computeDispatcher) { ChordRecognizer.recognize(frame, audioCapture.sampleRate) }
                    silentFrames = if (reading == null) silentFrames + 1 else 0
                    val level = Chromagram.rms(frame)
                    val settled = reading != null || silentFrames >= HOLD_FRAMES
                    _uiState.update { if (settled) it.copy(reading = reading, inputLevel = level) else it.copy(inputLevel = level) }
                }
            listenJob = null
            _uiState.update { it.copy(isListening = false, inputLevel = 0f) }
        }
    }

    private fun startBuilding() {
        if (_uiState.value.isBuilding) return
        stop()
        _uiState.update { s ->
            val seed = s.shownReading?.symbol?.let { VoicingGenerator.pianoNotes(it) }.orEmpty().toSet()
            built(s.copy(isBuilding = true), seed)
        }
    }

    private fun built(state: LiveChordUiState, notes: Set<Int>): LiveChordUiState =
        state.copy(builtNotes = notes, builtReading = ChordRecognizer.fromNotes(notes))

    private fun finishBuilding() {
        if (!_uiState.value.isBuilding) return
        _uiState.update { it.copy(isBuilding = false, builtNotes = emptySet(), builtReading = null) }
        if (listenJob == null) start()
    }

    private fun addBar(symbol: String) {
        val bars = _uiState.value.bars
        if (bars.size >= MAX_TRACK_BARS || Chord.parse(symbol) == null) return
        setBars(bars.map { it.symbol } + symbol)
    }

    private fun setBars(symbols: List<String>) {
        val key = symbols.firstOrNull()?.let(::keyOf)
        _uiState.update { s ->
            s.copy(
                bars = symbols.map { ChordTrackBar(it, if (key == null) "" else RomanNumerals.of(it, key)) },
                trackKey = key,
                savedProgressionId = null,
            )
        }
    }

    private fun keyOf(symbol: String): String? {
        val chord = Chord.parse(symbol) ?: return null
        val intervals = ChordTheory.intervals(chord.suffix)
        val minor = 3 in intervals && 4 !in intervals
        return chord.root + if (minor) "m" else ""
    }

    private fun playTrack() {
        if (_uiState.value.bars.isEmpty()) return
        playJob = viewModelScope.launch {
            _uiState.update { it.copy(isPlayingTrack = true) }
            try {
                do {
                    val s = _uiState.value
                    val barMs = 60_000L * BEATS_PER_BAR / s.bpm
                    s.bars.forEachIndexed { index, bar ->
                        _uiState.update { it.copy(playingBar = index) }
                        val midis = withContext(computeDispatcher) {
                            VoicingGenerator.voicings(bar.symbol, limit = 1).firstOrNull()?.let { VoicingGenerator.midiNotes(it) }.orEmpty()
                        }
                        tonePlayer.playChord(midis, barMs)
                    }
                } while (_uiState.value.isLooping && _uiState.value.bars.isNotEmpty())
            } finally {
                _uiState.update { it.copy(isPlayingTrack = false, playingBar = null) }
            }
        }
    }

    private fun stopTrack() {
        playJob?.cancel()
        playJob = null
        tonePlayer.stop()
        _uiState.update { it.copy(isPlayingTrack = false, playingBar = null) }
    }

    private fun saveAsProgression(title: String) {
        val s = _uiState.value
        val chords = s.bars.map { it.symbol }
        val key = s.trackKey
        if (title.isBlank() || chords.isEmpty() || key == null || s.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val draft = ProgressionDraft(
                title = title.trim(),
                key = key,
                bpm = s.bpm,
                genre = DEFAULT_GENRE,
                difficulty = AnalysisInsights.difficulty(chords),
                chords = chords,
            )
            progressionRepository.create(draft)
                .onSuccess { id -> _uiState.update { it.copy(isSaving = false, savedProgressionId = id) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, errorMessage = e.messageRes()) } }
        }
    }

    override fun onCleared() {
        tonePlayer.stop()
    }

    private fun stop() {
        listenJob?.cancel()
        listenJob = null
        _uiState.update { it.copy(isListening = false, inputLevel = 0f) }
    }

    companion object {
        private const val FRAME_SIZE = 4096
        private const val HOLD_FRAMES = 4
        private const val BEATS_PER_BAR = 4
        private const val DEFAULT_GENRE = "Pop"

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as WAKordApp).container
                LiveChordViewModel(container.audioCapture, container.tonePlayer, container.progressionRepository)
            }
        }
    }
}
