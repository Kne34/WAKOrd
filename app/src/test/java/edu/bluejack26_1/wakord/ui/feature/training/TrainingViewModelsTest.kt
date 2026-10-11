package edu.bluejack26_1.wakord.ui.feature.training

import edu.bluejack26_1.wakord.core.error.AppError
import edu.bluejack26_1.wakord.core.training.Curriculum
import edu.bluejack26_1.wakord.core.training.PitchSource
import edu.bluejack26_1.wakord.core.training.QuestionGenerator
import edu.bluejack26_1.wakord.data.fake.FakeSettingsRepository
import edu.bluejack26_1.wakord.data.fake.FakeTrainingRepository
import edu.bluejack26_1.wakord.domain.model.PitchStatus
import edu.bluejack26_1.wakord.domain.model.SkillType
import edu.bluejack26_1.wakord.domain.model.TrainingSession
import edu.bluejack26_1.wakord.testutil.MainDispatcherRule
import edu.bluejack26_1.wakord.testutil.RecordingTonePlayer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.ZoneOffset
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class TrainingViewModelsTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val now = 1_791_100_000_000L
    private val settings = FakeSettingsRepository()
    private val player = RecordingTonePlayer()

    private fun passed(level: Int) = TrainingSession("p$level", level, Curriculum.level(level).skill, 9, 10, 120_000, now - 86_400_000L)

    private fun exercise(repository: FakeTrainingRepository, level: Int = 1, clock: () -> Long = { now }) =
        ExerciseViewModel(level, repository, settings, player, QuestionGenerator(Random(3)), clock, ZoneOffset.UTC)

    @Test
    fun `locked level shows popup and open level starts exercise`() = runTest(main.dispatcher) {
        val vm = TrainingViewModel(FakeTrainingRepository(listOf(passed(1))), { now }, ZoneOffset.UTC)
        advanceUntilIdle()
        vm.onAction(TrainingAction.OpenLevel(3))
        assertEquals(3, vm.uiState.value.lockedLevel?.number)
        vm.onAction(TrainingAction.DismissLockedLevel)
        assertNull(vm.uiState.value.lockedLevel)

        vm.onAction(TrainingAction.Resume)
        advanceUntilIdle()
        assertEquals(TrainingEffect.StartExercise(2), vm.effects.first())
        assertEquals(1, vm.uiState.value.stats?.workout?.streakDays)
    }

    @Test
    fun `answer every question then save the session with a summary`() = runTest(main.dispatcher) {
        val repository = FakeTrainingRepository()
        var time = now
        val vm = exercise(repository, clock = { time })
        repeat(Curriculum.QUESTIONS_PER_SET) {
            val q = vm.uiState.value.question!!
            val pick = if (it < 8) q.answerId else q.choices.first { c -> c.id != q.answerId }.id
            vm.onAction(ExerciseAction.Check)
            assertNull(vm.uiState.value.feedback)
            vm.onAction(ExerciseAction.Select(pick))
            vm.onAction(ExerciseAction.Check)
            assertEquals(it < 8, vm.uiState.value.feedback?.isCorrect)
            time += 15_000
            vm.onAction(ExerciseAction.Next)
        }
        advanceUntilIdle()
        val summary = vm.uiState.value.summary!!
        assertEquals(8, summary.correct)
        assertEquals(0.8f, summary.accuracy)
        assertEquals(150_000L, summary.durationMs)
        assertEquals(2, summary.unlockedLevel?.number)
        assertEquals(0f, summary.levelBefore)
        assertEquals(0.8f, summary.levelAfter)
        assertEquals(1, summary.streakDays)
        val saved = repository.sessions.value.single()
        assertEquals(SkillType.SINGLE_NOTE, saved.skill)
        assertEquals(10, saved.total)
    }

    @Test
    fun `selection is locked after checking`() = runTest(main.dispatcher) {
        val vm = exercise(FakeTrainingRepository())
        val q = vm.uiState.value.question!!
        vm.onAction(ExerciseAction.Select(q.answerId))
        vm.onAction(ExerciseAction.Check)
        vm.onAction(ExerciseAction.Select(q.choices.first { it.id != q.answerId }.id))
        assertEquals(q.answerId, vm.uiState.value.selectedId)
        assertFalse(vm.uiState.value.canCheck)
    }

    @Test
    fun `reference and slower playback use the tone player`() = runTest(main.dispatcher) {
        val vm = exercise(FakeTrainingRepository(), level = 5)
        vm.onAction(ExerciseAction.HearReference)
        advanceUntilIdle()
        assertEquals(listOf(listOf(60)), player.chords)
        vm.onAction(ExerciseAction.PlaySlower)
        advanceUntilIdle()
        assertEquals(vm.uiState.value.question!!.sounds.single(), player.chords.last())
        assertFalse(vm.uiState.value.isPlaying)
    }

    @Test
    fun `pitch monitor compares the sung note with the reference`() = runTest(main.dispatcher) {
        val frequencies = MutableSharedFlow<Float?>()
        val vm = PitchMonitorViewModel(PitchSource { frequencies }, player, settings)
        vm.onAction(PitchMonitorAction.SelectReference(69))
        vm.onAction(PitchMonitorAction.ToggleListening)
        advanceUntilIdle()
        frequencies.emit(440f)
        advanceUntilIdle()
        assertEquals(PitchStatus.ON_PITCH, vm.uiState.value.reading?.status)
        frequencies.emit(400f)
        advanceUntilIdle()
        assertEquals(PitchStatus.FLAT, vm.uiState.value.reading?.status)
        assertNotNull(vm.uiState.value.reading?.cents)

        vm.onAction(PitchMonitorAction.ToggleListening)
        assertFalse(vm.uiState.value.isListening)
        assertTrue(vm.uiState.value.reading == null)
    }

    @Test
    fun `pitch monitor shows the reference pitch from settings`() = runTest(main.dispatcher) {
        settings.referencePitch.value = 442f
        val vm = PitchMonitorViewModel(PitchSource { MutableSharedFlow() }, player, settings)
        advanceUntilIdle()
        assertEquals(442f, vm.uiState.value.referencePitch)

        settings.referencePitch.value = 432f
        advanceUntilIdle()
        assertEquals(432f, vm.uiState.value.referencePitch)
    }

    @Test
    fun `pitch monitor shows mic off when the microphone is blocked`() = runTest(main.dispatcher) {
        val vm = PitchMonitorViewModel(PitchSource { flow { throw AppError.MicrophoneOff } }, player, settings)
        vm.onAction(PitchMonitorAction.ToggleListening)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.microphoneOff)
        assertFalse(vm.uiState.value.isListening)
    }
}
