package edu.bluejack26_1.wakord.core.training

import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.core.music.NoteNames
import edu.bluejack26_1.wakord.domain.model.AnswerChoice
import edu.bluejack26_1.wakord.domain.model.AnswerFeedback
import edu.bluejack26_1.wakord.domain.model.Question
import edu.bluejack26_1.wakord.domain.model.SkillType
import edu.bluejack26_1.wakord.domain.model.TrainingLevel
import kotlin.random.Random

enum class ChordQuality(val intervals: List<Int>, val labelRes: Int) {
    MAJOR(listOf(0, 4, 7), R.string.quality_major),
    MINOR(listOf(0, 3, 7), R.string.quality_minor),
    DIMINISHED(listOf(0, 3, 6), R.string.quality_diminished),
    AUGMENTED(listOf(0, 4, 8), R.string.quality_augmented),
    MAJOR7(listOf(0, 4, 7, 11), R.string.quality_major7),
    MINOR7(listOf(0, 3, 7, 10), R.string.quality_minor7),
    DOMINANT7(listOf(0, 4, 7, 10), R.string.quality_dominant7),
    HALF_DIMINISHED(listOf(0, 3, 6, 10), R.string.quality_half_diminished),
}

class QuestionGenerator(private val random: Random = Random.Default) {
    fun questions(level: TrainingLevel, count: Int = Curriculum.QUESTIONS_PER_SET, reference: Int = 60): List<Question> =
        List(count) { question(level, reference) }

    fun question(level: TrainingLevel, reference: Int = 60): Question = when (level.number) {
        1 -> note(listOf(60, 64, 67), reference)
        2 -> note((60..71).toList(), reference)
        3 -> interval(listOf(3, 4, 7), reference)
        4 -> interval((1..12).toList(), reference)
        5 -> quality(listOf(ChordQuality.MAJOR, ChordQuality.MINOR), reference)
        6 -> quality(listOf(ChordQuality.MAJOR7, ChordQuality.MINOR7, ChordQuality.DOMINANT7, ChordQuality.HALF_DIMINISHED), reference)
        7 -> progression(BASIC_PROGRESSIONS, reference)
        else -> progression(JAZZ_PROGRESSIONS, reference)
    }

    private fun note(pool: List<Int>, reference: Int): Question {
        val target = pool.random(random)
        val options = pick(pool, target)
        return Question(
            skill = SkillType.SINGLE_NOTE,
            referenceMidi = reference,
            sounds = listOf(listOf(target)),
            choices = options.sorted().map { AnswerChoice(id = noteId(it), text = NoteNames.pretty(noteId(it))) },
            answerId = noteId(target),
        )
    }

    private fun interval(pool: List<Int>, reference: Int): Question {
        val semitones = pool.random(random)
        val root = random.nextInt(55, 68)
        return Question(
            skill = SkillType.INTERVAL,
            referenceMidi = reference,
            sounds = listOf(listOf(root), listOf(root + semitones)),
            choices = pick(pool, semitones).sorted().map { AnswerChoice(id = "i$it", labelRes = intervalLabel(it)) },
            answerId = "i$semitones",
            semitones = semitones,
        )
    }

    private fun quality(pool: List<ChordQuality>, reference: Int): Question {
        val quality = pool.random(random)
        val root = random.nextInt(55, 65)
        return Question(
            skill = SkillType.CHORD_QUALITY,
            referenceMidi = reference,
            sounds = listOf(quality.intervals.map { root + it }),
            choices = pool.map { AnswerChoice(id = it.name, labelRes = it.labelRes) },
            answerId = quality.name,
        )
    }

    private fun progression(pool: List<List<Int>>, reference: Int): Question {
        val pattern = pool.random(random)
        return Question(
            skill = SkillType.PROGRESSION,
            referenceMidi = reference,
            sounds = pattern.map { degree -> triad(degree) },
            choices = pool.map { AnswerChoice(id = roman(it), text = roman(it)) },
            answerId = roman(pattern),
        )
    }

    private fun <T> pick(pool: List<T>, answer: T): List<T> =
        (pool.filter { it != answer }.shuffled(random).take(MAX_CHOICES - 1) + answer).shuffled(random)

    companion object {
        private const val MAX_CHOICES = 4
        private val MAJOR_SCALE = listOf(0, 2, 4, 5, 7, 9, 11)
        private val NUMERALS = listOf("I", "ii", "iii", "IV", "V", "vi", "vii°")

        val BASIC_PROGRESSIONS = listOf(listOf(1, 4, 5, 1), listOf(1, 5, 4, 1), listOf(1, 4, 1, 5), listOf(1, 5, 1, 4))
        val JAZZ_PROGRESSIONS = listOf(listOf(2, 5, 1), listOf(1, 6, 2, 5), listOf(1, 6, 4, 5), listOf(6, 4, 1, 5))

        fun noteId(midi: Int): String = NoteNames.name(midi % 12, preferFlats = false) + (midi / 12 - 1)

        fun intervalLabel(semitones: Int): Int = when (semitones) {
            1 -> R.string.interval_1
            2 -> R.string.interval_2
            3 -> R.string.interval_3
            4 -> R.string.interval_4
            5 -> R.string.interval_5
            6 -> R.string.interval_6
            7 -> R.string.interval_7
            8 -> R.string.interval_8
            9 -> R.string.interval_9
            10 -> R.string.interval_10
            11 -> R.string.interval_11
            else -> R.string.interval_12
        }

        fun roman(pattern: List<Int>): String = pattern.joinToString("–") { NUMERALS[it - 1] }

        fun triad(degree: Int, tonic: Int = 60): List<Int> = (0..2).map { step ->
            val index = degree - 1 + step * 2
            tonic + MAJOR_SCALE[index % 7] + 12 * (index / 7)
        }
    }
}

object Grader {
    fun grade(question: Question, choiceId: String): AnswerFeedback {
        val correct = question.choices.first { it.id == question.answerId }
        val (explanation, args) = when (question.skill) {
            SkillType.SINGLE_NOTE -> R.string.explain_note to emptyList()
            SkillType.INTERVAL -> R.string.explain_interval to listOf<Any>(question.semitones ?: 0)
            SkillType.CHORD_QUALITY -> R.string.explain_quality to emptyList()
            SkillType.PROGRESSION -> R.string.explain_progression to emptyList()
        }
        return AnswerFeedback(choiceId == question.answerId, correct, explanation, args)
    }
}
