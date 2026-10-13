package edu.bluejack26_1.wakord.ui.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.domain.model.ChordProgression
import edu.bluejack26_1.wakord.domain.model.Difficulty
import edu.bluejack26_1.wakord.domain.model.ProfileStats
import edu.bluejack26_1.wakord.domain.model.User
import edu.bluejack26_1.wakord.ui.components.BackgroundArt
import edu.bluejack26_1.wakord.ui.components.ErrorState
import edu.bluejack26_1.wakord.ui.components.GlassIconButton
import edu.bluejack26_1.wakord.ui.components.ProfileAvatar
import edu.bluejack26_1.wakord.ui.components.compactCount
import edu.bluejack26_1.wakord.ui.components.levelColor
import edu.bluejack26_1.wakord.ui.components.levelContainer
import edu.bluejack26_1.wakord.ui.components.levelLabel
import edu.bluejack26_1.wakord.ui.components.prettyChord
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.jakarta
import edu.bluejack26_1.wakord.ui.theme.spaceGrotesk

@Composable
fun OtherProfileScreen(
    state: OtherProfileUiState,
    onAction: (OtherProfileAction) -> Unit,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenProgression: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = WakTheme.colors
    val user = state.user
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        BackgroundArt(R.drawable.bg_tuner_alert, height = 460.dp)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item(key = "header") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    GlassIconButton(painterResource(R.drawable.ic_back), stringResource(R.string.back), onBack)
                }
            }
            when {
                state.loadError != null && user == null -> item(key = "error") {
                    ErrorState(stringResource(state.loadError), { onAction(OtherProfileAction.Retry) }, Modifier.padding(top = 80.dp))
                }
                state.isLoading -> item(key = "loading") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = c.primary)
                    }
                }
                user == null -> item(key = "missing") {
                    Text(
                        stringResource(R.string.other_not_found),
                        style = jakarta(14.sp),
                        color = c.text2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp, vertical = 120.dp),
                    )
                }
                else -> {
                    item(key = "identity") {
                        Identity(user, state.stats, state.progressions.size)
                    }
                    item(key = "actions") {
                        Actions(state, onAction, onShare, onEditProfile)
                    }
                    item(key = "section") {
                        Row(
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(stringResource(R.string.profile_progressions), style = spaceGrotesk(20.sp), color = c.text, modifier = Modifier.weight(1f))
                            Text(stringResource(R.string.other_most_liked), style = jakarta(12.sp), color = c.text3)
                        }
                    }
                    if (state.progressions.isEmpty()) {
                        item(key = "empty") {
                            Text(
                                stringResource(R.string.other_empty),
                                style = jakarta(14.sp),
                                color = c.text2,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 40.dp, vertical = 32.dp),
                            )
                        }
                    }
                    items(state.progressions, key = { it.id }) { progression ->
                        ProgressionCard(
                            progression,
                            onClick = { onOpenProgression(progression.id) },
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Identity(user: User, stats: ProfileStats, progressions: Int) {
    val c = WakTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProfileAvatar(user.fullName, user.uid, user.photoUrl, 96.dp, fontSize = 32.sp)
        Spacer(Modifier.height(12.dp))
        Text(user.fullName, style = spaceGrotesk(26.sp), color = c.text, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        val handle = listOfNotNull(user.username.takeIf { it.isNotBlank() }?.let { "@$it" }, user.bio.takeIf { it.isNotBlank() })
        if (handle.isNotEmpty()) {
            Spacer(Modifier.height(3.dp))
            Text(handle.joinToString(" · "), style = jakarta(12.sp), color = c.text3, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            StatText(compactCount(stats.followers), stringResource(R.string.other_stat_followers))
            StatText(compactCount(stats.following), stringResource(R.string.other_stat_following))
            StatText(compactCount(progressions), stringResource(R.string.other_stat_progressions))
        }
    }
}

@Composable
private fun StatText(value: String, label: String) {
    val c = WakTheme.colors
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontFamily = spaceGrotesk(13.sp).fontFamily, fontWeight = spaceGrotesk(13.sp).fontWeight, color = c.text)) { append(value) }
            withStyle(SpanStyle(color = c.text2)) { append(" $label") }
        },
        style = jakarta(13.sp),
        maxLines = 1,
        modifier = Modifier.semantics(mergeDescendants = true) {},
    )
}

