package edu.bluejack26_1.wakord.ui.feature.progression

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.WAKordApp
import edu.bluejack26_1.wakord.ui.components.CommentsPage
import edu.bluejack26_1.wakord.ui.components.ErrorToast

@Composable
fun ProgressionDetailRoute(
    onBack: () -> Unit,
    onOpenComments: () -> Unit,
    onOpenAuthor: (String) -> Unit,
    viewModel: ProgressionDetailViewModel = viewModel(factory = ProgressionDetailViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val progression = state.progression
    val shareText = if (progression != null) stringResource(R.string.progression_share_text, progression.title, progression.authorName) else ""
    val chooserTitle = stringResource(R.string.share)
    ErrorToast(state.errorMessage) { viewModel.onAction(ProgressionDetailAction.DismissError) }
    ProgressionDetailScreen(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        onShare = {
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText)
            context.startActivity(Intent.createChooser(send, chooserTitle))
        },
        onOpenComments = onOpenComments,
        onOpenAuthor = onOpenAuthor,
    )
}

@Composable
fun ProgressionCommentsRoute(
    onBack: () -> Unit,
    viewModel: ProgressionDetailViewModel = viewModel(factory = ProgressionDetailViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currentUser = remember { (context.applicationContext as WAKordApp).container.userRepository.currentUser() }
    val me by currentUser.collectAsStateWithLifecycle(initialValue = null)
    ErrorToast(state.errorMessage) { viewModel.onAction(ProgressionDetailAction.DismissError) }
    CommentsPage(
        title = state.progression?.title.orEmpty(),
        comments = state.comments,
        draft = state.commentDraft,
        isPosting = state.isPosting,
        showBlocked = state.showCommentBlocked,
        myName = me?.fullName.orEmpty(),
        myId = me?.uid.orEmpty(),
        onBack = onBack,
        onDraftChange = { viewModel.onAction(ProgressionDetailAction.CommentChanged(it)) },
        onPost = { viewModel.onAction(ProgressionDetailAction.PostComment) },
        onLike = { viewModel.onAction(ProgressionDetailAction.ToggleCommentLike(it)) },
        onDismissBlocked = { viewModel.onAction(ProgressionDetailAction.DismissCommentBlocked) },
    )
}
