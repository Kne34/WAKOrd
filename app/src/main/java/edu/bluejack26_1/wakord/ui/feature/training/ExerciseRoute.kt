package edu.bluejack26_1.wakord.ui.feature.training

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.ui.components.ErrorToast

@Composable
fun ExerciseRoute(
    onBack: () -> Unit,
    onOpenLevel: (Int) -> Unit,
    viewModel: ExerciseViewModel = viewModel(factory = ExerciseViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showQuitDialog by rememberSaveable { mutableStateOf(false) }
    val level = state.level ?: return
    val summary = state.summary

    BackHandler(enabled = summary == null) { showQuitDialog = true }
    ErrorToast(state.errorMessage) { viewModel.onAction(ExerciseAction.DismissError) }

    if (summary == null) {
        ExerciseScreen(
            state = state,
            onAction = viewModel::onAction,
            showQuitDialog = showQuitDialog,
            onQuit = { showQuitDialog = true },
            onConfirmQuit = {
                showQuitDialog = false
                onBack()
            },
            onDismissQuit = { showQuitDialog = false },
        )
    } else {
        val shareText = stringResource(R.string.complete_share_text, summary.correct, summary.total, stringResource(skillName(level.skill)), level.number)
        val shareTitle = stringResource(R.string.complete_share_cd)
        SessionCompleteScreen(
            level = level,
            summary = summary,
            onClose = onBack,
            onShare = {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText)
                context.startActivity(Intent.createChooser(send, shareTitle))
            },
            onRetry = { viewModel.onAction(ExerciseAction.Retry) },
            onNextLevel = onOpenLevel,
        )
    }
}
