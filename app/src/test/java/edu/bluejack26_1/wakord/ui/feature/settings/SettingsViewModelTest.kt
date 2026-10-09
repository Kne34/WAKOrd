package edu.bluejack26_1.wakord.ui.feature.settings

import edu.bluejack26_1.wakord.data.fake.FakeAuthRepository
import edu.bluejack26_1.wakord.data.fake.FakePlaybackController
import edu.bluejack26_1.wakord.data.fake.FakeSettingsRepository
import edu.bluejack26_1.wakord.data.fake.FakeSyncRepository
import edu.bluejack26_1.wakord.domain.model.ChordProgression
import edu.bluejack26_1.wakord.domain.model.Difficulty
import edu.bluejack26_1.wakord.domain.model.SyncStatus
import edu.bluejack26_1.wakord.domain.model.ThemeMode
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

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val settings = FakeSettingsRepository()
    private val sync = FakeSyncRepository()
    private val auth = FakeAuthRepository().apply { currentUserId.value = "demo" }
    private val playback = FakePlaybackController()

    private fun viewModel() = SettingsViewModel(settings, sync, auth, playback)

    @Test
    fun `shows stored settings and sync status`() = runTest(main.dispatcher) {
        settings.themeMode.value = ThemeMode.LIGHT
        settings.referencePitch.value = 442f
        settings.pushEnabled.value = false
        settings.keepOffline.value = false
        sync.status.value = SyncStatus(savedSongs = 12, savedChords = 24, isOnline = true, lastSyncedAt = 1L)
        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(ThemeMode.LIGHT, state.themeMode)
        assertEquals(442f, state.referencePitch)
        assertFalse(state.pushEnabled)
        assertFalse(state.keepOffline)
        assertEquals(12, state.sync.savedSongs)
        assertEquals(24, state.sync.savedChords)
        assertTrue(state.sync.isOnline)
    }

    @Test
    fun `changing the theme keeps the choice`() = runTest(main.dispatcher) {
        val vm = viewModel()
        vm.onAction(SettingsAction.SelectTheme(ThemeMode.DARK))
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, settings.themeMode.value)
        assertEquals(ThemeMode.DARK, vm.uiState.value.themeMode)

        vm.onAction(SettingsAction.SelectTheme(ThemeMode.LIGHT))
        advanceUntilIdle()
        assertEquals(ThemeMode.LIGHT, settings.themeMode.value)

        val reopened = viewModel()
        advanceUntilIdle()
        assertEquals(ThemeMode.LIGHT, reopened.uiState.value.themeMode)
    }

    @Test
    fun `reference pitch moves in whole hertz inside the range`() = runTest(main.dispatcher) {
        val vm = viewModel()
        vm.onAction(SettingsAction.OpenReferencePitch)
        assertTrue(vm.uiState.value.isPitchSheetOpen)

        vm.onAction(SettingsAction.SetReferencePitch(441.6f))
        advanceUntilIdle()
        assertEquals(442f, settings.referencePitch.value)

        vm.onAction(SettingsAction.SetReferencePitch(500f))
        advanceUntilIdle()
        assertEquals(SettingsViewModel.MAX_PITCH.toFloat(), settings.referencePitch.value)

        vm.onAction(SettingsAction.SetReferencePitch(400f))
        advanceUntilIdle()
        assertEquals(SettingsViewModel.MIN_PITCH.toFloat(), settings.referencePitch.value)

        vm.onAction(SettingsAction.CloseReferencePitch)
        assertFalse(vm.uiState.value.isPitchSheetOpen)
    }

    @Test
    fun `push and offline switches are saved`() = runTest(main.dispatcher) {
        val vm = viewModel()
        vm.onAction(SettingsAction.SetPushEnabled(false))
        vm.onAction(SettingsAction.SetKeepOffline(false))
        advanceUntilIdle()
        assertFalse(settings.pushEnabled.value)
        assertFalse(settings.keepOffline.value)
        assertFalse(vm.uiState.value.pushEnabled)
        assertFalse(vm.uiState.value.keepOffline)
    }

    @Test
    fun `cancelling the log out popup keeps the user signed in`() = runTest(main.dispatcher) {
        val vm = viewModel()
        vm.onAction(SettingsAction.RequestLogout)
        assertTrue(vm.uiState.value.isLogoutDialogOpen)

        vm.onAction(SettingsAction.DismissLogout)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLogoutDialogOpen)
        assertFalse(vm.uiState.value.isLoggedOut)
        assertEquals("demo", auth.currentUserId.value)
    }

    @Test
    fun `confirming log out signs out and stops playback`() = runTest(main.dispatcher) {
        playback.play(ChordProgression("p1", "Loop", "C", 90, "Pop", Difficulty.EASY, listOf("C", "G"), "u", "User"))
        val vm = viewModel()
        vm.onAction(SettingsAction.RequestLogout)
        vm.onAction(SettingsAction.ConfirmLogout)
        advanceUntilIdle()

        assertNull(auth.currentUserId.value)
        assertNull(playback.state.value.progression)
        assertTrue(vm.uiState.value.isLoggedOut)
        assertFalse(vm.uiState.value.isLogoutDialogOpen)
    }
}
