package edu.bluejack26_1.wakord.ui.feature.progression

import edu.bluejack26_1.wakord.data.fake.FakeCommentRepository
import edu.bluejack26_1.wakord.data.fake.FakeProgressionRepository
import edu.bluejack26_1.wakord.data.fake.FakeSavedItemsRepository
import edu.bluejack26_1.wakord.data.fake.SampleMusic
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
class ProgressionDetailViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val progressions = FakeProgressionRepository()
    private val comments = FakeCommentRepository()
    private val saved = FakeSavedItemsRepository()
    private val player = RecordingTonePlayer()

    private fun viewModel() = ProgressionDetailViewModel(
        SampleMusic.progression.id, progressions, comments, saved, player, computeDispatcher = main.dispatcher,
    )

    @Test
    fun `loads progression with a diagram per chord`() = runTest(main.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        val s = vm.uiState.value
        assertEquals("Neo-Soul Progression #04", s.progression?.title)
        assertEquals(3, s.chordDiagrams.size)
        assertTrue(s.chordDiagrams.all { it != null })
    }

    @Test
    fun `play progression records a play and plays every chord`() = runTest(main.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onAction(ProgressionDetailAction.TogglePlay)
        advanceUntilIdle()
        assertEquals(listOf(SampleMusic.progression.id), progressions.plays)
        assertEquals(3, player.chords.size)
        assertFalse(vm.uiState.value.isPlaying)
        assertNull(vm.uiState.value.playingIndex)
    }

    @Test
    fun `chord sheet shows voicings and plays preview`() = runTest(main.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onAction(ProgressionDetailAction.OpenChord("Cmaj9"))
        advanceUntilIdle()
        assertEquals("Cmaj9", vm.uiState.value.selectedChord)
        assertTrue(vm.uiState.value.voicings.isNotEmpty())

        vm.onAction(ProgressionDetailAction.PreviewChord)
        advanceUntilIdle()
        assertEquals(1, player.chords.size)

        vm.onAction(ProgressionDetailAction.CloseChord)
        assertNull(vm.uiState.value.selectedChord)
    }

    @Test
    fun `save chord from the sheet`() = runTest(main.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onAction(ProgressionDetailAction.OpenChord("G13"))
        advanceUntilIdle()
        vm.onAction(ProgressionDetailAction.ToggleChordSaved)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isChordSaved)
        assertTrue((SavedType.CHORD to "G13") in saved.saved.value)
    }

    @Test
    fun `like, save and comment`() = runTest(main.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onAction(ProgressionDetailAction.ToggleLike)
        vm.onAction(ProgressionDetailAction.ToggleSave)
        vm.onAction(ProgressionDetailAction.CommentChanged("That G13 voicing is perfect"))
        vm.onAction(ProgressionDetailAction.PostComment)
        advanceUntilIdle()
        val s = vm.uiState.value
        assertTrue(s.isLiked)
        assertTrue(s.isSaved)
        assertEquals("That G13 voicing is perfect", s.comments.single().text)
    }
}
