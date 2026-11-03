package edu.bluejack26_1.wakord.ui.feature.notifications

import app.cash.turbine.test
import edu.bluejack26_1.wakord.data.fake.FakeNotificationRepository
import edu.bluejack26_1.wakord.data.fake.SampleSocial
import edu.bluejack26_1.wakord.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationBadgeViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun `badge follows unread notifications`() = runTest(main.dispatcher) {
        val repository = FakeNotificationRepository()
        val vm = NotificationBadgeViewModel(repository)
        val unread = SampleSocial.notifications.count { !it.isRead }

        vm.unreadCount.test {
            assertEquals(0, awaitItem())
            assertEquals(unread, awaitItem())
            repository.markAllRead()
            assertEquals(0, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
