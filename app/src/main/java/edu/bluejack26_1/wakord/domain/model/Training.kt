package edu.bluejack26_1.wakord.domain.model

import androidx.annotation.StringRes

enum class SkillType { SINGLE_NOTE, INTERVAL, CHORD_QUALITY, PROGRESSION }

enum class LevelStatus { MASTERED, IN_PROGRESS, UNLOCKED, LOCKED }

data class TrainingLevel(val number: Int, @param:StringRes val titleRes: Int, val skill: SkillType)

data class LevelProgress(val level: TrainingLevel, val bestAccuracy: Float, val status: LevelStatus)

data class AnswerChoice(val id: String, @param:StringRes val labelRes: Int? = null, val text: String? = null)

data class Question(
    val skill: SkillType,
    val referenceMidi: Int,
    val sounds: List<List<Int>>,
    val choices: List<AnswerChoice>,
    val answerId: String,
    val semitones: Int? = null,
)

data class AnswerFeedback(
    val isCorrect: Boolean,
    val correct: AnswerChoice,
    @param:StringRes val explanationRes: Int,
    val explanationArgs: List<Any> = emptyList(),
)

data class TrainingSession(
    val id: String,
    val level: Int,
    val skill: SkillType,
    val correct: Int,
    val total: Int,
    val durationMs: Long,
    val startedAt: Long,
) {
    val accuracy: Float get() = if (total == 0) 0f else correct.toFloat() / total
}

data class DayStat(val epochDay: Long, val sessions: Int, val accuracy: Float, val minutes: Int)

data class DailyWorkout(
    val minutesToday: Int,
    val goalMinutes: Int,
    val trialsToday: Int,
    val accuracyToday: Float,
    val streakDays: Int,
    val currentLevel: Int,
)

data class AccuracyBucket(val startDay: Long, val accuracy: Float?)

data class AccuracySeries(val buckets: List<AccuracyBucket> = emptyList(), val change: Float? = null)

data class TrainingStats(
    val workout: DailyWorkout,
    val longestStreak: Int,
    val overallAccuracy: Float,
    val history: List<DayStat>,
    val accuracyBySkill: Map<SkillType, Float>,
    val levels: List<LevelProgress>,
    val daily: AccuracySeries = AccuracySeries(),
    val weekly: AccuracySeries = AccuracySeries(),
    val monthly: AccuracySeries = AccuracySeries(),
)

enum class PitchStatus { NO_SIGNAL, FLAT, ON_PITCH, SHARP }

data class PitchReading(
    val targetNote: String,
    val sungNote: String?,
    val frequencyHz: Float?,
    val cents: Int?,
    val status: PitchStatus,
)
