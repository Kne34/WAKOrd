package edu.bluejack26_1.wakord.ui.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.domain.model.AppNotification
import edu.bluejack26_1.wakord.domain.model.NotificationType
import edu.bluejack26_1.wakord.domain.model.TargetType
import edu.bluejack26_1.wakord.ui.components.GlassIconButton
import edu.bluejack26_1.wakord.ui.components.UserAvatar
import edu.bluejack26_1.wakord.ui.components.relativeTime
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.jakarta
import edu.bluejack26_1.wakord.ui.theme.spaceGrotesk

private const val MINUTE_MS = 60_000L

@Composable
fun NotificationsScreen(
    state: NotificationsUiState,
    onAction: (NotificationsAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = WakTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconButton(painterResource(R.drawable.ic_back), stringResource(R.string.back), onBack)
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.nav_notifications),
                style = spaceGrotesk(20.sp),
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (state.unreadCount > 0) {
                Text(
                    stringResource(R.string.notifications_mark_all),
                    style = jakarta(14.sp),
                    color = c.primaryText,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onAction(NotificationsAction.MarkAllRead) }
                        .padding(horizontal = 4.dp, vertical = 11.dp),
                )
            }
        }
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.primary)
            }
            state.isEmpty -> EmptyNotifications()
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            ) {
                if (state.newItems.isNotEmpty()) {
                    item(key = "new") {
                        SectionLabel(stringResource(R.string.notifications_new, state.newItems.size), Modifier.padding(top = 8.dp, bottom = 8.dp))
                    }
                    items(state.newItems, key = { it.id }) { notification ->
                        NotificationRow(notification, onClick = { onAction(NotificationsAction.Open(notification)) })
                    }
                }
                if (state.earlierItems.isNotEmpty()) {
                    item(key = "earlier") {
                        SectionLabel(
                            stringResource(R.string.notifications_earlier),
                            Modifier.padding(top = if (state.newItems.isEmpty()) 8.dp else 22.dp, bottom = 8.dp),
                        )
                    }
                    items(state.earlierItems, key = { it.id }) { notification ->
                        NotificationRow(notification, onClick = { onAction(NotificationsAction.Open(notification)) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = spaceGrotesk(11.sp).copy(letterSpacing = 1.1.sp, lineHeight = 14.sp),
        color = WakTheme.colors.text3,
        maxLines = 1,
        modifier = modifier.padding(horizontal = 20.dp),
    )
}

@Composable
private fun notificationText(notification: AppNotification): String {
    val actor = notification.actorName
    return when (notification.type) {
        NotificationType.LIKE -> if (notification.targetType == TargetType.PROGRESSION) {
            stringResource(R.string.notifications_row_like_progression, actor, notification.targetTitle)
        } else {
            stringResource(R.string.notifications_row_like, actor, notification.targetTitle)
        }
        NotificationType.COMMENT -> stringResource(R.string.notifications_row_comment, actor, notification.message)
        NotificationType.REPLY -> stringResource(R.string.notifications_row_reply, actor, notification.message)
        NotificationType.FOLLOW -> stringResource(R.string.notification_follow, actor)
    }
}

@Composable
private fun timeLabel(createdAt: Long): String {
    val now = System.currentTimeMillis()
    val text = relativeTime(createdAt, now)
    return if (now - createdAt < MINUTE_MS) text else stringResource(R.string.time_ago, text)
}

@Composable
private fun NotificationRow(notification: AppNotification, onClick: () -> Unit) {
    val c = WakTheme.colors
    val unread = !notification.isRead
    val unreadLabel = stringResource(R.string.notifications_unread_cd)
    val rowColor = c.primarySoft
    val lineColor = c.line
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (unread) {
                    drawRect(rowColor)
                    val y = size.height - 0.5.dp.toPx()
                    drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }
            }
            .clickable(onClick = onClick)
            .semantics { if (unread) stateDescription = unreadLabel }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(46.dp)) {
            UserAvatar(notification.actorName, notification.actorId, 46.dp, fontSize = 14.sp)
            TypeBadge(notification.type, unread, Modifier.align(Alignment.BottomEnd).offset(x = 3.dp, y = 3.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                notificationText(notification),
                style = jakarta(13.sp),
                color = if (unread) c.text else c.text2,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(timeLabel(notification.createdAt), style = jakarta(12.sp), color = c.text3, maxLines = 1)
        }
        if (unread) {
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .size(9.dp)
                    .background(c.orange, CircleShape),
            )
        }
    }
}

@Composable
private fun TypeBadge(type: NotificationType, unread: Boolean, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    val (container, content) = when {
        !unread -> c.s4 to c.text2
        type == NotificationType.LIKE -> c.orange to c.onOrange
        type == NotificationType.FOLLOW -> c.lavender to c.bg
        else -> c.primary to c.onPrimary
    }
    val icon = when (type) {
        NotificationType.LIKE -> R.drawable.ic_heart_filled
        NotificationType.FOLLOW -> R.drawable.ic_user_plus
        NotificationType.COMMENT, NotificationType.REPLY -> R.drawable.ic_comment
    }
    val ring = c.bg
    Box(
        modifier = modifier
            .size(22.dp)
            .drawBehind { drawCircle(ring, radius = size.minDimension / 2 + 2.dp.toPx()) }
            .background(container, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(12.dp))
    }
}

@Composable
private fun EmptyNotifications() {
    val c = WakTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp)
            .padding(bottom = 90.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(c.s3, CircleShape)
                .border(1.dp, c.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_bell), contentDescription = null, tint = c.text3, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.notifications_empty_title), style = spaceGrotesk(20.sp), color = c.text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.notifications_empty_message), style = jakarta(14.sp), color = c.text2, textAlign = TextAlign.Center)
    }
}

