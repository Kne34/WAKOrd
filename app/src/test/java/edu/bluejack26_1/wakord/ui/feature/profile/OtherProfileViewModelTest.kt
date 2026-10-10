package edu.bluejack26_1.wakord.ui.feature.profile

import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.data.fake.FakeFollowRepository
import edu.bluejack26_1.wakord.data.fake.FakeProfileRepository
import edu.bluejack26_1.wakord.data.fake.FakeUserRepository
import edu.bluejack26_1.wakord.data.fake.SampleSocial
import edu.bluejack26_1.wakord.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OtherProfileViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val follows = FakeFollowRepository()
    private val profiles = FakeProfileRepository()

    private fun viewModel(uid: String) = OtherProfileViewModel(uid, FakeUserRepository(SampleSocial.me), profiles, follows)

    @Test
    fun `shows progressions sorted by most liked and follows`() = runTest(main.dispatcher) {
        val vm = viewModel("julian")
        advanceUntilIdle()
        val s = vm.uiState.value
        assertEquals("Julian Keys", s.user?.fullName)
        assertEquals(s.progressions.sortedByDescending { it.likeCount }, s.progressions)
        assertFalse(s.isFollowing)

        vm.onAction(OtherProfileAction.ToggleFollow)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isFollowing)
        assertEquals(1, vm.uiState.value.stats.followers)
        assertTrue("julian" in follows.following.value)

        vm.onAction(OtherProfileAction.ToggleFollow)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isFollowing)
        assertEquals(0, vm.uiState.value.stats.followers)
    }

    @Test
    fun `cannot follow yourself`() = runTest(main.dispatcher) {
        val vm = viewModel("me")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isMe)
        vm.onAction(OtherProfileAction.ToggleFollow)
        advanceUntilIdle()
        assertTrue(follows.following.value.isEmpty())
    }

    @Test
    fun `unknown user shows not found`() = runTest(main.dispatcher) {
        val vm = viewModel("ghost")
        advanceUntilIdle()
        assertEquals(R.string.error_not_found, vm.uiState.value.errorMessage)
    }
}
