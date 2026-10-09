package edu.bluejack26_1.wakord.core.training

import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.domain.model.SkillType
import edu.bluejack26_1.wakord.domain.model.TrainingLevel

object Curriculum {
    const val QUESTIONS_PER_SET = 10
    const val UNLOCK_ACCURACY = 0.8f
    const val DAILY_GOAL_MINUTES = 5

    val levels = listOf(
        TrainingLevel(1, R.string.level_notes_basic, SkillType.SINGLE_NOTE),
        TrainingLevel(2, R.string.level_notes_octave, SkillType.SINGLE_NOTE),
        TrainingLevel(3, R.string.level_intervals_basic, SkillType.INTERVAL),
        TrainingLevel(4, R.string.level_intervals_all, SkillType.INTERVAL),
        TrainingLevel(5, R.string.level_quality_triads, SkillType.CHORD_QUALITY),
        TrainingLevel(6, R.string.level_quality_sevenths, SkillType.CHORD_QUALITY),
        TrainingLevel(7, R.string.level_progressions_basic, SkillType.PROGRESSION),
        TrainingLevel(8, R.string.level_progressions_jazz, SkillType.PROGRESSION),
    )

    val referenceNotes = listOf(60, 64, 69, 72)

    fun level(number: Int): TrainingLevel = levels.first { it.number == number.coerceIn(1, levels.size) }
}
