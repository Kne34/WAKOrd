package edu.bluejack26_1.wakord.ui.feature.profile

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.ui.components.ChordSheet
import edu.bluejack26_1.wakord.ui.components.ErrorToast
import edu.bluejack26_1.wakord.ui.feature.chord.ChordSheetAction
import edu.bluejack26_1.wakord.ui.feature.chord.ChordSheetViewModel
import edu.bluejack26_1.wakord.ui.feature.notifications.NotificationBadgeViewModel

@Composable
fun ProfileRoute(
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onEditProfile: () -> Unit,
    onNewProgression: () -> Unit,
    onOpenSong: (String) -> Unit,
    onOpenProgression: (String) -> Unit,
    onExplore: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
    badgeViewModel: NotificationBadgeViewModel = viewModel(factory = NotificationBadgeViewModel.Factory),
    chordSheetViewModel: ChordSheetViewModel = viewModel(factory = ChordSheetViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val unread by badgeViewModel.unreadCount.collectAsStateWithLifecycle()
    val chordSheet by chordSheetViewModel.uiState.collectAsStateWithLifecycle()
    ErrorToast(state.errorMessage) { viewModel.onAction(ProfileAction.DismissError) }
    ErrorToast(chordSheet.errorMessage) { chordSheetViewModel.onAction(ChordSheetAction.DismissError) }
    ProfileScreen(
        state = state,
        onAction = viewModel::onAction,
        unreadNotifications = unread,
        onOpenNotifications = onOpenNotifications,
        onOpenSettings = onOpenSettings,
        onEditProfile = onEditProfile,
        onNewProgression = onNewProgression,
        onOpenSong = onOpenSong,
        onOpenChord = { chordSheetViewModel.onAction(ChordSheetAction.Open(it)) },
        onOpenProgression = onOpenProgression,
        onExplore = onExplore,
    )
    val symbol = chordSheet.symbol
    if (symbol != null) {
        ChordSheet(
            symbol = symbol,
            voicings = chordSheet.voicings,
            selectedVoicing = chordSheet.selectedVoicing,
            isSaved = chordSheet.isSaved,
            onSelectVoicing = { chordSheetViewModel.onAction(ChordSheetAction.SelectVoicing(it)) },
            onPreview = { chordSheetViewModel.onAction(ChordSheetAction.Preview) },
            onToggleSaved = { chordSheetViewModel.onAction(ChordSheetAction.ToggleSaved) },
            onClose = { chordSheetViewModel.onAction(ChordSheetAction.Close) },
        )
    }
}

@Composable
fun EditProfileRoute(
    onBack: () -> Unit,
    viewModel: EditProfileViewModel = viewModel(factory = EditProfileViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onAction(EditProfileAction.PhotoPicked(uri.toString()))
    }
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onBack()
    }
    ErrorToast(state.errorMessage) { viewModel.onAction(EditProfileAction.DismissError) }
    EditProfileScreen(
        state = state,
        onAction = viewModel::onAction,
        onPickPhoto = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onBack = onBack,
    )
}

@Composable
fun NewProgressionRoute(
    onClose: () -> Unit,
    onViewProgression: (String) -> Unit,
    viewModel: NewProgressionViewModel = viewModel(factory = NewProgressionViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val savedMessage = stringResource(R.string.new_prog_draft_saved)
    var announceDraft by remember { mutableStateOf(false) }
    val saveAndClose = { announce: Boolean ->
        announceDraft = announce
        viewModel.onAction(NewProgressionAction.SaveDraft)
    }
    LaunchedEffect(state.isDraftSaved) {
        if (state.isDraftSaved) {
            if (announceDraft) Toast.makeText(context, savedMessage, Toast.LENGTH_SHORT).show()
            onClose()
        }
    }
    BackHandler { saveAndClose(false) }
    ErrorToast(state.errorMessage) { viewModel.onAction(NewProgressionAction.DismissError) }
    NewProgressionScreen(
        state = state,
        onAction = viewModel::onAction,
        onClose = { saveAndClose(false) },
        onSaveDraft = { saveAndClose(true) },
        onViewProgression = { id ->
            viewModel.onAction(NewProgressionAction.DismissUploaded)
            onViewProgression(id)
        },
        onBackToProfile = {
            viewModel.onAction(NewProgressionAction.DismissUploaded)
            onClose()
        },
    )
}

@Composable
fun OtherProfileRoute(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenProgression: (String) -> Unit,
    viewModel: OtherProfileViewModel = viewModel(factory = OtherProfileViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val user = state.user
    val shareText = stringResource(R.string.other_share_text, user?.fullName.orEmpty(), user?.username.orEmpty())
    val shareTitle = stringResource(R.string.other_share_cd)
    ErrorToast(state.errorMessage) { viewModel.onAction(OtherProfileAction.DismissError) }
    OtherProfileScreen(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        onShare = {
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText)
            context.startActivity(Intent.createChooser(send, shareTitle))
        },
        onEditProfile = onEditProfile,
        onOpenProgression = onOpenProgression,
    )
}
