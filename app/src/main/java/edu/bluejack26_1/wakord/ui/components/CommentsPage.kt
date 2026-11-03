package edu.bluejack26_1.wakord.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.domain.model.Comment
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.jakarta
import edu.bluejack26_1.wakord.ui.theme.spaceGrotesk

enum class CommentSort { TOP, NEWEST }

private data class CommentEntry(val comment: Comment, val isReply: Boolean)

private fun threaded(comments: List<Comment>, sort: CommentSort): List<CommentEntry> {
    val replies = comments.filter { it.parentId != null }.groupBy { it.parentId }
    val roots = comments.filter { it.parentId == null || comments.none { c -> c.id == it.parentId } }
    val ordered = when (sort) {
        CommentSort.TOP -> roots.sortedWith(compareByDescending<Comment> { it.likeCount }.thenByDescending { it.createdAt })
        CommentSort.NEWEST -> roots.sortedByDescending { it.createdAt }
    }
    return ordered.flatMap { root ->
        listOf(CommentEntry(root, false)) + replies[root.id].orEmpty().sortedBy { it.createdAt }.map { CommentEntry(it, true) }
    }
}

@Composable
fun CommentsPage(
    title: String,
    comments: List<Comment>,
    draft: String,
    isPosting: Boolean,
    showBlocked: Boolean,
    myName: String,
    myId: String,
    onBack: () -> Unit,
    onDraftChange: (String) -> Unit,
    onPost: () -> Unit,
    onLike: (Comment) -> Unit,
    onDismissBlocked: () -> Unit,
    modifier: Modifier = Modifier,
    replyingTo: Comment? = null,
    onReply: ((Comment?) -> Unit)? = null,
) {
    val c = WakTheme.colors
    var sort by rememberSaveable { mutableStateOf(CommentSort.TOP) }
    val entries = remember(comments, sort) { threaded(comments, sort) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
            .imePadding(),
    ) {
        CommentsHeader(
            title = title,
            count = comments.size,
            sort = sort,
            onSort = { sort = it },
            onBack = onBack,
        )
        if (entries.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(c.s3, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_comment), contentDescription = null, tint = c.primaryText, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.comments_empty_title), style = spaceGrotesk(20.sp), color = c.text)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.comments_empty_message), style = jakarta(14.sp), color = c.text2, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                items(entries, key = { it.comment.id }) { entry ->
                    CommentRow(
                        comment = entry.comment,
                        isReply = entry.isReply,
                        onReply = onReply?.let { reply ->
                            { reply(if (entry.isReply) comments.firstOrNull { it.id == entry.comment.parentId } ?: entry.comment else entry.comment) }
                        },
                        onLike = { onLike(entry.comment) },
                    )
                }
            }
        }
        Composer(draft, isPosting, showBlocked, replyingTo, myName, myId, onDraftChange, onPost, onDismissBlocked, onReply)
    }
}

