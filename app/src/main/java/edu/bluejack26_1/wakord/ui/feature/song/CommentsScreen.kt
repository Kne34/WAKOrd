package edu.bluejack26_1.wakord.ui.feature.song

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import edu.bluejack26_1.wakord.ui.components.CommentsPage
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme

@Composable
fun CommentsScreen(
    state: SongDetailUiState,
    onAction: (SongDetailAction) -> Unit,
    onBack: () -> Unit,
    myName: String,
    myId: String,
    modifier: Modifier = Modifier,
) {
    CommentsPage(
        title = state.song?.title.orEmpty(),
        comments = state.comments,
        draft = state.commentDraft,
        isPosting = state.isPosting,
        showBlocked = state.showCommentBlocked,
        myName = myName,
        myId = myId,
        onBack = onBack,
        onDraftChange = { onAction(SongDetailAction.CommentChanged(it)) },
        onPost = { onAction(SongDetailAction.PostComment) },
        onLike = { onAction(SongDetailAction.ToggleCommentLike(it)) },
        onDismissBlocked = { onAction(SongDetailAction.DismissCommentBlocked) },
        modifier = modifier,
        replyingTo = state.replyingTo,
        onReply = { onAction(SongDetailAction.ReplyTo(it)) },
    )
}

private val commentsPreviewState = SongDetailUiState(isLoading = false, song = PreviewSong, comments = PreviewComments)

@Preview(name = "Comments", heightDp = 844)
@Composable
private fun CommentsPreview() {
    WAKordTheme(darkTheme = true) {
        CommentsScreen(commentsPreviewState, {}, {}, "Keane Wilson", "me")
    }
}

@Preview(name = "Comments - blocked", heightDp = 844)
@Composable
private fun CommentsBlockedPreview() {
    WAKordTheme(darkTheme = true) {
        CommentsScreen(
            commentsPreviewState.copy(commentDraft = "you play like garbage, quit already", showCommentBlocked = true),
            {},
            {},
            "Keane Wilson",
            "me",
        )
    }
}

@Preview(name = "Comments - empty", heightDp = 844)
@Composable
private fun CommentsEmptyPreview() {
    WAKordTheme(darkTheme = true) {
        CommentsScreen(SongDetailUiState(isLoading = false, song = PreviewSong), {}, {}, "Keane Wilson", "me")
    }
}
