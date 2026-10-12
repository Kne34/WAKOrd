package edu.bluejack26_1.wakord.ui.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.domain.model.ChordProgression
import edu.bluejack26_1.wakord.domain.model.ChordVoicing
import edu.bluejack26_1.wakord.domain.model.Difficulty
import edu.bluejack26_1.wakord.domain.model.Instrument
import edu.bluejack26_1.wakord.domain.model.ProfileStats
import edu.bluejack26_1.wakord.domain.model.SavedEntry
import edu.bluejack26_1.wakord.domain.model.SavedType
import edu.bluejack26_1.wakord.domain.model.User
import edu.bluejack26_1.wakord.ui.components.BackgroundArt
import edu.bluejack26_1.wakord.ui.components.ChordCard
import edu.bluejack26_1.wakord.ui.components.DialogTone
import edu.bluejack26_1.wakord.ui.components.GlassIconButton
import edu.bluejack26_1.wakord.ui.components.NotificationBell
import edu.bluejack26_1.wakord.ui.components.ProfileAvatar
import edu.bluejack26_1.wakord.ui.components.WakDialog
import edu.bluejack26_1.wakord.ui.components.compactCount
import edu.bluejack26_1.wakord.ui.components.glassBorder
import edu.bluejack26_1.wakord.ui.components.glassColor
import edu.bluejack26_1.wakord.ui.components.prettyChord
import edu.bluejack26_1.wakord.ui.components.relativeTime
import edu.bluejack26_1.wakord.ui.feature.auth.nameRes
import edu.bluejack26_1.wakord.ui.feature.explore.ProgressionTile
import edu.bluejack26_1.wakord.ui.feature.home.SavedChord
import edu.bluejack26_1.wakord.ui.feature.song.songCover
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.jakarta
import edu.bluejack26_1.wakord.ui.theme.spaceGrotesk

