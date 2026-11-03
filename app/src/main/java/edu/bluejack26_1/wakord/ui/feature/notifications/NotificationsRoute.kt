package edu.bluejack26_1.wakord.ui.feature.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.bluejack26_1.wakord.domain.model.TargetType
import edu.bluejack26_1.wakord.ui.components.ErrorToast

@Composable
fun NotificationsRoute(
    onBack: () -> Unit,
    onOpenSong: (String) -> Unit,
    onOpenProgression: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    viewModel: NotificationsViewModel = viewModel(factory = NotificationsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is NotificationsEffect.Navigate -> when (effect.targetType) {
                    TargetType.SONG -> onOpenSong(effect.targetId)
                    TargetType.PROGRESSION -> onOpenProgression(effect.targetId)
                    TargetType.PROFILE -> onOpenProfile(effect.targetId)
                }
            }
        }
    }
    ErrorToast(state.errorMessage) { viewModel.onAction(NotificationsAction.DismissError) }
    NotificationsScreen(state, viewModel::onAction, onBack)
}
