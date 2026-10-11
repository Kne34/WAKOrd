package edu.bluejack26_1.wakord.ui.feature.analyze

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.ui.components.ChordSheet
import edu.bluejack26_1.wakord.ui.components.ErrorToast
import edu.bluejack26_1.wakord.ui.components.keyLabel
import edu.bluejack26_1.wakord.ui.feature.chord.ChordSheetAction
import edu.bluejack26_1.wakord.ui.feature.chord.ChordSheetViewModel
import edu.bluejack26_1.wakord.ui.feature.notifications.NotificationBadgeViewModel

private fun Context.hasMicPermission() =
    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

@Composable
fun AnalyzeRoute(
    onOpenAnalyzing: () -> Unit,
    onOpenResult: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    viewModel: AnalyzeViewModel = viewModel(factory = AnalyzeViewModel.Factory),
    badgeViewModel: NotificationBadgeViewModel = viewModel(factory = NotificationBadgeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val unread by badgeViewModel.unreadCount.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(context.hasMicPermission()) }
    LifecycleResumeEffect(Unit) {
        hasPermission = context.hasMicPermission()
        onPauseOrDispose {}
    }
    val activity = LocalActivity.current
    LifecycleStartEffect(viewModel) {
        onStopOrDispose {
            val rotating = activity?.isChangingConfigurations == true
            if (!rotating && viewModel.uiState.value.isRecording) viewModel.onAction(AnalyzeAction.CancelAnalysis)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        viewModel.onAction(if (granted) AnalyzeAction.ToggleRecording else AnalyzeAction.MicrophoneDenied)
    }
    val resultId = state.result?.id
    LaunchedEffect(state.isAnalyzing, resultId) {
        when {
            state.isAnalyzing -> onOpenAnalyzing()
            resultId != null -> onOpenResult(resultId)
        }
    }
    ErrorToast(state.errorMessage) { viewModel.onAction(AnalyzeAction.DismissError) }

    AnalyzeScreen(
        state = state,
        onAction = viewModel::onAction,
        microphoneOff = state.microphoneOff && !hasPermission,
        unreadNotifications = unread,
        onToggleRecording = {
            if (!state.isRecording && !hasPermission) {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                viewModel.onAction(AnalyzeAction.ToggleRecording)
            }
        },
        onOpenNotifications = onOpenNotifications,
        onOpenSettings = {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
        },
    )
}

@Composable
fun AnalyzingRoute(
    onShowResult: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: AnalyzeViewModel = viewModel(factory = AnalyzeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val resultId = state.result?.id
    LaunchedEffect(state.isAnalyzing, resultId) {
        when {
            resultId != null -> onShowResult(resultId)
            !state.isAnalyzing -> onBack()
        }
    }
    BackHandler { viewModel.onAction(AnalyzeAction.CancelAnalysis) }
    AnalyzingScreen(
        stage = state.stage,
        recordedMs = state.elapsedMs,
        onCancel = { viewModel.onAction(AnalyzeAction.CancelAnalysis) },
    )
}

@Composable
fun AnalysisResultRoute(
    onBack: () -> Unit,
    onOpenSong: (String) -> Unit,
    onOpenProgression: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    viewModel: AnalyzeViewModel = viewModel(factory = AnalyzeViewModel.Factory),
    chordSheetViewModel: ChordSheetViewModel = viewModel(factory = ChordSheetViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chordSheet by chordSheetViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val result = state.result
    LaunchedEffect(result == null) {
        if (result == null) onBack()
    }
    val close = {
        viewModel.onAction(AnalyzeAction.CloseResult)
    }
    BackHandler { close() }
    ErrorToast(state.errorMessage) { viewModel.onAction(AnalyzeAction.DismissError) }
    ErrorToast(chordSheet.errorMessage) { chordSheetViewModel.onAction(ChordSheetAction.DismissError) }
    if (result == null) return

    val shareText = stringResource(R.string.result_share_text, keyLabel(result.key), chordLine(result))
    val shareTitle = stringResource(R.string.result_share_cd)
    AnalysisResultScreen(
        state = state,
        onAction = viewModel::onAction,
        onBack = close,
        onShare = {
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText)
            context.startActivity(Intent.createChooser(send, shareTitle))
        },
        onOpenChord = { chordSheetViewModel.onAction(ChordSheetAction.Open(it)) },
        onOpenSong = onOpenSong,
        onOpenProgression = onOpenProgression,
        onOpenArtist = onOpenArtist,
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
