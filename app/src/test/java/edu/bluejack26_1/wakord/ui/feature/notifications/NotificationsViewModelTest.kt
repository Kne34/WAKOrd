package edu.bluejack26_1.wakord.ui.feature.notifications

import edu.bluejack26_1.wakord.core.notification.NotificationAlerts
import edu.bluejack26_1.wakord.data.fake.FakeNotificationRepository
import edu.bluejack26_1.wakord.data.fake.SampleSocial
import edu.bluejack26_1.wakord.domain.model.TargetType
import edu.bluejack26_1.wakord.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val repository = FakeNotificationRepository()
    private val now = 1_791_100_000_000L

    @Test
    fun `splits new and earlier and counts unread`() = runTest(main.dispatcher) {
        val vm = NotificationsViewModel(repository) { now }
        advanceUntilIdle()
        val s = vm.uiState.value
        assertEquals(listOf("n1", "n3"), s.newItems.map { it.id })
        assertEquals(listOf("n2"), s.earlierItems.map { it.id })
        assertEquals(2, s.unreadCount)
    }

    @Test
    fun `opening marks read and navigates to the target`() = runTest(main.dispatcher) {
        val vm = NotificationsViewModel(repository) { now }
        advanceUntilIdle()
        vm.onAction(NotificationsAction.Open(SampleSocial.notifications.first()))
        advanceUntilIdle()
        assertEquals(NotificationsEffect.Navigate(TargetType.PROGRESSION, "p1"), vm.effects.first())
        assertEquals(1, vm.uiState.value.unreadCount)
    }

    @Test
    fun `mark all read and empty state`() = runTest(main.dispatcher) {
        val vm = NotificationsViewModel(repository) { now }
        advanceUntilIdle()
        vm.onAction(NotificationsAction.MarkAllRead)
        advanceUntilIdle()
        assertEquals(0, vm.uiState.value.unreadCount)
        repository.notifications.value = emptyList()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isEmpty)
    }

    @Test
    fun `alerts only fire for new unread notifications after the first load`() {
        val all = SampleSocial.notifications
        assertTrue(NotificationAlerts.fresh(null, all).isEmpty())
        assertEquals(listOf("n3"), NotificationAlerts.fresh(setOf("n1", "n2"), all).map { it.id })
        assertTrue(NotificationAlerts.fresh(all.map { it.id }.toSet(), all).isEmpty())
    }
}
