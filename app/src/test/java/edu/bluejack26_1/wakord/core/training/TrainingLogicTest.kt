package edu.bluejack26_1.wakord.core.training

import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.domain.model.LevelStatus
import edu.bluejack26_1.wakord.domain.model.PitchStatus
import edu.bluejack26_1.wakord.domain.model.SkillType
import edu.bluejack26_1.wakord.domain.model.TrainingSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset
import kotlin.random.Random

class TrainingLogicTest {
    private val generator = QuestionGenerator(Random(7))
    private val day = 86_400_000L
    private val now = 1_791_100_000_000L

    private fun session(level: Int, correct: Int, daysAgo: Int = 0, minutes: Int = 2) = TrainingSession(
        id = "s$level-$daysAgo-$correct",
        level = level,
        skill = Curriculum.level(level).skill,
        correct = correct,
        total = 10,
        durationMs = minutes * 60_000L,
        startedAt = now - daysAgo * day,
    )

    @Test
    fun `every level makes valid questions with the answer among the choices`() {
        Curriculum.levels.forEach { level ->
            generator.questions(level, count = 20).forEach { q ->
                assertEquals(level.skill, q.skill)
                assertTrue(q.choices.any { it.id == q.answerId })
                assertTrue(q.choices.size in 2..4)
                assertEquals(q.choices.size, q.choices.map { it.id }.toSet().size)
                assertTrue(q.sounds.isNotEmpty())
            }
        }
    }

    @Test
    fun `interval questions play two notes the right distance apart`() {
        repeat(20) {
            val q = generator.question(Curriculum.level(4))
            val (first, second) = q.sounds.map { it.single() }
            assertEquals(q.semitones, second - first)
            assertEquals("i${q.semitones}", q.answerId)
        }
    }

    @Test
    fun `progression triads follow the major scale`() {
        assertEquals(listOf(60, 64, 67), QuestionGenerator.triad(1))
        assertEquals(listOf(62, 65, 69), QuestionGenerator.triad(2))
        assertEquals(listOf(67, 71, 74), QuestionGenerator.triad(5))
        assertEquals("ii–V–I", QuestionGenerator.roman(listOf(2, 5, 1)))
    }

    @Test
    fun `grader explains intervals with semitones`() {
        val q = generator.question(Curriculum.level(3))
        val right = Grader.grade(q, q.answerId)
        assertTrue(right.isCorrect)
        assertEquals(R.string.explain_interval, right.explanationRes)
        assertEquals(listOf<Any>(q.semitones!!), right.explanationArgs)
        val wrongId = q.choices.first { it.id != q.answerId }.id
        assertFalse(Grader.grade(q, wrongId).isCorrect)
        assertEquals(q.answerId, Grader.grade(q, wrongId).correct.id)
    }

    @Test
    fun `eighty percent unlocks the next level`() {
        val below = TrainingStatsCalculator.levelProgress(listOf(session(1, 7)))
        assertEquals(LevelStatus.IN_PROGRESS, below[0].status)
        assertEquals(LevelStatus.LOCKED, below[1].status)

        val passed = TrainingStatsCalculator.levelProgress(listOf(session(1, 7), session(1, 8)))
        assertEquals(LevelStatus.MASTERED, passed[0].status)
        assertEquals(0.8f, passed[0].bestAccuracy)
        assertEquals(LevelStatus.UNLOCKED, passed[1].status)
        assertEquals(LevelStatus.LOCKED, passed[2].status)
        assertEquals(2, TrainingStatsCalculator.currentLevel(passed))
    }

    @Test
    fun `streak counts consecutive days and survives until today ends`() {
        val sessions = listOf(session(1, 9, 1), session(1, 9, 2), session(1, 9, 3), session(1, 9, 5), session(1, 9, 6))
        val stats = TrainingStatsCalculator.calculate(sessions, now, ZoneOffset.UTC)
        assertEquals(3, stats.workout.streakDays)
        assertEquals(3, stats.longestStreak)
        assertEquals(0, stats.workout.trialsToday)

        val withToday = TrainingStatsCalculator.calculate(sessions + session(2, 5), now, ZoneOffset.UTC)
        assertEquals(4, withToday.workout.streakDays)
        assertEquals(4, withToday.longestStreak)
    }

    @Test
    fun `daily numbers, history and accuracy by skill`() {
        val sessions = listOf(session(1, 8, minutes = 3), session(3, 6, minutes = 2), session(1, 10, 2))
        val stats = TrainingStatsCalculator.calculate(sessions, now, ZoneOffset.UTC)
        assertEquals(5, stats.workout.minutesToday)
        assertEquals(20, stats.workout.trialsToday)
        assertEquals(0.7f, stats.workout.accuracyToday)
        assertEquals(14, stats.history.size)
        assertEquals(2, stats.history.last().sessions)
        assertEquals(0.9f, stats.accuracyBySkill[SkillType.SINGLE_NOTE])
        assertEquals(0.6f, stats.accuracyBySkill[SkillType.INTERVAL])
        assertEquals(0.8f, stats.overallAccuracy)
    }

    @Test
    fun `pitch reading reports note, cents and status`() {
        val a4 = PitchMath.reading(440f, 69)
        assertEquals("A4", a4.sungNote)
        assertEquals(0, a4.cents)
        assertEquals(PitchStatus.ON_PITCH, a4.status)

        val flat = PitchMath.reading(415.3f, 69)
        assertEquals("G#4", flat.sungNote)
        assertEquals(-100, flat.cents)
        assertEquals(PitchStatus.FLAT, flat.status)

        assertEquals(PitchStatus.SHARP, PitchMath.reading(270f, 60).status)
        assertEquals(0, PitchMath.reading(442f, 69, referencePitch = 442f).cents)

        val silent = PitchMath.reading(null, 60)
        assertEquals(PitchStatus.NO_SIGNAL, silent.status)
        assertNull(silent.sungNote)
        assertEquals("C4", silent.targetNote)
    }
}
