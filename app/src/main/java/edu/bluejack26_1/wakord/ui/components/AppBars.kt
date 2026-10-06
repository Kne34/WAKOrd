package edu.bluejack26_1.wakord.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.ui.navigation.Routes
import edu.bluejack26_1.wakord.ui.theme.SpaceGrotesk
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme

data class TopLevelDestination(
    val route: String,
    @param:DrawableRes val icon: Int,
    @param:StringRes val labelRes: Int,
)

val TopLevelDestinations = listOf(
    TopLevelDestination(Routes.HOME, R.drawable.ic_nav_home, R.string.nav_home),
    TopLevelDestination(Routes.EXPLORE, R.drawable.ic_nav_explore, R.string.nav_explore),
    TopLevelDestination(Routes.TUNER, R.drawable.ic_nav_tuner, R.string.nav_tuner),
    TopLevelDestination(Routes.ANALYZE, R.drawable.ic_nav_analyze, R.string.nav_analyze),
    TopLevelDestination(Routes.PROFILE, R.drawable.ic_nav_profile, R.string.nav_profile),
)

@Composable
fun WakBottomBar(currentRoute: String?, onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(if (c.isDark) c.bg.copy(alpha = 0.88f) else c.s1.copy(alpha = 0.92f)),
    ) {
        HorizontalDivider(
            thickness = 1.dp,
            color = if (c.isDark) c.text2.copy(alpha = 0.10f) else c.text.copy(alpha = 0.08f),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp)
                .padding(horizontal = 8.dp),
        ) {
            TopLevelDestinations.forEach { dest ->
                NavItem(
                    destination = dest,
                    selected = currentRoute == dest.route,
                    onClick = { onNavigate(dest.route) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    destination: TopLevelDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = WakTheme.colors
    val tint = if (selected) c.primaryText else c.text3
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(width = 22.dp, height = 3.dp)
                    .background(c.primary, RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp)),
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 15.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(painterResource(destination.icon), contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(5.dp))
            Text(
                text = stringResource(destination.labelRes),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = MaterialTheme.typography.bodySmall.fontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 0.sp,
                ),
                color = tint,
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
            Icon(
                painter = painterResource(R.drawable.ic_bell),
                contentDescription = stringResource(R.string.nav_notifications),
                tint = c.text,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
fun MiniPlayer(
    title: String,
    currentChord: String,
    detail: String,
    isPlaying: Boolean,
    isLiked: Boolean,
    progress: Float,
    onTogglePlay: () -> Unit,
    onToggleLike: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cover: Painter? = null,
) {
    val c = WakTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .shadow(
                elevation = 18.dp,
                shape = shape,
                ambientColor = Color.Transparent,
                spotColor = if (c.isDark) Color.Black.copy(alpha = 0.45f) else c.text.copy(alpha = 0.14f),
            )
            .height(64.dp),
        shape = shape,
        color = if (c.isDark) c.s2.copy(alpha = 0.92f) else c.s1.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, if (c.isDark) c.text.copy(alpha = 0.09f) else c.text.copy(alpha = 0.08f)),
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 9.dp, end = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.s4),
                ) {
                    if (cover != null) {
                        Image(cover, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp),
                        color = c.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentChord,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold),
                            color = c.primaryText,
                        )
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = c.text3,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                IconButton(onClick = onToggleLike) {
                    Icon(
                        painter = painterResource(R.drawable.ic_heart),
                        contentDescription = stringResource(R.string.like),
                        tint = if (isLiked) c.peach else c.text,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        painter = painterResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                        contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                        tint = c.text,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 19.dp, end = 19.dp, bottom = 1.dp)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(if (c.isDark) c.text2.copy(alpha = 0.10f) else c.text.copy(alpha = 0.08f), RoundedCornerShape(2.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(c.primary, RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}

@Preview(name = "Navbar + mini player - dark")
@Composable
private fun AppBarsDarkPreview() {
    WAKordTheme(darkTheme = true) {
        Column(Modifier.background(WakTheme.colors.bg)) {
            MiniPlayer(
                title = "Neo-Soul Progression #04",
                currentChord = "Dm9",
                detail = "→ G13 → Cmaj9 · Julian Keys",
                isPlaying = true,
                isLiked = false,
                progress = 0.38f,
                onTogglePlay = {},
                onToggleLike = {},
                onClick = {},
                modifier = Modifier.padding(vertical = 8.dp),
            )
            WakBottomBar(currentRoute = Routes.HOME, onNavigate = {})
        }
    }
}

@Preview(name = "Navbar + mini player - light")
@Composable
private fun AppBarsLightPreview() {
    WAKordTheme(darkTheme = false) {
        Column(Modifier.background(WakTheme.colors.bg)) {
            MiniPlayer(
                title = "Neo-Soul Progression #04",
                currentChord = "Dm9",
                detail = "→ G13 → Cmaj9 · Julian Keys",
                isPlaying = false,
                isLiked = true,
                progress = 0.38f,
                onTogglePlay = {},
                onToggleLike = {},
                onClick = {},
                modifier = Modifier.padding(vertical = 8.dp),
            )
            WakBottomBar(currentRoute = Routes.EXPLORE, onNavigate = {})
        }
    }
}
