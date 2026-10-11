package edu.bluejack26_1.wakord.ui.feature.training

import androidx.annotation.StringRes
import edu.bluejack26_1.wakord.domain.model.AnswerFeedback
import edu.bluejack26_1.wakord.domain.model.LevelProgress
import edu.bluejack26_1.wakord.domain.model.PitchReading
import edu.bluejack26_1.wakord.domain.model.Question
import edu.bluejack26_1.wakord.domain.model.TrainingLevel
import edu.bluejack26_1.wakord.domain.model.TrainingStats

data class TrainingUiState(
    val isLoading: Boolean = true,
    val stats: TrainingStats? = null,
    val lockedLevel: TrainingLevel? = null,
    @param:StringRes val errorMessage: Int? = null,
) {
    val levels: List<LevelProgress> get() = stats?.levels.orEmpty()
}

sealed interface TrainingAction {
    data class OpenLevel(val level: Int) : TrainingAction
    data object Resume : TrainingAction
    data object DismissLockedLevel : TrainingAction
    data object DismissError : TrainingAction
}

sealed interface TrainingEffect {
    data class StartExercise(val level: Int) : TrainingEffect
}

data class SessionSummary(
    val correct: Int,
    val total: Int,
    val accuracy: Float,
    val durationMs: Long,
    val streakDays: Int,
    val answers: List<Boolean>,
    val levelBefore: Float,
    val levelAfter: Float,
    val unlockedLevel: TrainingLevel?,
)

data class ExerciseUiState(
    val level: TrainingLevel? = null,
    val index: Int = 0,
    val total: Int = 0,
    val question: Question? = null,
    val selectedId: String? = null,
    val feedback: AnswerFeedback? = null,
    val isPlaying: Boolean = false,
    val summary: SessionSummary? = null,
    @param:StringRes val errorMessage: Int? = null,
) {
    val canCheck: Boolean get() = selectedId != null && feedback == null
    val isLast: Boolean get() = index == total - 1
}

sealed interface ExerciseAction {
    data object HearReference : ExerciseAction
    data object PlayQuestion : ExerciseAction
    data object PlaySlower : ExerciseAction
    data class Select(val choiceId: String) : ExerciseAction
    data object Check : ExerciseAction
    data object Next : ExerciseAction
    data object Retry : ExerciseAction
    data object DismissError : ExerciseAction
}

data class PitchMonitorUiState(
    val referenceMidi: Int = 60,
    val referencePitch: Float = 440f,
    val isListening: Boolean = false,
    val reading: PitchReading? = null,
    val microphoneOff: Boolean = false,
)

sealed interface PitchMonitorAction {
    data class SelectReference(val midi: Int) : PitchMonitorAction
    data object PlayReference : PitchMonitorAction
    data object ToggleListening : PitchMonitorAction
    data object MicrophoneDenied : PitchMonitorAction
}