private const val CHORD_COLUMNS = 3
private const val MINUTE_MS = 60_000L

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onAction: (ProfileAction) -> Unit,
    unreadNotifications: Int,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onEditProfile: () -> Unit,
    onNewProgression: () -> Unit,
    onOpenSong: (String) -> Unit,
    onOpenChord: (String) -> Unit,
    onOpenProgression: (String) -> Unit,
    onExplore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = WakTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        BackgroundArt(R.drawable.bg_splash, height = 440.dp)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item(key = "header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.profile_title), style = spaceGrotesk(26.sp), color = c.text, modifier = Modifier.weight(1f))
                    NotificationBell(unreadCount = unreadNotifications, onClick = onOpenNotifications)
                    Spacer(Modifier.width(8.dp))
                    GlassIconButton(painterResource(R.drawable.ic_settings), stringResource(R.string.settings_title), onOpenSettings)
                }
            }
            item(key = "identity") {
                Identity(state.user, Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp))
            }
            item(key = "stats") {
                StatsCard(state.stats, state.myProgressions.size, Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp))
            }
            item(key = "actions") {
                Row(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ActionButton(stringResource(R.string.profile_edit), R.drawable.ic_edit, primary = false, onClick = onEditProfile, modifier = Modifier.weight(152f))
                    ActionButton(stringResource(R.string.profile_new_progression), R.drawable.ic_plus, primary = true, onClick = onNewProgression, modifier = Modifier.weight(188f))
                }
            }
            item(key = "tabs") {
                Tabs(state.tab, onSelect = { onAction(ProfileAction.SelectTab(it)) }, modifier = Modifier.padding(top = 22.dp))
            }
            when {
                state.isLoading -> item(key = "loading") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = c.primary)
                    }
                }
                state.tab == ProfileTab.SONGS -> savedSongs(state.savedSongs, onOpenSong, onAction, onExplore)
                state.tab == ProfileTab.CHORDS -> savedChords(state.savedChords, onOpenChord, onExplore)
                else -> myProgressions(state.myProgressions.sortedByDescending { it.createdAt }, onOpenProgression, onNewProgression)
            }
        }
    }

    val pending = state.pendingRemoval
    if (pending != null) {
        WakDialog(
            title = stringResource(R.string.profile_remove_title),
            message = stringResource(R.string.profile_remove_message, pending.title),
            primaryText = stringResource(R.string.profile_remove),
            onPrimary = { onAction(ProfileAction.ConfirmRemove) },
            onDismissRequest = { onAction(ProfileAction.CancelRemove) },
            secondaryText = stringResource(R.string.profile_keep),
            icon = painterResource(R.drawable.ic_trash),
            tone = DialogTone.Danger,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Identity(user: User?, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    val name = user?.fullName.orEmpty()
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ProfileAvatar(name, user?.uid.orEmpty(), user?.photoUrl, 84.dp, fontSize = 28.sp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = spaceGrotesk(22.sp), color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val handle = listOfNotNull(user?.username?.takeIf { it.isNotBlank() }?.let { "@$it" }, user?.bio?.takeIf { it.isNotBlank() })
            if (handle.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(handle.joinToString(" · "), style = jakarta(12.sp), color = c.text3, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            val tags = user?.instruments.orEmpty().map { stringResource(it.nameRes()) } + user?.genres.orEmpty()
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    maxLines = 2,
                ) {
                    tags.forEach { ProfileTag(it) }
                }
            }
        }
    }
}

@Composable
internal fun ProfileTag(text: String, color: Color = WakTheme.colors.text2, container: Color = WakTheme.colors.s4) {
    Text(
        text,
        style = spaceGrotesk(11.sp, FontWeight.SemiBold),
        color = color,
        maxLines = 1,
        modifier = Modifier
            .background(container, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
    )
}

@Composable
private fun StatsCard(stats: ProfileStats, progressions: Int, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    val divider = c.text2.copy(alpha = 0.10f)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = glassColor(c),
        border = glassBorder(c),
    ) {
        Row(Modifier.padding(vertical = 13.dp)) {
            Stat(compactCount(stats.followers), stringResource(R.string.profile_followers), null)
            Stat(compactCount(stats.following), stringResource(R.string.profile_following), divider)
            Stat(compactCount(progressions), stringResource(R.string.profile_progressions), divider)
        }
    }
}

@Composable
private fun RowScope.Stat(value: String, label: String, divider: Color?) {
    val c = WakTheme.colors
    Column(
        modifier = Modifier
            .weight(1f)
            .drawBehind {
                if (divider != null) drawLine(divider, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 1.dp.toPx())
            }
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = spaceGrotesk(18.sp), color = c.text, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(label, style = jakarta(12.sp), color = c.text3, maxLines = 1)
    }
}

@Composable
private fun ActionButton(text: String, icon: Int, primary: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    val content = if (primary) c.onPrimary else c.text
    Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (primary) c.primary else c.s3,
        border = if (primary) null else BorderStroke(1.dp, c.outline),
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, style = spaceGrotesk(14.sp), color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun Tabs(selected: ProfileTab, onSelect: (ProfileTab) -> Unit, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    val line = c.text2.copy(alpha = 0.10f)
    val description = stringResource(R.string.profile_tabs_cd)
    val labels = listOf(
        ProfileTab.SONGS to stringResource(R.string.profile_tab_songs),
        ProfileTab.CHORDS to stringResource(R.string.profile_tab_chords),
        ProfileTab.PROGRESSIONS to stringResource(R.string.profile_tab_progressions),
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val y = size.height - 0.5.dp.toPx()
                drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }
            .semantics { contentDescription = description }
            .selectableGroup()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        labels.forEach { (tab, label) ->
            val active = tab == selected
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .selectable(selected = active, role = Role.Tab, onClick = { onSelect(tab) })
                    .padding(top = 12.dp),
            ) {
                Text(label, style = jakarta(14.sp), color = if (active) c.text else c.text3, maxLines = 1)
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(if (active) c.primary else Color.Transparent),
                )
            }
        }
    }
}

