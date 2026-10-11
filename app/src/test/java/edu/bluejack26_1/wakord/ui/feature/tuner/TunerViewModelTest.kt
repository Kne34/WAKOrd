package edu.bluejack26_1.wakord.ui.feature.tuner

import edu.bluejack26_1.wakord.core.audio.AudioInput
import edu.bluejack26_1.wakord.core.audio.YinPitchDetector
import edu.bluejack26_1.wakord.data.fake.FakeTuningRepository
import edu.bluejack26_1.wakord.domain.model.TuneStatus
import edu.bluejack26_1.wakord.domain.model.TunerInstrument
import edu.bluejack26_1.wakord.domain.model.TuningPresets
import edu.bluejack26_1.wakord.testutil.MainDispatcherRule
import edu.bluejack26_1.wakord.testutil.RecordingTonePlayer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

@OptIn(ExperimentalCoroutinesApi::class)
class TunerViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private class ToneInput(private val frequency: Float) : AudioInput {
        override val sampleRate = 44_100
        override fun frames(): Flow<FloatArray> {
            val frame = FloatArray(4096) { i -> (0.5 * sin(2 * PI * frequency * i / sampleRate)).toFloat() }
            return flowOf(frame, frame, frame)
        }
    }

    private fun viewModel(
        frequency: Float = 110f,
        tonePlayer: RecordingTonePlayer = RecordingTonePlayer(),
        referencePitch: Float = 440f,
    ) = TunerViewModel(
        audioInput = ToneInput(frequency),
        pitchDetector = YinPitchDetector(),
        tuningRepository = FakeTuningRepository(),
        tonePlayer = tonePlayer,
        referencePitch = flowOf(referencePitch),
        computeDispatcher = mainRule.dispatcher,
    )

    @Test
    fun `in tune A string is detected and marked tuned`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(110f)
        vm.onAction(TunerAction.PermissionResult(granted = true))
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(1, state.activeString)
        assertEquals(TuneStatus.IN_TUNE, state.status)
        assertEquals("A", state.reading?.name)
        assertEquals(2, state.reading?.octave)
        assertTrue(1 in state.tunedStrings)
    }

    @Test
    fun `flat D string shows flat status`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(145.6f)
        vm.onAction(TunerAction.PermissionResult(granted = true))
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(2, state.activeString)
        assertEquals(TuneStatus.FLAT, state.status)
        assertTrue(state.centsOff < -5f)
        assertFalse(2 in state.tunedStrings)
    }

    @Test
    fun `sharp tone shows sharp status`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(113f)
        vm.onAction(TunerAction.PermissionResult(granted = true))
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.activeString)
        assertEquals(TuneStatus.SHARP, vm.uiState.value.status)
    }

    @Test
    fun `manual string compares against the selected string`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(110f)
        vm.onAction(TunerAction.SelectString(2))
        vm.onAction(TunerAction.PermissionResult(granted = true))
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.autoDetect)
        assertEquals(2, state.activeString)
        assertEquals(TuneStatus.FLAT, state.status)
    }

    @Test
    fun `reference pitch shifts the target`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(frequency = 108f, referencePitch = 432f)
        vm.onAction(TunerAction.PermissionResult(granted = true))
        advanceUntilIdle()

        assertEquals(TuneStatus.IN_TUNE, vm.uiState.value.status)
    }

    @Test
    fun `denied permission does not listen`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(110f)
        vm.onAction(TunerAction.PermissionResult(granted = false))
        advanceUntilIdle()

        assertEquals(MicPermission.DENIED, vm.uiState.value.micPermission)
        assertNull(vm.uiState.value.reading)
    }

    @Test
    fun `pitch pipe mode turns off once the mic is allowed`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(110f)
        vm.onAction(TunerAction.PermissionResult(granted = false))
        vm.onAction(TunerAction.UsePitchPipe)
        assertTrue(vm.uiState.value.pitchPipeMode)

        vm.onAction(TunerAction.PermissionResult(granted = true))
        advanceUntilIdle()

        assertFalse(vm.uiState.value.pitchPipeMode)
        assertEquals(MicPermission.GRANTED, vm.uiState.value.micPermission)
    }

    @Test
    fun `play mode stops listening and listen mode starts it again`() = runTest(mainRule.dispatcher) {
        val player = RecordingTonePlayer()
        val vm = viewModel(110f, player)
        vm.onAction(TunerAction.PermissionResult(granted = true))
        advanceUntilIdle()
        assertEquals(TuneStatus.IN_TUNE, vm.uiState.value.status)

        vm.onAction(TunerAction.UsePitchPipe)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.pitchPipeMode)
        assertNull(vm.uiState.value.reading)
        assertEquals(TuneStatus.IDLE, vm.uiState.value.status)

        vm.onAction(TunerAction.PermissionResult(granted = true))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.pitchPipeMode)
        assertNull(vm.uiState.value.reading)

        vm.onAction(TunerAction.PlayPitchPipe(2))
        vm.onAction(TunerAction.UseListening)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.pitchPipeMode)
        assertNull(vm.uiState.value.pitchPipeString)
        assertTrue(player.stopped > 0)
        assertEquals(TuneStatus.IN_TUNE, vm.uiState.value.status)
    }

    @Test
    fun `pitch pipe plays the string note`() = runTest(mainRule.dispatcher) {
        val player = RecordingTonePlayer()
        val vm = viewModel(110f, player)
        vm.onAction(TunerAction.PlayPitchPipe(0))
        advanceUntilIdle()

        assertEquals(listOf(listOf(40)), player.chords)
        assertEquals(0, vm.uiState.value.selectedString)
        assertNull(vm.uiState.value.pitchPipeString)
    }

    @Test
    fun `changing instrument resets to its default tuning`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(110f)
        vm.onAction(TunerAction.SelectInstrument(TunerInstrument.MANDOLIN))
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(TunerInstrument.MANDOLIN, state.instrument)
        assertEquals(TuningPresets.default(TunerInstrument.MANDOLIN), state.tuning)
        assertNull(state.sheet)
    }

    @Test
    fun `selecting a preset keeps the instrument in sync`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(110f)
        val dropD = TuningPresets.forInstrument(TunerInstrument.GUITAR)[1]
        vm.onAction(TunerAction.SelectTuning(dropD))

        assertEquals(dropD, vm.uiState.value.tuning)
        assertEquals(TunerInstrument.GUITAR, vm.uiState.value.instrument)
    }

    @Test
    fun `custom tuning is saved and selected`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(110f)
        vm.onAction(TunerAction.SaveCustomTuning(" Half step down ", listOf("D#2", "G#2", "C#3", "F#3", "A#3", "D#4")))
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("Half step down", state.tuning.name)
        assertTrue(state.tuning.isCustom)
        assertEquals(listOf(state.tuning), state.customTunings)
    }
}
