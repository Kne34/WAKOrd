package edu.bluejack26_1.wakord.ui.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginRoute(
    onLoggedIn: () -> Unit,
    onRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                LoginEffect.NavigateHome -> onLoggedIn()
            }
        }
    }
    LoginScreen(state, viewModel::onAction, onForgotPassword, onRegister)
}

@Composable
fun RegisterRoute(
    onRegistered: () -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: RegisterViewModel = viewModel(factory = RegisterViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                RegisterEffect.NavigateToPreferences -> onRegistered()
            }
        }
    }
    RegisterScreen(state, viewModel::onAction, onBackToLogin)
}

@Composable
fun PreferencesRoute(
    onDone: () -> Unit,
    viewModel: PreferencesViewModel = viewModel(factory = PreferencesViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PreferencesEffect.NavigateHome -> onDone()
            }
        }
    }
    PreferencesScreen(state, viewModel::onAction)
}

@Composable
fun ResetPasswordRoute(
    onBack: () -> Unit,
    viewModel: ResetPasswordViewModel = viewModel(factory = ResetPasswordViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ResetPasswordScreen(state, viewModel::onAction, onBack)
}