@Composable
private fun CommentsHeader(
    title: String,
    count: Int,
    sort: CommentSort,
    onSort: (CommentSort) -> Unit,
    onBack: () -> Unit,
) {
    val c = WakTheme.colors
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassIconButton(painterResource(R.drawable.ic_back), stringResource(R.string.back), onBack)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.comments), style = spaceGrotesk(20.sp), color = c.text)
            Text(stringResource(R.string.comments_subtitle, title, compactCount(count)), style = jakarta(12.sp), color = c.text3, maxLines = 1)
        }
        Box {
            Surface(
                onClick = { menuOpen = true },
                modifier = Modifier.height(40.dp),
                shape = CircleShape,
                color = c.s3,
                border = BorderStroke(1.dp, c.line),
            ) {
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(if (sort == CommentSort.TOP) R.string.comment_sort_top else R.string.comment_sort_newest),
                        style = jakarta(16.sp),
                        color = c.text,
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(painterResource(R.drawable.ic_chevron_down), contentDescription = null, tint = c.text, modifier = Modifier.size(16.dp))
                }
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = c.s3) {
                CommentSort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(if (option == CommentSort.TOP) R.string.comment_sort_top else R.string.comment_sort_newest),
                                style = jakarta(14.sp),
                                color = if (option == sort) c.primaryText else c.text,
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onSort(option)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentRow(comment: Comment, isReply: Boolean, onReply: (() -> Unit)?, onLike: () -> Unit) {
    val c = WakTheme.colors
    val likeCd = pluralStringResource(R.plurals.comment_like_cd, comment.likeCount, comment.likeCount)
    Column(Modifier.padding(start = if (isReply) 70.dp else 20.dp, end = 20.dp)) {
        Row(Modifier.padding(top = 12.dp)) {
            UserAvatar(comment.authorName, comment.authorId, if (isReply) AvatarSmall else AvatarLarge)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(comment.authorName, style = jakarta(13.sp, FontWeight.SemiBold), color = c.text, maxLines = 1)
                    Spacer(Modifier.width(6.dp))
                    Text(relativeTime(comment.createdAt), style = jakarta(12.sp), color = c.text3)
                }
                Spacer(Modifier.height(4.dp))
                Text(comment.text, style = jakarta(14.sp), color = c.text)
                if (onReply != null) {
                    Text(
                        stringResource(R.string.comment_reply),
                        style = jakarta(12.sp, FontWeight.SemiBold),
                        color = c.text3,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onReply)
                            .padding(vertical = 6.dp, horizontal = 2.dp),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .size(width = 44.dp, height = 48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onLike)
                    .semantics { contentDescription = likeCd },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(6.dp))
                Icon(
                    painterResource(if (comment.likedByMe) R.drawable.ic_heart_filled else R.drawable.ic_heart),
                    contentDescription = null,
                    tint = if (comment.likedByMe) c.peach else c.text,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.height(2.dp))
                Text(comment.likeCount.toString(), style = spaceGrotesk(12.sp, FontWeight.Medium), color = c.text3)
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(c.line),
        )
    }
}

@Composable
private fun Composer(
    draft: String,
    isPosting: Boolean,
    showBlocked: Boolean,
    replyingTo: Comment?,
    myName: String,
    myId: String,
    onDraftChange: (String) -> Unit,
    onPost: () -> Unit,
    onDismissBlocked: () -> Unit,
    onReply: ((Comment?) -> Unit)?,
) {
    val c = WakTheme.colors
    val canPost = draft.isNotBlank() && !isPosting
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.s1)
            .border(1.dp, c.line)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 13.dp),
    ) {
        AnimatedVisibility(visible = showBlocked) {
            Row(
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .fillMaxWidth()
                    .background(c.errorSoft, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Icon(painterResource(R.drawable.ic_shield_alert), contentDescription = null, tint = c.error, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(stringResource(R.string.comment_blocked_title), style = spaceGrotesk(14.sp), color = c.error)
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(R.string.comment_blocked_message), style = jakarta(13.sp), color = c.error)
                }
            }
        }
        if (replyingTo != null && onReply != null) {
            Row(
                modifier = Modifier.padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.comment_replying_to, replyingTo.authorName),
                    style = jakarta(12.sp, FontWeight.SemiBold),
                    color = c.primaryText,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.comment_cancel_reply),
                    tint = c.text3,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { onReply(null) }
                        .padding(6.dp),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(myName, myId, AvatarLarge)
            Spacer(Modifier.width(10.dp))
            val shape = RoundedCornerShape(16.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(c.s2, shape)
                    .border(1.dp, if (showBlocked) c.error else c.outline, shape)
                    .padding(horizontal = 17.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (draft.isEmpty()) {
                    Text(stringResource(R.string.comment_placeholder), style = jakarta(15.sp), color = c.text3, maxLines = 1)
                }
                BasicTextField(
                    value = draft,
                    onValueChange = {
                        onDraftChange(it)
                        if (showBlocked) onDismissBlocked()
                    },
                    textStyle = jakarta(15.sp).copy(color = c.text),
                    cursorBrush = SolidColor(c.primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (canPost) onPost() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.width(10.dp))
            Surface(
                onClick = onPost,
                enabled = canPost,
                modifier = Modifier
                    .size(48.dp)
                    .alpha(if (canPost || isPosting) 1f else 0.5f),
                shape = CircleShape,
                color = c.primary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isPosting) {
                        CircularProgressIndicator(color = c.onPrimary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(painterResource(R.drawable.ic_send), contentDescription = stringResource(R.string.comment_post), tint = c.onPrimary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
