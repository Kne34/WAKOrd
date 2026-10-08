package edu.bluejack26_1.wakord.ui.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.bluejack26_1.wakord.ui.components.ErrorToast

@Composable
fun HomeRoute(
    onOpenSong: (String) -> Unit,
    onOpenProgression: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenTraining: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ErrorToast(state.errorMessage) { viewModel.onAction(HomeAction.DismissError) }
    HomeScreen(
        state = state,
        onAction = viewModel::onAction,
        onOpenSong = onOpenSong,
        onOpenProgression = onOpenProgression,
        onOpenSearch = onOpenSearch,
        onOpenNotifications = onOpenNotifications,
        onOpenProfile = onOpenProfile,
        onOpenTraining = onOpenTraining,
    )
}
