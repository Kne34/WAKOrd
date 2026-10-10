package edu.bluejack26_1.wakord.ui.feature.analyze

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bluejack26_1.wakord.WAKordApp
import edu.bluejack26_1.wakord.core.analysis.ChordRecognizer
import edu.bluejack26_1.wakord.core.audio.AudioCapture
import edu.bluejack26_1.wakord.core.error.AppError
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
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LiveChordUiState())
    val uiState: StateFlow<LiveChordUiState> = _uiState.asStateFlow()

    private var listenJob: Job? = null

    fun onAction(action: LiveChordAction) {
        when (action) {
            LiveChordAction.ToggleListening -> if (listenJob == null) start() else stop()
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
                    if (reading != null || silentFrames >= HOLD_FRAMES) _uiState.update { it.copy(reading = reading) }
                }
            listenJob = null
            _uiState.update { it.copy(isListening = false) }
        }
    }

    private fun stop() {
        listenJob?.cancel()
        listenJob = null
        _uiState.update { it.copy(isListening = false) }
    }

    companion object {
        private const val FRAME_SIZE = 4096
        private const val HOLD_FRAMES = 4

        val Factory = viewModelFactory {
            initializer {
                LiveChordViewModel((this[APPLICATION_KEY] as WAKordApp).container.audioCapture)
            }
        }
    }
}
