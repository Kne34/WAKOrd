package edu.bluejack26_1.wakord.ui.feature.explore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.bluejack26_1.wakord.ui.components.ErrorToast
import edu.bluejack26_1.wakord.ui.feature.chord.ChordSheetAction
import edu.bluejack26_1.wakord.ui.feature.chord.ChordSheetViewModel
import edu.bluejack26_1.wakord.ui.feature.notifications.NotificationBadgeViewModel

@Composable
fun ExploreRoute(
    onOpenSong: (String) -> Unit,
    onOpenProgression: (String) -> Unit,
    onOpenSection: (ExploreSection) -> Unit,
    onOpenNotifications: () -> Unit,
    viewModel: ExploreViewModel = viewModel(factory = ExploreViewModel.Factory),
    chordSheetViewModel: ChordSheetViewModel = viewModel(factory = ChordSheetViewModel.Factory),
    badgeViewModel: NotificationBadgeViewModel = viewModel(factory = NotificationBadgeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val unread by badgeViewModel.unreadCount.collectAsStateWithLifecycle()
    val chordSheet by chordSheetViewModel.uiState.collectAsStateWithLifecycle()
    ErrorToast(state.errorMessage) { viewModel.onAction(ExploreAction.DismissError) }
    ErrorToast(chordSheet.errorMessage) { chordSheetViewModel.onAction(ChordSheetAction.DismissError) }
    ExploreScreen(
        state = state,
        onAction = viewModel::onAction,
        chordSheet = chordSheet,
        onChordAction = chordSheetViewModel::onAction,
        onOpenSong = onOpenSong,
        onOpenProgression = onOpenProgression,
        onOpenSection = onOpenSection,
        onOpenNotifications = onOpenNotifications,
        unreadNotifications = unread,
    )
}

@Composable
fun ExploreSectionRoute(
    section: ExploreSection,
    onBack: () -> Unit,
    onOpenSong: (String) -> Unit,
    onOpenProgression: (String) -> Unit,
    viewModel: ExploreViewModel = viewModel(factory = ExploreViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ErrorToast(state.errorMessage) { viewModel.onAction(ExploreAction.DismissError) }
    ExploreSectionScreen(
        section = section,
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        onOpenSong = onOpenSong,
        onOpenProgression = onOpenProgression,
    )
}
