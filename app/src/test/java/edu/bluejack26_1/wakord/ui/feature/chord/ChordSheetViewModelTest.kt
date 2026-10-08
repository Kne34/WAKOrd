package edu.bluejack26_1.wakord.ui.feature.chord

import edu.bluejack26_1.wakord.data.fake.FakeSavedItemsRepository
import edu.bluejack26_1.wakord.domain.model.SavedType
import edu.bluejack26_1.wakord.testutil.MainDispatcherRule
import edu.bluejack26_1.wakord.testutil.RecordingTonePlayer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChordSheetViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val saved = FakeSavedItemsRepository()
    private val player = RecordingTonePlayer()

    private fun viewModel() = ChordSheetViewModel(saved, player, computeDispatcher = main.dispatcher)

    @Test
    fun `opening a chord loads its voicings and saved state`() = runTest(main.dispatcher) {
        saved.setSaved(SavedType.CHORD, "Cmaj9", true)
        val vm = viewModel()
        vm.onAction(ChordSheetAction.Open("Cmaj9"))
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("Cmaj9", state.symbol)
        assertTrue(state.voicings.isNotEmpty())
        assertEquals(0, state.selectedVoicing)
        assertTrue(state.isSaved)
    }

    @Test
    fun `selecting a voicing stays inside the list`() = runTest(main.dispatcher) {
        val vm = viewModel()
        vm.onAction(ChordSheetAction.Open("G"))
        advanceUntilIdle()
        val last = vm.uiState.value.voicings.lastIndex

        vm.onAction(ChordSheetAction.SelectVoicing(last))
        assertEquals(last, vm.uiState.value.selectedVoicing)
        vm.onAction(ChordSheetAction.SelectVoicing(last + 5))
        assertEquals(last, vm.uiState.value.selectedVoicing)
    }

    @Test
    fun `preview plays the selected voicing`() = runTest(main.dispatcher) {
        val vm = viewModel()
        vm.onAction(ChordSheetAction.Open("Am7"))
        advanceUntilIdle()
        vm.onAction(ChordSheetAction.Preview)
        advanceUntilIdle()

        assertEquals(1, player.chords.size)
        assertTrue(player.chords.single().isNotEmpty())
    }

    @Test
    fun `saving and unsaving a chord is stored`() = runTest(main.dispatcher) {
        val vm = viewModel()
        vm.onAction(ChordSheetAction.Open("Fmaj7"))
        advanceUntilIdle()

        vm.onAction(ChordSheetAction.ToggleSaved)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isSaved)
        assertTrue((SavedType.CHORD to "Fmaj7") in saved.saved.value)

        vm.onAction(ChordSheetAction.ToggleSaved)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isSaved)
        assertFalse((SavedType.CHORD to "Fmaj7") in saved.saved.value)
    }

    @Test
    fun `closing clears the sheet and stops the sound`() = runTest(main.dispatcher) {
        val vm = viewModel()
        vm.onAction(ChordSheetAction.Open("Dm9"))
        advanceUntilIdle()
        vm.onAction(ChordSheetAction.Close)

        assertNull(vm.uiState.value.symbol)
        assertTrue(vm.uiState.value.voicings.isEmpty())
        assertEquals(1, player.stopped)
    }
}
