package edu.bluejack26_1.wakord.ui.feature.analyze

import edu.bluejack26_1.wakord.core.audio.AudioCapture
import edu.bluejack26_1.wakord.core.error.AppError
import edu.bluejack26_1.wakord.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

@OptIn(ExperimentalCoroutinesApi::class)
class LiveChordViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val rate = 16_000
    private val frames = MutableSharedFlow<FloatArray>()

    private val capture = object : AudioCapture {
        override val sampleRate = rate
        override fun frames(frameSize: Int): Flow<FloatArray> = frames
    }

    private fun chord(vararg midis: Int) = FloatArray(4096) { i ->
        midis.sumOf { m -> 0.2 * sin(2 * PI * 440.0 * 2.0.pow((m - 69) / 12.0) * i / rate) }.toFloat()
    }

    @Test
    fun `shows the strummed chord and keeps it through short silences`() = runTest(main.dispatcher) {
        val vm = LiveChordViewModel(capture, main.dispatcher)
        vm.onAction(LiveChordAction.ToggleListening)
        advanceUntilIdle()
        frames.emit(chord(50, 57, 62, 65))
        advanceUntilIdle()
        assertEquals("Dm", vm.uiState.value.reading?.symbol)
        assertEquals(listOf("D", "F", "A"), vm.uiState.value.reading?.notes)

        repeat(3) { frames.emit(FloatArray(4096)) }
        advanceUntilIdle()
        assertEquals("Dm", vm.uiState.value.reading?.symbol)
        frames.emit(FloatArray(4096))
        advanceUntilIdle()
        assertNull(vm.uiState.value.reading)

        vm.onAction(LiveChordAction.ToggleListening)
        assertFalse(vm.uiState.value.isListening)
    }

    @Test
    fun `microphone off is reported`() = runTest(main.dispatcher) {
        val denied = object : AudioCapture {
            override val sampleRate = rate
            override fun frames(frameSize: Int): Flow<FloatArray> = flow { throw AppError.MicrophoneOff }
        }
        val vm = LiveChordViewModel(denied, main.dispatcher)
        vm.onAction(LiveChordAction.ToggleListening)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.microphoneOff)
        assertFalse(vm.uiState.value.isListening)
    }
}
