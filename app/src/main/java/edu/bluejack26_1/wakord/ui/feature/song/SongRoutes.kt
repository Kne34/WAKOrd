package edu.bluejack26_1.wakord.ui.feature.song

import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.WAKordApp

@Composable
fun SongDetailRoute(
    onBack: () -> Unit,
    onOpenComments: () -> Unit,
    viewModel: SongDetailViewModel = viewModel(factory = SongDetailViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val song = state.song
    val shareText = if (song != null) stringResource(R.string.song_share_text, song.title, song.artist) else ""
    val chooserTitle = stringResource(R.string.share)
    ErrorToast(state.errorMessage) { viewModel.onAction(SongDetailAction.DismissError) }
    SongDetailScreen(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        onOpenComments = onOpenComments,
        onShare = {
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText)
            context.startActivity(Intent.createChooser(send, chooserTitle))
        },
    )
}

@Composable
fun SongCommentsRoute(
    onBack: () -> Unit,
    viewModel: SongDetailViewModel = viewModel(factory = SongDetailViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currentUser = remember { (context.applicationContext as WAKordApp).container.userRepository.currentUser() }
    val me by currentUser.collectAsStateWithLifecycle(initialValue = null)
    ErrorToast(state.errorMessage) { viewModel.onAction(SongDetailAction.DismissError) }
    CommentsScreen(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        myName = me?.fullName.orEmpty(),
        myId = me?.uid.orEmpty(),
    )
}

@Composable
private fun ErrorToast(message: Int?, onShown: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(message) {
        if (message != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            onShown()
        }
    }
}
