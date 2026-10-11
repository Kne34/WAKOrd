package edu.bluejack26_1.wakord.ui.feature.tuner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.ui.components.WakDialog
import edu.bluejack26_1.wakord.ui.feature.notifications.NotificationBadgeViewModel

private fun Context.hasMicPermission() =
    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

@Composable
fun TunerRoute(
    onOpenNotifications: () -> Unit,
    viewModel: TunerViewModel = viewModel(factory = TunerViewModel.Factory),
    badgeViewModel: NotificationBadgeViewModel = viewModel(factory = NotificationBadgeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val unread by badgeViewModel.unreadCount.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAllowMic by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onAction(TunerAction.PermissionResult(granted))
    }

    LifecycleStartEffect(Unit) {
        if (context.hasMicPermission()) {
            viewModel.onAction(TunerAction.PermissionResult(true))
        } else if (viewModel.uiState.value.micPermission == MicPermission.UNKNOWN) {
            showAllowMic = true
        }
        onStopOrDispose { viewModel.onAction(TunerAction.StopListening) }
    }

    if (showAllowMic) {
        WakDialog(
            title = stringResource(R.string.allow_mic_title),
            message = stringResource(R.string.allow_mic_message),
            primaryText = stringResource(R.string.allow_microphone),
            onPrimary = {
                showAllowMic = false
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            onDismissRequest = {
                showAllowMic = false
                viewModel.onAction(TunerAction.PermissionResult(false))
            },
            secondaryText = stringResource(R.string.not_now),
            icon = painterResource(R.drawable.ic_mic),
        )
    }

    TunerScreen(
        state = state,
        onAction = viewModel::onAction,
        onOpenSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
            )
        },
        onOpenNotifications = onOpenNotifications,
        unreadNotifications = unread,
    )
}
