package edu.bluejack26_1.wakord.ui.feature.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bluejack26_1.wakord.WAKordApp
import edu.bluejack26_1.wakord.core.training.Curriculum
import edu.bluejack26_1.wakord.core.training.TrainingStatsCalculator
import edu.bluejack26_1.wakord.domain.model.LevelStatus
import edu.bluejack26_1.wakord.domain.repository.TrainingRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId

class TrainingViewModel(
    private val trainingRepository: TrainingRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(TrainingUiState())
    val uiState: StateFlow<TrainingUiState> = _uiState.asStateFlow()

    private val _effects = Channel<TrainingEffect>(Channel.BUFFERED)
    val effects: Flow<TrainingEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            trainingRepository.refreshFromRemote()
            trainingRepository.syncPending()
        }
        viewModelScope.launch {
            trainingRepository.sessions.collect { sessions ->
                val stats = TrainingStatsCalculator.calculate(sessions, clock(), zone)
                _uiState.update { it.copy(isLoading = false, stats = stats) }
            }
        }
    }

    fun onAction(action: TrainingAction) {
        when (action) {
            is TrainingAction.OpenLevel -> openLevel(action.level)
            TrainingAction.Resume -> openLevel(_uiState.value.stats?.workout?.currentLevel ?: 1)
            TrainingAction.DismissLockedLevel -> _uiState.update { it.copy(lockedLevel = null) }
            TrainingAction.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun openLevel(number: Int) {
        val progress = _uiState.value.levels.firstOrNull { it.level.number == number }
        if (progress?.status == LevelStatus.LOCKED) {
            _uiState.update { it.copy(lockedLevel = Curriculum.level(number)) }
        } else {
            viewModelScope.launch { _effects.send(TrainingEffect.StartExercise(number)) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                TrainingViewModel((this[APPLICATION_KEY] as WAKordApp).container.trainingRepository)
            }
        }
    }
}