private fun previewItems(now: Long): Pair<List<AppNotification>, List<AppNotification>> {
    val minute = MINUTE_MS
    val newItems = listOf(
        AppNotification("1", NotificationType.LIKE, "aria", "Aria Chen", TargetType.PROGRESSION, "p1", "Rainy window loop", createdAt = now - 2 * minute),
        AppNotification("2", NotificationType.COMMENT, "marcus", "Marcus L.", TargetType.PROGRESSION, "p1", "Rainy window loop", "That G13 voicing is perfect", now - 18 * minute),
        AppNotification("3", NotificationType.FOLLOW, "julian", "Julian Keys", TargetType.PROFILE, "julian", createdAt = now - 60 * minute),
    )
    val earlier = listOf(
        AppNotification("4", NotificationType.REPLY, "aria", "Aria Chen", TargetType.SONG, "s1", "Subtle Resonance", "Capo 1 works great", now - 3 * 24 * 60 * minute, true),
        AppNotification("5", NotificationType.FOLLOW, "sora", "Sora Lane", TargetType.PROFILE, "sora", createdAt = now - 2 * 24 * 60 * minute, isRead = true),
    )
    return newItems to earlier
}

@Preview(name = "Notifications", heightDp = 844)
@Composable
private fun NotificationsPreview() {
    val (newItems, earlier) = previewItems(System.currentTimeMillis())
    WAKordTheme(darkTheme = true) {
        NotificationsScreen(
            state = NotificationsUiState(isLoading = false, newItems = newItems, earlierItems = earlier, unreadCount = newItems.size),
            onAction = {},
            onBack = {},
        )
    }
}

@Preview(name = "Notifications light", heightDp = 844)
@Composable
private fun NotificationsLightPreview() {
    val (newItems, earlier) = previewItems(System.currentTimeMillis())
    WAKordTheme(darkTheme = false) {
        NotificationsScreen(
            state = NotificationsUiState(isLoading = false, newItems = newItems, earlierItems = earlier, unreadCount = newItems.size),
            onAction = {},
            onBack = {},
        )
    }
}

@Preview(name = "Notifications empty", heightDp = 844)
@Composable
private fun NotificationsEmptyPreview() {
    WAKordTheme(darkTheme = true) {
        NotificationsScreen(state = NotificationsUiState(isLoading = false), onAction = {}, onBack = {})
    }
}
