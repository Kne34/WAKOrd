package edu.bluejack26_1.wakord.ui.feature.tuner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bluejack26_1.wakord.WAKordApp
import edu.bluejack26_1.wakord.core.audio.AudioInput
import edu.bluejack26_1.wakord.core.audio.NoteMapper
import edu.bluejack26_1.wakord.core.audio.PitchDetector
import edu.bluejack26_1.wakord.core.audio.PitchResult
import edu.bluejack26_1.wakord.core.audio.TonePlayer
import edu.bluejack26_1.wakord.domain.model.StringMatcher
import edu.bluejack26_1.wakord.domain.model.TuneStatus
import edu.bluejack26_1.wakord.domain.model.Tuning
import edu.bluejack26_1.wakord.domain.model.TuningPresets
import edu.bluejack26_1.wakord.domain.repository.TuningRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs

class TunerViewModel(
    private val audioInput: AudioInput,
    private val pitchDetector: PitchDetector,
    private val tuningRepository: TuningRepository,
    private val tonePlayer: TonePlayer,
    referencePitch: Flow<Float>,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TunerUiState())
    val uiState: StateFlow<TunerUiState> = _uiState.asStateFlow()

    private var listenJob: Job? = null
    private var pitchPipeJob: Job? = null
    private val recentFrequencies = ArrayDeque<Float>()

    init {
        viewModelScope.launch {
            referencePitch.collect { pitch -> _uiState.update { it.copy(referencePitch = pitch) } }
        }
        viewModelScope.launch {
            tuningRepository.customTunings().collect { list -> _uiState.update { it.copy(customTunings = list) } }
        }
    }

    fun onAction(action: TunerAction) {
        when (action) {
            is TunerAction.PermissionResult -> onPermissionResult(action.granted)
            TunerAction.StopListening -> stopListening()
            is TunerAction.SelectInstrument -> _uiState.update {
                it.copy(instrument = action.instrument, tuning = TuningPresets.default(action.instrument), sheet = null).resetReading()
            }
            is TunerAction.SelectTuning -> _uiState.update {
                it.copy(instrument = action.tuning.instrument, tuning = action.tuning, sheet = null).resetReading()
            }
            is TunerAction.SelectString -> _uiState.update { it.copy(selectedString = action.index, autoDetect = false) }
            TunerAction.ToggleAuto -> _uiState.update {
                it.copy(autoDetect = !it.autoDetect, selectedString = it.activeString ?: it.selectedString)
            }
            TunerAction.UsePitchPipe -> _uiState.update { it.copy(pitchPipeMode = true) }
            is TunerAction.PlayPitchPipe -> playPitchPipe(action.index)
            is TunerAction.SaveCustomTuning -> saveCustomTuning(action.name, action.notes)
            is TunerAction.OpenSheet -> _uiState.update { it.copy(sheet = action.sheet) }
            TunerAction.CloseSheet -> _uiState.update { it.copy(sheet = null) }
        }
    }

    private fun onPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(
                micPermission = if (granted) MicPermission.GRANTED else MicPermission.DENIED,
                pitchPipeMode = if (granted) false else it.pitchPipeMode,
            )
        }
        if (granted) startListening()
    }

    private fun startListening() {
        if (listenJob?.isActive == true) return
        listenJob = viewModelScope.launch {
            audioInput.frames()
                .map { frame -> pitchDetector.detect(frame, audioInput.sampleRate) }
                .flowOn(computeDispatcher)
                .catch { _uiState.update { it.copy(micPermission = MicPermission.DENIED) } }
                .collect(::onPitch)
        }
    }

    private fun stopListening() {
        listenJob?.cancel()
        listenJob = null
        recentFrequencies.clear()
    }

    private fun onPitch(result: PitchResult?) {
        if (result == null || result.probability < MIN_PROBABILITY) return
        val frequency = smooth(result.frequencyHz)
        _uiState.update { s ->
            val index = if (s.autoDetect) {
                StringMatcher.closestString(frequency, s.tuning, s.referencePitch)
            } else {
                s.selectedString
            }
            val cents = StringMatcher.centsToString(frequency, s.tuning, index, s.referencePitch)
            val status = StringMatcher.statusOf(cents)
            s.copy(
                reading = NoteMapper.fromFrequency(frequency, s.referencePitch),
                activeString = index,
                centsOff = cents,
                status = status,
                tunedStrings = if (status == TuneStatus.IN_TUNE) s.tunedStrings + index else s.tunedStrings,
            )
        }
    }

    private fun smooth(frequency: Float): Float {
        val last = recentFrequencies.lastOrNull()
        if (last != null && abs(NoteMapper.centsBetween(frequency, last)) > 100f) recentFrequencies.clear()
        recentFrequencies.addLast(frequency)
        if (recentFrequencies.size > 5) recentFrequencies.removeFirst()
        return recentFrequencies.sorted()[recentFrequencies.size / 2]
    }

    private fun playPitchPipe(index: Int) {
        val state = _uiState.value
        val midi = NoteMapper.midiOf(state.tuning.notes[index])
        pitchPipeJob?.cancel()
        tonePlayer.stop()
        pitchPipeJob = viewModelScope.launch {
            _uiState.update { it.copy(pitchPipeString = index, selectedString = index) }
            tonePlayer.playNote(midi, durationMs = PITCH_PIPE_MS, referencePitch = state.referencePitch)
            _uiState.update { it.copy(pitchPipeString = null) }
        }
    }

    private fun saveCustomTuning(name: String, notes: List<String>) {
        val tuning = Tuning(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            instrument = _uiState.value.instrument,
            notes = notes,
            isCustom = true,
        )
        _uiState.update { it.copy(tuning = tuning, sheet = null).resetReading() }
        viewModelScope.launch { tuningRepository.saveCustomTuning(tuning) }
    }

    private fun TunerUiState.resetReading() = copy(
        reading = null,
        activeString = null,
        centsOff = 0f,
        status = TuneStatus.IDLE,
        tunedStrings = emptySet(),
        selectedString = 0,
    )

    override fun onCleared() {
        tonePlayer.stop()
    }

    companion object {
        private const val MIN_PROBABILITY = 0.8f
        private const val PITCH_PIPE_MS = 2000L

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as WAKordApp).container
                TunerViewModel(
                    audioInput = container.audioInput,
                    pitchDetector = container.pitchDetector,
                    tuningRepository = container.tuningRepository,
                    tonePlayer = container.tonePlayer,
                    referencePitch = container.settingsRepository.referencePitch,
                )
            }
        }
    }
}