@Composable
private fun Actions(
    state: OtherProfileUiState,
    onAction: (OtherProfileAction) -> Unit,
    onShare: () -> Unit,
    onEditProfile: () -> Unit,
) {
    val c = WakTheme.colors
    Row(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val (label, icon, primary) = when {
            state.isMe -> Triple(stringResource(R.string.profile_edit), R.drawable.ic_edit, false)
            state.isFollowing -> Triple(stringResource(R.string.other_following), R.drawable.ic_check, false)
            else -> Triple(stringResource(R.string.other_follow), R.drawable.ic_user_plus, true)
        }
        val content = if (primary) c.onPrimary else c.text
        Surface(
            onClick = { if (state.isMe) onEditProfile() else onAction(OtherProfileAction.ToggleFollow) },
            modifier = Modifier
                .weight(1f)
                .height(54.dp),
            shape = RoundedCornerShape(18.dp),
            color = if (primary) c.primary else c.s3,
            border = if (primary) null else BorderStroke(1.dp, c.outline),
        ) {
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(label, style = jakarta(16.sp), color = content, maxLines = 1)
            }
        }
        Surface(
            onClick = onShare,
            modifier = Modifier.size(54.dp),
            shape = RoundedCornerShape(18.dp),
            color = c.s3,
            border = BorderStroke(1.dp, c.line),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_share), contentDescription = stringResource(R.string.other_share_cd), tint = c.text, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProgressionCard(progression: ChordProgression, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = c.s2,
        border = BorderStroke(1.dp, c.line),
    ) {
        Column(Modifier.padding(horizontal = 17.dp, vertical = 15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(progression.title, style = spaceGrotesk(16.sp), color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Icon(painterResource(R.drawable.ic_heart), contentDescription = null, tint = c.text3, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(compactCount(progression.likeCount), style = jakarta(12.sp), color = c.text3)
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), maxLines = 1) {
                progression.chords.forEach { chord ->
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .background(c.s3, RoundedCornerShape(12.dp))
                            .border(1.dp, c.line, RoundedCornerShape(12.dp))
                            .padding(horizontal = 11.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(prettyChord(chord), style = spaceGrotesk(14.sp), color = c.text, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (progression.genre.isNotBlank()) ProfileTag(progression.genre)
                ProfileTag(levelLabel(progression.difficulty), levelColor(progression.difficulty, c), levelContainer(progression.difficulty, c))
            }
        }
    }
}

private val PreviewOther = OtherProfileUiState(
    isLoading = false,
    user = User(uid = "julian", fullName = "Julian Keys", email = "", username = "juliankeys", bio = "Session keys & guitar"),
    stats = ProfileStats(8400, 212),
    progressions = listOf(
        ChordProgression("p1", "Neo-Soul Progression #04", "C", 84, "Neo-Soul", Difficulty.ADVANCED, listOf("Dm9", "G13", "Cmaj9", "Am7"), "julian", "Julian Keys", likeCount = 342),
        ChordProgression("p2", "Late night tension", "F", 70, "Jazz", Difficulty.INTERMEDIATE, listOf("Fmaj9", "Em7", "Ebdim7", "Dm7"), "julian", "Julian Keys", likeCount = 298),
    ),
)

@Preview(name = "Other profile", heightDp = 900)
@Composable
private fun OtherProfilePreview() {
    WAKordTheme(darkTheme = true) {
        OtherProfileScreen(PreviewOther, onAction = {}, onBack = {}, onShare = {}, onEditProfile = {}, onOpenProgression = {})
    }
}

@Preview(name = "Other profile following light", heightDp = 900)
@Composable
private fun OtherProfileFollowingPreview() {
    WAKordTheme(darkTheme = false) {
        OtherProfileScreen(PreviewOther.copy(isFollowing = true), onAction = {}, onBack = {}, onShare = {}, onEditProfile = {}, onOpenProgression = {})
    }
}
