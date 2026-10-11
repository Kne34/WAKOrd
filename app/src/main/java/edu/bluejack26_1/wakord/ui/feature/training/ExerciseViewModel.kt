package edu.bluejack26_1.wakord.ui.feature.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bluejack26_1.wakord.WAKordApp
import edu.bluejack26_1.wakord.core.audio.TonePlayer
import edu.bluejack26_1.wakord.core.error.messageRes
import edu.bluejack26_1.wakord.core.training.Curriculum
import edu.bluejack26_1.wakord.core.training.Grader
import edu.bluejack26_1.wakord.core.training.QuestionGenerator
import edu.bluejack26_1.wakord.core.training.TrainingStatsCalculator
import edu.bluejack26_1.wakord.domain.model.LevelStatus
import edu.bluejack26_1.wakord.domain.model.Question
import edu.bluejack26_1.wakord.domain.model.TrainingSession
import edu.bluejack26_1.wakord.domain.repository.SettingsRepository
import edu.bluejack26_1.wakord.domain.repository.TrainingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.util.UUID

class ExerciseViewModel(
    levelNumber: Int,
    private val trainingRepository: TrainingRepository,
    private val settingsRepository: SettingsRepository,
    private val tonePlayer: TonePlayer,
    private val generator: QuestionGenerator = QuestionGenerator(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {
    private val level = Curriculum.level(levelNumber)
    private val _uiState = MutableStateFlow(ExerciseUiState(level = level))
    val uiState: StateFlow<ExerciseUiState> = _uiState.asStateFlow()

    private var questions: List<Question> = emptyList()
    private val answers = mutableListOf<Boolean>()
    private var startedAt = 0L
    private var playJob: Job? = null

    init {
        start()
    }

    fun onAction(action: ExerciseAction) {
        when (action) {
            ExerciseAction.HearReference -> play { tonePlayer.playNote(it.referenceMidi, NOTE_MS, referencePitch()) }
            ExerciseAction.PlayQuestion -> play { playSounds(it, 1) }
            ExerciseAction.PlaySlower -> play { playSounds(it, SLOW_FACTOR) }
            is ExerciseAction.Select -> _uiState.update { if (it.feedback == null) it.copy(selectedId = action.choiceId) else it }
            ExerciseAction.Check -> check()
            ExerciseAction.Next -> next()
            ExerciseAction.Retry -> start()
            ExerciseAction.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun start() {
        questions = generator.questions(level)
        answers.clear()
        startedAt = clock()
        _uiState.value = ExerciseUiState(level = level, total = questions.size, question = questions.first())
    }

    private fun play(block: suspend (Question) -> Unit) {
        val question = _uiState.value.question ?: return
        playJob?.cancel()
        tonePlayer.stop()
        playJob = viewModelScope.launch {
            _uiState.update { it.copy(isPlaying = true) }
            try {
                block(question)
            } finally {
                _uiState.update { it.copy(isPlaying = false) }
            }
        }
    }

    private suspend fun playSounds(question: Question, factor: Int) {
        val pitch = referencePitch()
        question.sounds.forEach { sound ->
            if (sound.size == 1) tonePlayer.playNote(sound.first(), NOTE_MS * factor, pitch)
            else tonePlayer.playChord(sound, CHORD_MS * factor, pitch)
        }
    }

    private suspend fun referencePitch(): Float = settingsRepository.referencePitch.first()

    private fun check() {
        val s = _uiState.value
        val question = s.question ?: return
        val selected = s.selectedId ?: return
        if (s.feedback != null) return
        val feedback = Grader.grade(question, selected)
        answers += feedback.isCorrect
        _uiState.update { it.copy(feedback = feedback, answers = answers.toList()) }
    }

    private fun next() {
        val s = _uiState.value
        if (s.feedback == null) return
        if (s.isLast) {
            finish()
        } else {
            val index = s.index + 1
            _uiState.update { it.copy(index = index, question = questions[index], selectedId = null, feedback = null) }
        }
    }

    private fun finish() {
        playJob?.cancel()
        tonePlayer.stop()
        viewModelScope.launch {
            val before = trainingRepository.sessions.first()
            val now = clock()
            val session = TrainingSession(
                id = UUID.randomUUID().toString(),
                level = level.number,
                skill = level.skill,
                correct = answers.count { it },
                total = answers.size,
                durationMs = now - startedAt,
                startedAt = startedAt,
            )
            val result = trainingRepository.save(session)
            val levelsBefore = TrainingStatsCalculator.levelProgress(before)
            val after = TrainingStatsCalculator.calculate(before + session, now, zone)
            val nextBefore = levelsBefore.firstOrNull { it.level.number == level.number + 1 }
            val nextAfter = after.levels.firstOrNull { it.level.number == level.number + 1 }
            val unlocked = nextAfter?.level?.takeIf { nextBefore?.status == LevelStatus.LOCKED && nextAfter.status != LevelStatus.LOCKED }
            _uiState.update {
                it.copy(
                    summary = SessionSummary(
                        correct = session.correct,
                        total = session.total,
                        accuracy = session.accuracy,
                        durationMs = session.durationMs,
                        streakDays = after.workout.streakDays,
                        answers = answers.toList(),
                        levelBefore = levelsBefore.first { p -> p.level.number == level.number }.bestAccuracy,
                        levelAfter = after.levels.first { p -> p.level.number == level.number }.bestAccuracy,
                        unlockedLevel = unlocked,
                    ),
                    errorMessage = result.exceptionOrNull()?.messageRes(),
                )
            }
        }
    }

    override fun onCleared() {
        tonePlayer.stop()
    }

    companion object {
        const val LEVEL = "level"
        private const val NOTE_MS = 900L
        private const val CHORD_MS = 1500L
        private const val SLOW_FACTOR = 2

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as WAKordApp).container
                ExerciseViewModel(
                    levelNumber = createSavedStateHandle().get<Int>(LEVEL) ?: 1,
                    trainingRepository = container.trainingRepository,
                    settingsRepository = container.settingsRepository,
                    tonePlayer = container.tonePlayer,
                )
            }
        }
    }
}
