package edu.bluejack26_1.wakord.ui.feature

import edu.bluejack26_1.wakord.data.fake.FakeFollowRepository
import edu.bluejack26_1.wakord.data.fake.FakeNotificationRepository
import edu.bluejack26_1.wakord.data.fake.FakeProfileRepository
import edu.bluejack26_1.wakord.data.fake.FakeUserRepository
import edu.bluejack26_1.wakord.data.fake.SampleSocial
import edu.bluejack26_1.wakord.domain.model.AppNotification
import edu.bluejack26_1.wakord.domain.model.User
import edu.bluejack26_1.wakord.domain.repository.NotificationRepository
import edu.bluejack26_1.wakord.domain.repository.ProfileRepository
import edu.bluejack26_1.wakord.testutil.MainDispatcherRule
import edu.bluejack26_1.wakord.ui.feature.notifications.NotificationsAction
import edu.bluejack26_1.wakord.ui.feature.notifications.NotificationsViewModel
import edu.bluejack26_1.wakord.ui.feature.profile.OtherProfileAction
import edu.bluejack26_1.wakord.ui.feature.profile.OtherProfileViewModel
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoadErrorRetryTest {
    @get:Rule
    val main = MainDispatcherRule()

    private var failNext = true

    private fun <T> failingOnce(source: Flow<T>): Flow<T> = flow {
        if (failNext) {
            failNext = false
            throw IOException("offline")
        }
        source.collect { emit(it) }
    }

    @Test
    fun `notifications show a load error and recover on retry`() = runTest(main.dispatcher) {
        val fake = FakeNotificationRepository()
        val repository = object : NotificationRepository by fake {
            override val notifications: Flow<List<AppNotification>> = failingOnce(fake.notifications)
        }
        val vm = NotificationsViewModel(repository) { 1_791_100_000_000L }
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.loadError)
        assertFalse(vm.uiState.value.isLoading)

        vm.onAction(NotificationsAction.Retry)
        advanceUntilIdle()
        assertNull(vm.uiState.value.loadError)
        assertEquals(SampleSocial.notifications.size, vm.uiState.value.newItems.size + vm.uiState.value.earlierItems.size)
    }

    @Test
    fun `other profile shows a load error and recovers on retry`() = runTest(main.dispatcher) {
        val fake = FakeProfileRepository()
        val profiles = object : ProfileRepository by fake {
            override fun user(uid: String): Flow<User?> = failingOnce(fake.user(uid))
        }
        val vm = OtherProfileViewModel("julian", FakeUserRepository(SampleSocial.me), profiles, FakeFollowRepository())
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.loadError)
        assertNull(vm.uiState.value.user)

        vm.onAction(OtherProfileAction.Retry)
        advanceUntilIdle()
        assertNull(vm.uiState.value.loadError)
        assertEquals("Julian Keys", vm.uiState.value.user?.fullName)
    }
}
