package edu.bluejack26_1.wakord.ui.feature.home

import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.data.fake.FakeFeedRepository
import edu.bluejack26_1.wakord.data.fake.FakeNotificationRepository
import edu.bluejack26_1.wakord.data.fake.FakePlaybackController
import edu.bluejack26_1.wakord.data.fake.FakeProgressionRepository
import edu.bluejack26_1.wakord.data.fake.FakeSavedItemsRepository
import edu.bluejack26_1.wakord.data.fake.FakeUserRepository
import edu.bluejack26_1.wakord.data.fake.SampleDiscover
import edu.bluejack26_1.wakord.data.fake.FakeTrainingRepository
import edu.bluejack26_1.wakord.domain.model.FeedItem
import edu.bluejack26_1.wakord.domain.model.SavedType
import edu.bluejack26_1.wakord.domain.model.SkillType
import edu.bluejack26_1.wakord.domain.model.TrainingSession
import edu.bluejack26_1.wakord.domain.model.User
import edu.bluejack26_1.wakord.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val users = FakeUserRepository(User(uid = "u", fullName = "Keane Wilson", email = "k@mail.com", genres = listOf("Jazz")))
    private val feed = FakeFeedRepository()
    private val saved = FakeSavedItemsRepository()
    private val progressions = FakeProgressionRepository()
    private val playback = FakePlaybackController()
    private val now = 1_791_100_000_000L
    private val training = FakeTrainingRepository(
        listOf(TrainingSession("s1", 1, SkillType.SINGLE_NOTE, 8, 10, 180_000, now - 3_600_000)),
    )

    private fun viewModel() = HomeViewModel(
        users, feed, saved, progressions, playback, training, FakeNotificationRepository(),
        computeDispatcher = main.dispatcher, clock = { now }, zone = ZoneOffset.UTC,
    )

    @Test
    fun `daily workout card shows today's training`() = runTest(main.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        val workout = vm.uiState.value.workout
        assertEquals(3, workout.minutesDone)
        assertEquals(5, workout.minutesGoal)
        assertEquals(1, workout.streakDays)
        assertEquals(2, workout.level)
        assertEquals(80, workout.accuracy)
    }

    @Test
    fun `first page loads greeting, recommendations and feed`() = runTest(main.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        val s = vm.uiState.value
        assertFalse(s.isLoading)
        assertEquals("Keane Wilson", s.userName)
        assertEquals(2, s.unreadNotifications)
        assertTrue(s.recommended.isNotEmpty())
        assertEquals(HomeViewModel.PAGE_SIZE + 2, s.feed.size)
        assertTrue(s.feed.all { it.commentCount == 2 })
        assertFalse(s.endReached)
    }

    @Test
    fun `liking a progression from the feed saves it and bumps the count`() = runTest(main.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        val item = vm.uiState.value.feed.filterIsInstance<FeedItem.ProgressionItem>().first()
        val before = item.progression.likeCount

        vm.onAction(HomeAction.ToggleProgressionLike(item.progression))
        advanceUntilIdle()

        val after = vm.uiState.value.feed.filterIsInstance<FeedItem.ProgressionItem>().first { it.id == item.id }
        assertTrue(item.id in vm.uiState.value.likedIds)
        assertTrue(item.id in progressions.liked.value)
        assertEquals(before + 1, after.progression.likeCount)

        vm.onAction(HomeAction.ToggleProgressionLike(after.progression))
        advanceUntilIdle()
        val reverted = vm.uiState.value.feed.filterIsInstance<FeedItem.ProgressionItem>().first { it.id == item.id }
        assertFalse(item.id in vm.uiState.value.likedIds)
        assertEquals(before, reverted.progression.likeCount)
    }

    @Test
    fun `likes made elsewhere update the feed in real time`() = runTest(main.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        val item = vm.uiState.value.feed.filterIsInstance<FeedItem.ProgressionItem>().first()
        val before = item.progression.likeCount

        feed.liked.value = setOf(item.id)
        advanceUntilIdle()

        val after = vm.uiState.value.feed.filterIsInstance<FeedItem.ProgressionItem>().first { it.id == item.id }
        assertTrue(item.id in vm.uiState.value.likedIds)
        assertEquals(before + 1, after.progression.likeCount)
    }

    @Test
    fun `favorite genre is ranked first in the feed`() = runTest(main.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals("Jazz", vm.uiState.value.feed.first().genre)
    }

    @Test
    fun `load more uses the cursor until the end`() = runTest(main.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onAction(HomeAction.LoadMore)
        advanceUntilIdle()
        vm.onAction(HomeAction.LoadMore)
        advanceUntilIdle()
        val s = vm.uiState.value
        assertTrue(s.endReached)
        assertEquals(SampleDiscover.progressions.size + SampleDiscover.songs.size, s.feed.size)
        assertEquals(s.feed.size, s.feed.map { it.id }.toSet().size)
        assertEquals(3, feed.requestedCursors.size)
        assertNull(feed.requestedCursors.first())

        vm.onAction(HomeAction.LoadMore)
        advanceUntilIdle()
        assertEquals(3, feed.requestedCursors.size)
    }

    @Test
    fun `failed page shows error and keeps the feed`() = runTest(main.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        feed.failNextPage = true
        vm.onAction(HomeAction.LoadMore)
        advanceUntilIdle()
        val s = vm.uiState.value
        assertEquals(R.string.error_network, s.errorMessage)
        assertEquals(HomeViewModel.PAGE_SIZE + 2, s.feed.size)
        assertFalse(s.isLoadingMore)
    }

    @Test
    fun `recently saved chords come with a diagram`() = runTest(main.dispatcher) {
        saved.setSaved(SavedType.CHORD, "G", true)
        saved.setSaved(SavedType.CHORD, "Am7", true)
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(listOf("Am7", "G"), vm.uiState.value.savedChords.map { it.symbol })
        assertTrue(vm.uiState.value.savedChords.all { it.voicing != null })
    }

    @Test
    fun `playing from the feed starts the mini player and records a play`() = runTest(main.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        val item = vm.uiState.value.feed.filterIsInstance<FeedItem.ProgressionItem>().first()
        vm.onAction(HomeAction.PlayProgression(item.progression))
        advanceUntilIdle()
        assertEquals(listOf(item.id), playback.played)
        assertEquals(listOf(item.id), progressions.plays)
        assertTrue(playback.state.value.isPlaying)
    }
}
