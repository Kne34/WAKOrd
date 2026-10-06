package edu.bluejack26_1.wakord.ui.feature.auth

import app.cash.turbine.test
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.core.error.AppError
import edu.bluejack26_1.wakord.data.fake.FakeAuthRepository
import edu.bluejack26_1.wakord.domain.model.Instrument
import edu.bluejack26_1.wakord.domain.validation.FieldError
import edu.bluejack26_1.wakord.testutil.FakeAuthValidator
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
class AuthViewModelsTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val repository = FakeAuthRepository()
    private val validator = FakeAuthValidator()

    @Test
    fun `login success navigates home`() = runTest(main.dispatcher) {
        val vm = LoginViewModel(repository, validator)
        vm.effects.test {
            vm.onAction(LoginAction.EmailChanged("keane.wilson@mail.com"))
            vm.onAction(LoginAction.PasswordChanged("wakordpass"))
            vm.onAction(LoginAction.Submit)
            assertEquals(LoginEffect.NavigateHome, awaitItem())
        }
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(repository.isLoggedIn)
    }

    @Test
    fun `wrong password shows login failed popup`() = runTest(main.dispatcher) {
        repository.loginResult = Result.failure(AppError.InvalidCredentials)
        val vm = LoginViewModel(repository, validator)
        vm.onAction(LoginAction.Submit)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.showLoginFailed)

        vm.onAction(LoginAction.DismissLoginFailed)
        assertFalse(vm.uiState.value.showLoginFailed)
    }

    @Test
    fun `invalid input does not call firebase`() = runTest(main.dispatcher) {
        validator.emailError = FieldError.REQUIRED
        repository.loginResult = Result.failure(IllegalStateException("should not be called"))
        val vm = LoginViewModel(repository, validator)
        vm.onAction(LoginAction.Submit)
        advanceUntilIdle()
        assertEquals(FieldError.REQUIRED, vm.uiState.value.emailError)
        assertFalse(vm.uiState.value.showLoginFailed)
    }

    @Test
    fun `register success shows account created then goes to preferences`() = runTest(main.dispatcher) {
        val vm = RegisterViewModel(repository, validator)
        vm.onAction(RegisterAction.Submit)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.showAccountCreated)

        vm.effects.test {
            vm.onAction(RegisterAction.ContinueAfterCreated)
            assertEquals(RegisterEffect.NavigateToPreferences, awaitItem())
        }
        assertFalse(vm.uiState.value.showAccountCreated)
    }

    @Test
    fun `email already used shows message`() = runTest(main.dispatcher) {
        repository.registerResult = Result.failure(AppError.EmailAlreadyUsed)
        val vm = RegisterViewModel(repository, validator)
        vm.onAction(RegisterAction.Submit)
        advanceUntilIdle()
        assertEquals(R.string.error_email_used, vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.showAccountCreated)
    }

    @Test
    fun `register shows every field error at once`() = runTest(main.dispatcher) {
        validator.fullNameError = FieldError.REQUIRED
        validator.passwordError = FieldError.PASSWORD_TOO_SHORT
        validator.confirmError = FieldError.PASSWORD_MISMATCH
        val vm = RegisterViewModel(repository, validator)
        vm.onAction(RegisterAction.Submit)
        advanceUntilIdle()
        val s = vm.uiState.value
        assertEquals(FieldError.REQUIRED, s.fullNameError)
        assertEquals(FieldError.PASSWORD_TOO_SHORT, s.passwordError)
        assertEquals(FieldError.PASSWORD_MISMATCH, s.confirmPasswordError)
        assertFalse(repository.isLoggedIn)
    }

    @Test
    fun `preferences are saved then navigate home`() = runTest(main.dispatcher) {
        val vm = PreferencesViewModel(repository, validator)
        vm.onAction(PreferencesAction.ToggleInstrument(Instrument.GUITAR))
        listOf("Pop", "Jazz", "Lo-fi").forEach { vm.onAction(PreferencesAction.ToggleGenre(it)) }
        vm.effects.test {
            vm.onAction(PreferencesAction.Continue)
            assertEquals(PreferencesEffect.NavigateHome, awaitItem())
        }
        assertEquals(setOf(Instrument.GUITAR), repository.savedInstruments)
        assertEquals(setOf("Pop", "Jazz", "Lo-fi"), repository.savedGenres)
    }

    @Test
    fun `toggling a genre twice removes it`() = runTest(main.dispatcher) {
        val vm = PreferencesViewModel(repository, validator)
        vm.onAction(PreferencesAction.ToggleGenre("Pop"))
        vm.onAction(PreferencesAction.ToggleGenre("Pop"))
        assertTrue(vm.uiState.value.selectedGenres.isEmpty())
    }

    @Test
    fun `too few genres blocks saving`() = runTest(main.dispatcher) {
        validator.genresError = FieldError.TOO_FEW_GENRES
        val vm = PreferencesViewModel(repository, validator)
        vm.onAction(PreferencesAction.Continue)
        advanceUntilIdle()
        assertEquals(FieldError.TOO_FEW_GENRES, vm.uiState.value.genreError)
        assertNull(repository.savedGenres)
    }

    @Test
    fun `reset link sent shows check your email state`() = runTest(main.dispatcher) {
        val vm = ResetPasswordViewModel(repository, validator)
        vm.onAction(ResetPasswordAction.EmailChanged("keane.wilson@mail.com"))
        vm.onAction(ResetPasswordAction.Send)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isSent)
        assertEquals("keane.wilson@mail.com", repository.resetEmail)
    }

    @Test
    fun `reset failure shows network message`() = runTest(main.dispatcher) {
        repository.resetResult = Result.failure(AppError.Network)
        val vm = ResetPasswordViewModel(repository, validator)
        vm.onAction(ResetPasswordAction.Send)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isSent)
        assertEquals(R.string.error_network, vm.uiState.value.errorMessage)
    }

    @Test
    fun `splash knows when user is still logged in`() = runTest(main.dispatcher) {
        assertFalse(SplashViewModel(repository).isLoggedIn)
        repository.login("a@b.com", "wakordpass")
        assertTrue(SplashViewModel(repository).isLoggedIn)
        repository.logout()
        assertFalse(SplashViewModel(repository).isLoggedIn)
    }
}
