package edu.bluejack26_1.wakord.ui.feature.training

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.bluejack26_1.wakord.ui.components.ErrorToast
import edu.bluejack26_1.wakord.ui.feature.notifications.NotificationBadgeViewModel

private fun Context.hasMicPermission() =
    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

@Composable
fun TrainingRoute(
    onBack: () -> Unit,
    onStartExercise: (Int) -> Unit,
    onOpenStats: () -> Unit,
    onOpenNotifications: () -> Unit,
    viewModel: TrainingViewModel = viewModel(factory = TrainingViewModel.Factory),
    pitchViewModel: PitchMonitorViewModel = viewModel(factory = PitchMonitorViewModel.Factory),
    badgeViewModel: NotificationBadgeViewModel = viewModel(factory = NotificationBadgeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pitch by pitchViewModel.uiState.collectAsStateWithLifecycle()
    val unread by badgeViewModel.unreadCount.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pitchViewModel.onAction(if (granted) PitchMonitorAction.ToggleListening else PitchMonitorAction.MicrophoneDenied)
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TrainingEffect.StartExercise -> onStartExercise(effect.level)
            }
        }
    }
    val activity = LocalActivity.current
    LifecycleStartEffect(pitchViewModel) {
        onStopOrDispose {
            val rotating = activity?.isChangingConfigurations == true
            if (!rotating && pitchViewModel.uiState.value.isListening) pitchViewModel.onAction(PitchMonitorAction.ToggleListening)
        }
    }
    ErrorToast(state.errorMessage) { viewModel.onAction(TrainingAction.DismissError) }

    TrainingScreen(
        state = state,
        onAction = viewModel::onAction,
        pitch = pitch,
        onPitchAction = { action ->
            val starting = action == PitchMonitorAction.ToggleListening && !pitch.isListening
            if (starting && !context.hasMicPermission()) {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                pitchViewModel.onAction(action)
            }
        },
        unreadNotifications = unread,
        onBack = onBack,
        onOpenStats = onOpenStats,
        onOpenNotifications = onOpenNotifications,
        onOpenSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
            )
        },
    )
}
