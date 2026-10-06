package edu.bluejack26_1.wakord.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.ui.navigation.Routes
import edu.bluejack26_1.wakord.ui.theme.WakTheme

data class TopLevelDestination(val route: String, val icon: ImageVector, val labelRes: Int)

val TopLevelDestinations = listOf(
    TopLevelDestination(Routes.HOME, Icons.Rounded.Home, R.string.nav_home),
    TopLevelDestination(Routes.EXPLORE, Icons.Rounded.Explore, R.string.nav_explore),
    TopLevelDestination(Routes.TUNER, Icons.Rounded.GraphicEq, R.string.nav_tuner),
    TopLevelDestination(Routes.ANALYZE, Icons.Rounded.Mic, R.string.nav_analyze),
    TopLevelDestination(Routes.PROFILE, Icons.Rounded.Person, R.string.nav_profile),
)

@Composable
fun WakBottomBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    val c = WakTheme.colors
    NavigationBar(containerColor = c.s1) {
        TopLevelDestinations.forEach { dest ->
            NavigationBarItem(
                selected = currentRoute == dest.route,
                onClick = { onNavigate(dest.route) },
                icon = { Icon(dest.icon, contentDescription = null) },
                label = { Text(stringResource(dest.labelRes), style = MaterialTheme.typography.labelMedium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = c.onPrimary,
                    indicatorColor = c.primary,
                    selectedTextColor = c.primaryText,
                    unselectedIconColor = c.text3,
                    unselectedTextColor = c.text3,
                ),
            )
        }
    }
}

@Composable
fun NotificationBell(unreadCount: Int, onClick: () -> Unit) {
    val c = WakTheme.colors
    IconButton(onClick = onClick) {
        BadgedBox(
            badge = {
                if (unreadCount > 0) {
                    Badge(containerColor = c.peach, contentColor = c.onPrimary) {
                        Text(if (unreadCount > 9) "9+" else "$unreadCount")
                    }
                }
            },
        ) {
            Icon(Icons.Rounded.Notifications, contentDescription = stringResource(R.string.nav_notifications), tint = c.text)
        }
    }
}

@Composable
fun MiniPlayer(
    title: String,
    subtitle: String,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = WakTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .background(c.s3, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(c.primary, RoundedCornerShape(999.dp))
                .clickable(onClick = onTogglePlay),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                tint = c.onPrimary,
            )
        }
    }
}
