package edu.bluejack26_1.wakord.ui.feature.analyze

import edu.bluejack26_1.wakord.core.audio.AudioCapture
import edu.bluejack26_1.wakord.core.error.AppError
import edu.bluejack26_1.wakord.data.fake.FakeProgressionRepository
import edu.bluejack26_1.wakord.testutil.MainDispatcherRule
import edu.bluejack26_1.wakord.testutil.RecordingTonePlayer
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
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

@OptIn(ExperimentalCoroutinesApi::class)
class LiveChordViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val rate = 16_000
    private val player = RecordingTonePlayer()
    private val progressions = FakeProgressionRepository(null)
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
        val vm = LiveChordViewModel(capture, player, progressions, main.dispatcher)
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
        val vm = LiveChordViewModel(denied, player, progressions, main.dispatcher)
        vm.onAction(LiveChordAction.ToggleListening)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.microphoneOff)
        assertFalse(vm.uiState.value.isListening)
    }

    private fun trackVm() = LiveChordViewModel(capture, player, progressions, main.dispatcher)

    @Test
    fun `adding bars guesses the key from the first bar and labels numerals`() = runTest(main.dispatcher) {
        val vm = trackVm()
        vm.onAction(LiveChordAction.AddBar("Bm7"))
        vm.onAction(LiveChordAction.AddBar("E7"))
        vm.onAction(LiveChordAction.AddBar("F#m"))
        val s = vm.uiState.value
        assertEquals("Bm", s.trackKey)
        assertEquals(listOf("Bm7", "E7", "F#m"), s.bars.map { it.symbol })
        assertEquals(3, s.bars.count { it.roman.isNotBlank() })

        vm.onAction(LiveChordAction.RemoveBar(0))
        assertEquals("E", vm.uiState.value.trackKey)
        assertEquals(listOf("E7", "F#m"), vm.uiState.value.bars.map { it.symbol })
    }

    @Test
    fun `the track holds at most eight bars and ignores unknown chords`() = runTest(main.dispatcher) {
        val vm = trackVm()
        repeat(10) { vm.onAction(LiveChordAction.AddBar("C")) }
        vm.onAction(LiveChordAction.AddBar("not a chord"))
        assertEquals(MAX_TRACK_BARS, vm.uiState.value.bars.size)
        assertFalse(vm.uiState.value.canAddBar)
    }

    @Test
    fun `bpm stays in range`() = runTest(main.dispatcher) {
        val vm = trackVm()
        vm.onAction(LiveChordAction.SetBpm(500))
        assertEquals(MAX_TRACK_BPM, vm.uiState.value.bpm)
        vm.onAction(LiveChordAction.SetBpm(5))
        assertEquals(MIN_TRACK_BPM, vm.uiState.value.bpm)
    }

    @Test
    fun `playing the track plays every bar once without loop`() = runTest(main.dispatcher) {
        val vm = trackVm()
        vm.onAction(LiveChordAction.AddBar("C"))
        vm.onAction(LiveChordAction.AddBar("G"))
        vm.onAction(LiveChordAction.PlayTrack)
        advanceUntilIdle()
        assertEquals(2, player.chords.size)
        assertFalse(vm.uiState.value.isPlayingTrack)
        assertNull(vm.uiState.value.playingBar)
    }

    @Test
    fun `saving the track creates a progression`() = runTest(main.dispatcher) {
        val vm = trackVm()
        vm.onAction(LiveChordAction.AddBar("Am"))
        vm.onAction(LiveChordAction.AddBar("F"))
        vm.onAction(LiveChordAction.SaveAsProgression("Night drive"))
        advanceUntilIdle()
        val id = vm.uiState.value.savedProgressionId
        assertTrue(id != null)
        val saved = progressions.progressions.value.getValue(id!!)
        assertEquals("Night drive", saved.title)
        assertEquals("Am", saved.key)
        assertEquals(listOf("Am", "F"), saved.chords)
    }

    @Test
    fun `start and stop listening are idempotent`() = runTest(main.dispatcher) {
        val vm = trackVm()
        vm.onAction(LiveChordAction.StartListening)
        vm.onAction(LiveChordAction.StartListening)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isListening)
        vm.onAction(LiveChordAction.StopListening)
        vm.onAction(LiveChordAction.StopListening)
        assertFalse(vm.uiState.value.isListening)
    }

    @Test
    fun `lock keeps showing the chord while the mic keeps listening`() = runTest(main.dispatcher) {
        val vm = trackVm()
        vm.onAction(LiveChordAction.StartListening)
        advanceUntilIdle()
        frames.emit(chord(50, 57, 62, 65))
        advanceUntilIdle()
        vm.onAction(LiveChordAction.ToggleLock)
        assertTrue(vm.uiState.value.isLocked)

        frames.emit(chord(48, 55, 60, 64))
        advanceUntilIdle()
        assertEquals("Dm", vm.uiState.value.shownReading?.symbol)
        assertEquals("C", vm.uiState.value.reading?.symbol)

        vm.onAction(LiveChordAction.ToggleLock)
        assertEquals("C", vm.uiState.value.shownReading?.symbol)
        vm.onAction(LiveChordAction.StopListening)
    }

    @Test
    fun `building pauses the mic, starts from the shown chord and names the keys`() = runTest(main.dispatcher) {
        val vm = trackVm()
        vm.onAction(LiveChordAction.StartListening)
        advanceUntilIdle()
        frames.emit(chord(50, 57, 62, 65))
        advanceUntilIdle()

        vm.onAction(LiveChordAction.StartBuilding)
        val building = vm.uiState.value
        assertTrue(building.isBuilding)
        assertFalse(building.isListening)
        assertEquals(setOf(50, 53, 57), building.builtNotes)
        assertEquals("Dm", building.shownReading?.symbol)

        vm.onAction(LiveChordAction.ToggleKey(53))
        assertNull(vm.uiState.value.shownReading)
        vm.onAction(LiveChordAction.ToggleKey(54))
        assertEquals("D", vm.uiState.value.shownReading?.symbol)
        vm.onAction(LiveChordAction.ToggleKey(60))
        assertEquals("D7", vm.uiState.value.shownReading?.symbol)

        vm.onAction(LiveChordAction.AddBar(vm.uiState.value.shownReading!!.symbol))
        assertEquals(listOf("D7"), vm.uiState.value.bars.map { it.symbol })

        vm.onAction(LiveChordAction.StartListening)
        assertFalse(vm.uiState.value.isListening)
        vm.onAction(LiveChordAction.ClearKeys)
        assertTrue(vm.uiState.value.builtNotes.isEmpty())

        vm.onAction(LiveChordAction.FinishBuilding)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isBuilding)
        assertTrue(vm.uiState.value.isListening)
        assertEquals("Dm", vm.uiState.value.shownReading?.symbol)
        vm.onAction(LiveChordAction.StopListening)
    }

    @Test
    fun `keys are ignored outside build mode and tapping the status card ends building`() = runTest(main.dispatcher) {
        val vm = trackVm()
        vm.onAction(LiveChordAction.ToggleKey(60))
        assertTrue(vm.uiState.value.builtNotes.isEmpty())

        vm.onAction(LiveChordAction.StartBuilding)
        assertTrue(vm.uiState.value.builtNotes.isEmpty())
        vm.onAction(LiveChordAction.ToggleListening)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isBuilding)
        assertTrue(vm.uiState.value.isListening)
        vm.onAction(LiveChordAction.StopListening)
    }
}
