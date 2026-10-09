package edu.bluejack26_1.wakord.ui.feature.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.bluejack26_1.wakord.core.audio.TonePlayer
import edu.bluejack26_1.wakord.core.training.PitchMath
import edu.bluejack26_1.wakord.core.training.PitchSource
import edu.bluejack26_1.wakord.domain.repository.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PitchMonitorViewModel(
    private val pitchSource: PitchSource,
    private val tonePlayer: TonePlayer,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PitchMonitorUiState())
    val uiState: StateFlow<PitchMonitorUiState> = _uiState.asStateFlow()

    private var listenJob: Job? = null

    fun onAction(action: PitchMonitorAction) {
        when (action) {
            is PitchMonitorAction.SelectReference -> _uiState.update { it.copy(referenceMidi = action.midi, reading = null) }
            PitchMonitorAction.PlayReference -> viewModelScope.launch {
                tonePlayer.playNote(_uiState.value.referenceMidi, REFERENCE_MS, settingsRepository.referencePitch.first())
            }
            PitchMonitorAction.ToggleListening -> if (listenJob == null) startListening() else stopListening()
        }
    }

    private fun startListening() {
        _uiState.update { it.copy(isListening = true) }
        listenJob = viewModelScope.launch {
            val pitch = settingsRepository.referencePitch.first()
            pitchSource.frequencies().collect { hz ->
                _uiState.update { it.copy(reading = PitchMath.reading(hz, it.referenceMidi, pitch)) }
            }
        }
    }

    private fun stopListening() {
        listenJob?.cancel()
        listenJob = null
        _uiState.update { it.copy(isListening = false, reading = null) }
    }

    private companion object {
        const val REFERENCE_MS = 1500L
    }
}