private fun LazyListScope.savedSongs(
    songs: List<SavedEntry>,
    onOpenSong: (String) -> Unit,
    onAction: (ProfileAction) -> Unit,
    onExplore: () -> Unit,
) {
    if (songs.isEmpty()) {
        item(key = "songs-empty") {
            EmptyTab(
                title = stringResource(R.string.profile_empty_title),
                message = stringResource(R.string.profile_empty_message),
                action = stringResource(R.string.profile_explore_songs),
                onAction = onExplore,
            )
        }
        return
    }
    item(key = "songs-top") { Spacer(Modifier.height(10.dp)) }
    items(songs, key = { "song-" + it.id }) { entry ->
        SavedSongRow(entry, onClick = { onOpenSong(entry.id) }, onRemove = { onAction(ProfileAction.AskRemove(entry)) })
    }
}

@Composable
private fun savedLabel(savedAt: Long): String {
    val now = System.currentTimeMillis()
    return if (now - savedAt < MINUTE_MS) {
        stringResource(R.string.profile_saved_now)
    } else {
        stringResource(R.string.profile_saved_at, stringResource(R.string.time_ago, relativeTime(savedAt, now)))
    }
}

@Composable
private fun SavedSongRow(entry: SavedEntry, onClick: () -> Unit, onRemove: () -> Unit) {
    val c = WakTheme.colors
    val saved = savedLabel(entry.savedAt)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painterResource(songCover(entry.id)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp)),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.title, style = spaceGrotesk(16.sp), color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(
                listOf(entry.subtitle, saved).filter { it.isNotBlank() }.joinToString(" · "),
                style = jakarta(12.sp),
                color = c.text3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                painterResource(R.drawable.ic_bookmark_filled),
                contentDescription = stringResource(R.string.profile_remove_cd, entry.title),
                tint = c.primaryText,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private fun LazyListScope.savedChords(
    chords: List<SavedChord>,
    onOpenChord: (String) -> Unit,
    onExplore: () -> Unit,
) {
    val shown = chords.filter { it.voicing != null }
    if (shown.isEmpty()) {
        item(key = "chords-empty") {
            EmptyTab(
                title = stringResource(R.string.profile_empty_title),
                message = stringResource(R.string.profile_empty_message),
                action = stringResource(R.string.profile_explore_songs),
                onAction = onExplore,
            )
        }
        return
    }
    items(shown.chunked(CHORD_COLUMNS), key = { row -> "chords-" + row.first().symbol }) { row ->
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            row.forEach { chord ->
                val voicing = chord.voicing ?: return@forEach
                ChordCard(
                    name = prettyChord(chord.symbol),
                    frets = voicing.frets,
                    fingers = voicing.fingers,
                    baseFret = voicing.baseFret,
                    onClick = { onOpenChord(chord.symbol) },
                    modifier = Modifier.weight(1f),
                )
            }
            repeat(CHORD_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

private fun LazyListScope.myProgressions(
    progressions: List<ChordProgression>,
    onOpenProgression: (String) -> Unit,
    onNewProgression: () -> Unit,
) {
    if (progressions.isEmpty()) {
        item(key = "progressions-empty") {
            EmptyTab(
                title = stringResource(R.string.profile_empty_progressions_title),
                message = stringResource(R.string.profile_empty_progressions),
                action = stringResource(R.string.profile_new_progression),
                onAction = onNewProgression,
            )
        }
        return
    }
    items(progressions, key = { "progression-" + it.id }) { progression ->
        ProgressionTile(
            progression = progression,
            onClick = { onOpenProgression(progression.id) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp),
        )
    }
}

@Composable
private fun EmptyTab(title: String, message: String, action: String, onAction: () -> Unit) {
    val c = WakTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 40.dp, end = 40.dp, top = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(c.s3, CircleShape)
                .border(1.dp, c.text2.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_bookmark), contentDescription = null, tint = c.text3, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(title, style = spaceGrotesk(20.sp), color = c.text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(message, style = jakarta(14.sp), color = c.text2, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Surface(
            onClick = onAction,
            modifier = Modifier.height(40.dp),
            shape = RoundedCornerShape(14.dp),
            color = c.primary,
        ) {
            Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                Text(action, style = spaceGrotesk(14.sp), color = c.onPrimary, maxLines = 1)
            }
        }
    }
}

private val PreviewUser = User(
    uid = "keane",
    fullName = "Keane Wilson",
    email = "keane@mail.com",
    username = "keane.wilson",
    bio = "Indie producer",
    instruments = listOf(Instrument.GUITAR, Instrument.UKULELE),
    genres = listOf("Neo-Soul"),
)

private val PreviewSongs = listOf(
    SavedEntry(SavedType.SONG, "subtle-resonance", "Subtle Resonance", "Alex Vance", System.currentTimeMillis() - 2 * 86_400_000L),
    SavedEntry(SavedType.SONG, "midnight-waves", "Midnight Waves", "Neon Mirage", System.currentTimeMillis() - 5 * 86_400_000L),
    SavedEntry(SavedType.SONG, "golden-hour-glow", "Golden Hour Glow", "Koro Acoustic Trio", System.currentTimeMillis() - 7 * 86_400_000L),
)

@Preview(name = "Profile", heightDp = 900)
@Composable
private fun ProfilePreview() {
    WAKordTheme(darkTheme = true) {
        ProfileScreen(
            state = ProfileUiState(isLoading = false, user = PreviewUser, stats = ProfileStats(1200, 318), savedSongs = PreviewSongs),
            onAction = {}, unreadNotifications = 3, onOpenNotifications = {}, onOpenSettings = {}, onEditProfile = {},
            onNewProgression = {}, onOpenSong = {}, onOpenChord = {}, onOpenProgression = {}, onExplore = {},
        )
    }
}

@Preview(name = "Profile chords", heightDp = 900)
@Composable
private fun ProfileChordsPreview() {
    val voicing = ChordVoicing(listOf(-1, 3, 2, 0, 0, 0), listOf(0, 3, 2, 0, 0, 0), 1, 0)
    WAKordTheme(darkTheme = true) {
        ProfileScreen(
            state = ProfileUiState(
                isLoading = false,
                user = PreviewUser,
                tab = ProfileTab.CHORDS,
                savedChords = listOf(SavedChord("Cmaj7", voicing), SavedChord("Am9", voicing), SavedChord("G13", voicing), SavedChord("Dm9", voicing)),
            ),
            onAction = {}, unreadNotifications = 0, onOpenNotifications = {}, onOpenSettings = {}, onEditProfile = {},
            onNewProgression = {}, onOpenSong = {}, onOpenChord = {}, onOpenProgression = {}, onExplore = {},
        )
    }
}

@Preview(name = "Profile progressions light", heightDp = 900)
@Composable
private fun ProfileProgressionsPreview() {
    val progression = ChordProgression("p1", "Rainy window loop", "C", 78, "Lo-fi", Difficulty.EASY, listOf("Cmaj7", "Am7", "Fmaj7", "G"), "keane", "Keane Wilson")
    WAKordTheme(darkTheme = false) {
        ProfileScreen(
            state = ProfileUiState(isLoading = false, user = PreviewUser, tab = ProfileTab.PROGRESSIONS, myProgressions = listOf(progression)),
            onAction = {}, unreadNotifications = 0, onOpenNotifications = {}, onOpenSettings = {}, onEditProfile = {},
            onNewProgression = {}, onOpenSong = {}, onOpenChord = {}, onOpenProgression = {}, onExplore = {},
        )
    }
}

@Preview(name = "Profile empty", heightDp = 900)
@Composable
private fun ProfileEmptyPreview() {
    WAKordTheme(darkTheme = true) {
        ProfileScreen(
            state = ProfileUiState(isLoading = false, user = PreviewUser),
            onAction = {}, unreadNotifications = 0, onOpenNotifications = {}, onOpenSettings = {}, onEditProfile = {},
            onNewProgression = {}, onOpenSong = {}, onOpenChord = {}, onOpenProgression = {}, onExplore = {},
        )
    }
}
