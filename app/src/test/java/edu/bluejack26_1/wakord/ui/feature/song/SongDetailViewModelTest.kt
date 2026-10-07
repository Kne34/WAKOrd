package edu.bluejack26_1.wakord.ui.feature.song

import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.core.error.AppError
import edu.bluejack26_1.wakord.data.fake.FakeCommentRepository
import edu.bluejack26_1.wakord.data.fake.FakeSavedItemsRepository
import edu.bluejack26_1.wakord.data.fake.FakeSongRepository
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
class SongDetailViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val songs = FakeSongRepository()
    private val comments = FakeCommentRepository()
    private val saved = FakeSavedItemsRepository()
    private val player = RecordingTonePlayer()

    private fun viewModel(id: String = SampleMusic.song.id) =
        SongDetailViewModel(id, songs, comments, saved, player, computeDispatcher = main.dispatcher)

    @Test
    fun `loads song`() = runTest(main.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
        assertEquals("Subtle Resonance", vm.uiState.value.song?.title)
        assertEquals("Eb", vm.uiState.value.originalKey)
    }

    @Test
    fun `missing song shows not found`() = runTest(main.dispatcher) {
        val vm = viewModel("nope")
        advanceUntilIdle()
        assertNull(vm.uiState.value.song)
        assertEquals(R.string.error_not_found, vm.uiState.value.errorMessage)
    }

    @Test
    fun `transpose up and reset`() = runTest(main.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onAction(SongDetailAction.Transpose(1))
        vm.onAction(SongDetailAction.Transpose(1))
        assertEquals("F", vm.uiState.value.song?.key)
        assertEquals(2, vm.uiState.value.transposeSteps)
        assertEquals("Fmaj7", vm.uiState.value.song?.sections?.first()?.lines?.first()?.segments?.first()?.chord)

        vm.onAction(SongDetailAction.ResetTranspose)
        assertEquals("Eb", vm.uiState.value.song?.key)
    }

    @Test
    fun `play goes through every chord then stops`() = runTest(main.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onAction(SongDetailAction.TogglePlay)
        advanceUntilIdle()

        val expected = SongDetailViewModel.chordPositions(SampleMusic.song).size
        assertEquals(expected, player.chords.size)
        assertTrue(player.chords.all { it.isNotEmpty() })
        assertFalse(vm.uiState.value.isPlaying)
        assertNull(vm.uiState.value.playingPosition)
    }

    @Test
    fun `slower speed makes the song longer`() = runTest(main.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onAction(SongDetailAction.TogglePlay)
        advanceUntilIdle()
        val normal = vm.uiState.value.totalMs

        vm.onAction(SongDetailAction.SetSpeed(0.5f))
        vm.onAction(SongDetailAction.TogglePlay)
        advanceUntilIdle()
        assertEquals(normal * 2, vm.uiState.value.totalMs)
    }

    @Test
    fun `like and save are stored`() = runTest(main.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onAction(SongDetailAction.ToggleLike)
        vm.onAction(SongDetailAction.ToggleSave)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isLiked)
        assertTrue(vm.uiState.value.isSaved)
        assertTrue((SavedType.SONG to SampleMusic.song.id) in saved.saved.value)
    }

    @Test
    fun `failed like is reverted`() = runTest(main.dispatcher) {
        songs.likeResult = Result.failure(AppError.Network)
        val vm = viewModel()
        advanceUntilIdle()
        vm.onAction(SongDetailAction.ToggleLike)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLiked)
        assertEquals(R.string.error_network, vm.uiState.value.errorMessage)
    }

    @Test
    fun `post and reply to comments`() = runTest(main.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onAction(SongDetailAction.CommentChanged("Try the open voicing"))
        vm.onAction(SongDetailAction.PostComment)
        advanceUntilIdle()
        val first = vm.uiState.value.comments.single()
        assertEquals("", vm.uiState.value.commentDraft)

        vm.onAction(SongDetailAction.ReplyTo(first))
        vm.onAction(SongDetailAction.CommentChanged("Thanks!"))
        vm.onAction(SongDetailAction.PostComment)
        advanceUntilIdle()
        assertEquals(first.id, vm.uiState.value.comments.last().parentId)
        assertNull(vm.uiState.value.replyingTo)
    }

    @Test
    fun `blocked comment shows popup and keeps draft`() = runTest(main.dispatcher) {
        comments.blockedWord = "spam"
        val vm = viewModel()
        advanceUntilIdle()
        vm.onAction(SongDetailAction.CommentChanged("buy spam now"))
        vm.onAction(SongDetailAction.PostComment)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.showCommentBlocked)
        assertEquals("buy spam now", vm.uiState.value.commentDraft)
    }

    @Test
    fun `like a comment`() = runTest(main.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onAction(SongDetailAction.CommentChanged("Nice"))
        vm.onAction(SongDetailAction.PostComment)
        advanceUntilIdle()
        vm.onAction(SongDetailAction.ToggleCommentLike(vm.uiState.value.comments.single()))
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.comments.single().likeCount)
        assertTrue(vm.uiState.value.comments.single().likedByMe)
    }
}
