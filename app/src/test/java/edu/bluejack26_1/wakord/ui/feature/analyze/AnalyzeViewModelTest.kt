package edu.bluejack26_1.wakord.ui.feature.analyze

import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.core.error.AppError
import edu.bluejack26_1.wakord.data.fake.FakeAnalysisRepository
import edu.bluejack26_1.wakord.data.fake.FakeAudioCapture
import edu.bluejack26_1.wakord.data.fake.FakeChordAnalyzer
import edu.bluejack26_1.wakord.data.fake.FakeExploreRepository
import edu.bluejack26_1.wakord.data.fake.FakeProgressionRepository
import edu.bluejack26_1.wakord.data.fake.SampleAnalysis
import edu.bluejack26_1.wakord.domain.model.Difficulty
import edu.bluejack26_1.wakord.testutil.MainDispatcherRule
import edu.bluejack26_1.wakord.testutil.RecordingTonePlayer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyzeViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val analyzer = FakeChordAnalyzer()
    private val history = FakeAnalysisRepository()
    private val progressions = FakeProgressionRepository(null)
    private val player = RecordingTonePlayer()

    private fun viewModel(capture: FakeAudioCapture) = AnalyzeViewModel(
        capture, analyzer, history, FakeExploreRepository(), progressions, player, computeDispatcher = main.dispatcher,
    )

    @Test
    fun `recording stops at thirty seconds then analyzes and saves history`() = runTest(main.dispatcher) {
        val vm = viewModel(FakeAudioCapture())
        vm.onAction(AnalyzeAction.ToggleRecording)
        advanceTimeBy(1_000)
        assertTrue(vm.uiState.value.isRecording)
        assertTrue(vm.uiState.value.inputLevel > 0f)
        advanceUntilIdle()
        val s = vm.uiState.value
        assertFalse(s.isRecording)
        assertEquals(MAX_RECORDING_MS, s.elapsedMs)
        assertEquals(16_000 * 30, analyzer.analyzedSamples)
        assertEquals(SampleAnalysis.result.id, s.result?.id)
        assertEquals(listOf(SampleAnalysis.result), s.history)
        assertNull(s.stage)
        assertFalse(s.usedFallback)
        assertTrue(s.chordTrack.isNotEmpty())
    }

    @Test
    fun `tapping again stops early`() = runTest(main.dispatcher) {
        val vm = viewModel(FakeAudioCapture())
        vm.onAction(AnalyzeAction.ToggleRecording)
        advanceTimeBy(3_000)
        vm.onAction(AnalyzeAction.ToggleRecording)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.elapsedMs in 2_500L..3_600L)
        assertTrue(analyzer.analyzedSamples < 16_000 * 4)
        assertEquals(SampleAnalysis.result.id, vm.uiState.value.result?.id)
    }

    @Test
    fun `too short recording asks to try again`() = runTest(main.dispatcher) {
        val vm = viewModel(FakeAudioCapture(frameCount = 2))
        vm.onAction(AnalyzeAction.ToggleRecording)
        advanceUntilIdle()
        assertEquals(R.string.error_not_found, vm.uiState.value.errorMessage)
        assertNull(vm.uiState.value.result)
    }

    @Test
    fun `microphone off shows the mic off state`() = runTest(main.dispatcher) {
        val vm = viewModel(FakeAudioCapture(error = AppError.MicrophoneOff))
        vm.onAction(AnalyzeAction.ToggleRecording)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.microphoneOff)
        assertFalse(vm.uiState.value.isRecording)
    }

    @Test
    fun `failed analysis shows an error`() = runTest(main.dispatcher) {
        analyzer.result = Result.failure(AppError.Network)
        val vm = viewModel(FakeAudioCapture(frameCount = 10))
        vm.onAction(AnalyzeAction.ToggleRecording)
        advanceUntilIdle()
        assertEquals(R.string.error_network, vm.uiState.value.errorMessage)
        assertTrue(vm.uiState.value.history.isEmpty())
    }

    @Test
    fun `edit the chord track, play it and save it as a progression`() = runTest(main.dispatcher) {
        val vm = viewModel(FakeAudioCapture())
        vm.onAction(AnalyzeAction.OpenHistory(SampleAnalysis.result))
        advanceUntilIdle()
        val bars = vm.uiState.value.chordTrack.size
        vm.onAction(AnalyzeAction.AddBar("G7"))
        assertEquals("V7", vm.uiState.value.chordTrack.last().roman)
        vm.onAction(AnalyzeAction.RemoveBar(0))
        assertEquals(bars, vm.uiState.value.chordTrack.size)

        vm.onAction(AnalyzeAction.PlayResult)
        advanceUntilIdle()
        assertEquals(bars, player.chords.size)
        assertFalse(vm.uiState.value.isPlayingResult)

        vm.onAction(AnalyzeAction.SaveAsProgression("My loop"))
        advanceUntilIdle()
        val id = vm.uiState.value.savedProgressionId!!
        val saved = progressions.progressions.value.getValue(id)
        assertEquals("My loop", saved.title)
        assertEquals("C", saved.key)
        assertEquals(92, saved.bpm)
        assertEquals(Difficulty.INTERMEDIATE, saved.difficulty)
        assertEquals(vm.uiState.value.chordTrack.map { it.symbol }, saved.chords)
    }
}
