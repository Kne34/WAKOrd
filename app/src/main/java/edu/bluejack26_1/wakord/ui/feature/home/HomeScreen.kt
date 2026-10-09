package edu.bluejack26_1.wakord.ui.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import edu.bluejack26_1.wakord.domain.model.ChordSegment
import edu.bluejack26_1.wakord.domain.model.ChordVoicing
import edu.bluejack26_1.wakord.domain.model.Difficulty
import edu.bluejack26_1.wakord.domain.model.FeedItem
import edu.bluejack26_1.wakord.domain.model.Song
import edu.bluejack26_1.wakord.domain.model.SongLine
import edu.bluejack26_1.wakord.domain.model.SongSection
import edu.bluejack26_1.wakord.ui.components.ChordCard
import edu.bluejack26_1.wakord.ui.components.GlassIconButton
import edu.bluejack26_1.wakord.ui.components.InitialsAvatar
import edu.bluejack26_1.wakord.ui.components.NotificationBell
import edu.bluejack26_1.wakord.ui.components.alertContainer
import edu.bluejack26_1.wakord.ui.components.compactCount
import edu.bluejack26_1.wakord.ui.components.levelColor
import edu.bluejack26_1.wakord.ui.components.levelContainer
import edu.bluejack26_1.wakord.ui.components.levelLabel
import edu.bluejack26_1.wakord.ui.components.prettyChord
import edu.bluejack26_1.wakord.ui.components.relativeTime
import edu.bluejack26_1.wakord.ui.feature.song.songCover
import edu.bluejack26_1.wakord.ui.feature.song.songWideCover
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.jakarta
import edu.bluejack26_1.wakord.ui.theme.spaceGrotesk
import java.util.Calendar

private data class WideInk(val text: Color, val tagText: Color)

private val WideInks = mapOf(
    R.drawable.wide_golden_hour_glow to WideInk(Color(0xFF2B0C00), Color(0xFFFFD9C9)),
    R.drawable.wide_midnight_waves to WideInk(Color(0xFF15133D), Color(0xFFE5E3FD)),
    R.drawable.wide_subtle_resonance to WideInk(Color(0xFF00363B), Color(0xFFC9F7FB)),
)

private fun Song.distinctChords(): List<String> =
    sections.flatMap { section -> section.lines.flatMap { line -> line.segments.mapNotNull { it.chord } } }.distinct()

@Composable
private fun greeting(hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): String = stringResource(
    when (hour) {
        in 5..11 -> R.string.greeting_morning
        in 12..17 -> R.string.greeting_afternoon
        else -> R.string.greeting_evening
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    onOpenSong: (String) -> Unit,
    onOpenProgression: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenTraining: () -> Unit,
    modifier: Modifier = Modifier,
    unreadNotifications: Int = 0,
) {
    val c = WakTheme.colors
    val listState = rememberLazyListState()
    val currentState by rememberUpdatedState(state)
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 3
        }.collect { nearEnd ->
            if (nearEnd && currentState.feed.isNotEmpty()) onAction(HomeAction.LoadMore)
        }
    }
    PullToRefreshBox(
        isRefreshing = state.isLoading && state.feed.isNotEmpty(),
        onRefresh = { onAction(HomeAction.Refresh) },
        modifier = modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item(key = "header") {
                HomeHeader(state.userName, unreadNotifications, onOpenProfile, onOpenSearch, onOpenNotifications)
            }
            item(key = "workout") {
                WorkoutCard(state.workout, onOpenTraining)
            }
            if (state.recommended.isNotEmpty()) {
                item(key = "recommended") {
                    RecommendedSection(state.recommended, onOpenSong, onSeeAll = onOpenSearch)
                }
            }
            item(key = "saved") {
                SavedChordsSection(state.savedChords, onSeeAll = onOpenProfile)
            }
            item(key = "feed-header") {
                SectionHeader(
                    title = stringResource(R.string.feed_title),
                    trailing = { Text(stringResource(R.string.feed_subtitle), style = jakarta(12.sp), color = c.text3) },
                    modifier = Modifier.padding(top = 30.dp, bottom = 8.dp),
                )
            }
            items(state.feed, key = { "${it::class.simpleName}-${it.id}" }) { item ->
                when (item) {
                    is FeedItem.SongItem -> SongRow(item.song, onClick = { onOpenSong(item.song.id) })
                    is FeedItem.ProgressionItem -> ProgressionCard(
                        item = item,
                        onClick = { onOpenProgression(item.progression.id) },
                        isLiked = item.id in state.likedIds,
                        onPlay = { onAction(HomeAction.PlayProgression(item.progression)) },
                        onToggleLike = { onAction(HomeAction.ToggleProgressionLike(item.progression)) },
                    )
                }
            }
            item(key = "feed-footer") {
                FeedFooter(state)
            }
        }
    }
}

@Composable
private fun HomeHeader(
    userName: String,
    unreadNotifications: Int,
    onOpenProfile: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenNotifications: () -> Unit,
) {
    val c = WakTheme.colors
    val profileCd = stringResource(R.string.home_profile_cd)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InitialsAvatar(
            name = userName,
            seed = userName,
            size = 40.dp,
            fontSize = 14.sp,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onOpenProfile)
                .semantics { contentDescription = profileCd },
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(greeting(), style = jakarta(12.sp), color = c.text3)
            Text(userName, style = spaceGrotesk(18.sp), color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        GlassIconButton(painterResource(R.drawable.ic_search), stringResource(R.string.home_search_cd), onOpenSearch)
        Spacer(Modifier.width(12.dp))
        NotificationBell(unreadCount = unreadNotifications, onClick = onOpenNotifications)
    }
}

@Composable
private fun WorkoutCard(workout: DailyWorkout, onOpen: () -> Unit) {
    val c = WakTheme.colors
    val skill = workout.skill.ifBlank { stringResource(R.string.workout_skill_default) }
    val startCd = stringResource(R.string.workout_start_cd)
    Surface(
        onClick = onOpen,
        modifier = Modifier
            .padding(start = 20.dp, end = 20.dp, top = 24.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = c.s2,
        border = BorderStroke(1.dp, c.text2.copy(alpha = 0.10f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 17.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                val progress = if (workout.minutesGoal > 0) (workout.minutesDone.toFloat() / workout.minutesGoal).coerceIn(0f, 1f) else 0f
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 5.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(c.s5, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                    drawArc(c.primary, -90f, 360f * progress, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Text(stringResource(R.string.workout_minutes, workout.minutesDone, workout.minutesGoal), style = spaceGrotesk(13.sp), color = c.text)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.workout_title),
                        style = spaceGrotesk(11.sp, FontWeight.Medium),
                        color = c.text3,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (workout.streakDays > 0) {
                        Spacer(Modifier.width(6.dp))
                        Row(
                            modifier = Modifier
                                .background(alertContainer(c), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(painterResource(R.drawable.ic_trending), contentDescription = null, tint = c.peachText, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(
                                pluralStringResource(R.plurals.workout_streak, workout.streakDays, workout.streakDays),
                                style = spaceGrotesk(10.sp),
                                color = c.peachText,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.workout_level, skill, workout.level), style = spaceGrotesk(16.sp), color = c.text)
                Spacer(Modifier.height(3.dp))
                Text(
                    if (workout.accuracy != null) {
                        stringResource(R.string.workout_progress, workout.minutesDone, workout.minutesGoal, workout.accuracy)
                    } else {
                        stringResource(R.string.workout_progress_short, workout.minutesDone, workout.minutesGoal)
                    },
                    style = jakarta(12.sp),
                    color = c.text3,
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(c.primary, CircleShape)
                    .semantics { contentDescription = startCd },
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_play), contentDescription = null, tint = c.onPrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    val c = WakTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = spaceGrotesk(20.sp), color = c.text, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun SeeAll(onClick: () -> Unit) {
    val c = WakTheme.colors
    Text(
        stringResource(R.string.see_all),
        style = jakarta(13.sp, FontWeight.SemiBold),
        color = c.text,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(12.dp),
    )
}

@Composable
private fun RecommendedSection(songs: List<Song>, onOpenSong: (String) -> Unit, onSeeAll: () -> Unit) {
    val c = WakTheme.colors
    val genres = songs.map { it.genre }.filter { it.isNotBlank() }.distinct().take(2).map { it.lowercase() }
    Column(Modifier.padding(top = 26.dp)) {
        SectionHeader(stringResource(R.string.recommended_title), trailing = { SeeAll(onSeeAll) })
        if (genres.isNotEmpty()) {
            val joined = if (genres.size == 2) stringResource(R.string.recommended_and, genres[0], genres[1]) else genres[0]
            Text(
                stringResource(R.string.recommended_based_on, joined),
                style = jakarta(12.sp),
                color = c.text3,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(songs, key = { it.id }) { song -> WideSongCard(song, onClick = { onOpenSong(song.id) }) }
        }
    }
}

@Composable
private fun WideSongCard(song: Song, onClick: () -> Unit) {
    val image = songWideCover(song.id)
    val ink = WideInks[image] ?: WideInk(Color(0xFF080D1D), Color.White)
    val description = stringResource(R.string.recommended_play_cd, song.title)
    Box(
        modifier = Modifier
            .size(width = 256.dp, height = 132.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        Image(painterResource(image), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Column(Modifier.padding(16.dp)) {
            Text(song.title, style = spaceGrotesk(19.sp), color = ink.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(
                pluralStringResource(R.plurals.recommended_chords, song.distinctChords().size, song.artist, song.distinctChords().size),
                style = jakarta(12.sp),
                color = ink.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            levelLabel(song.difficulty),
            style = spaceGrotesk(11.sp, FontWeight.SemiBold),
            color = ink.tagText,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .background(ink.text, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .size(48.dp)
                .background(Color(0xFF080D1D), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_play), contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SavedChordsSection(chords: List<SavedChord>, onSeeAll: () -> Unit) {
    val c = WakTheme.colors
    Column(Modifier.padding(top = 30.dp)) {
        SectionHeader(stringResource(R.string.saved_chords_title), trailing = { if (chords.isNotEmpty()) SeeAll(onSeeAll) })
        Spacer(Modifier.height(12.dp))
        val withDiagrams = chords.filter { it.voicing != null }
        if (withDiagrams.isEmpty()) {
            Text(
                stringResource(R.string.saved_chords_empty),
                style = jakarta(13.sp),
                color = c.text3,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .background(c.s2, RoundedCornerShape(20.dp))
                    .border(1.dp, c.text2.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
                    .padding(18.dp),
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(withDiagrams, key = { it.symbol }) { chord ->
                    val voicing = chord.voicing ?: return@items
                    ChordCard(
                        name = prettyChord(chord.symbol),
                        frets = voicing.frets,
                        fingers = voicing.fingers,
                        baseFret = voicing.baseFret,
                    )
                }
            }
        }
    }
}

@Composable
private fun SongRow(song: Song, onClick: () -> Unit) {
    val c = WakTheme.colors
    val chords = song.distinctChords().take(4)
    val subtitle = if (chords.isNotEmpty()) {
        (listOf(song.artist) + chords.map { prettyChord(it) }).joinToString(" · ")
    } else {
        "${song.artist} · ${stringResource(R.string.feed_key_of, prettyChord(song.key))}"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painterResource(songCover(song.id)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp)),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, style = spaceGrotesk(16.sp), color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, style = jakarta(12.sp), color = c.text3, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(10.dp))
        LevelTag(song.difficulty)
    }
}

@Composable
private fun LevelTag(difficulty: Difficulty) {
    val c = WakTheme.colors
    Text(
        levelLabel(difficulty),
        style = spaceGrotesk(11.sp, FontWeight.SemiBold),
        color = levelColor(difficulty, c),
        modifier = Modifier
            .background(levelContainer(difficulty, c), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProgressionCard(
    item: FeedItem.ProgressionItem,
    isLiked: Boolean,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onToggleLike: () -> Unit,
) {
    val c = WakTheme.colors
    val progression = item.progression
    val likesCd = stringResource(if (isLiked) R.string.progression_unlike_cd else R.string.progression_like_cd, compactCount(progression.likeCount))
    val commentsCd = stringResource(R.string.feed_comments_cd, compactCount(item.commentCount))
    val playCd = stringResource(R.string.feed_play_cd, progression.title)
    Surface(
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = c.s2,
        border = BorderStroke(1.dp, c.text2.copy(alpha = 0.10f)),
    ) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(progression.authorName, progression.authorId, 34.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(progression.authorName, style = jakarta(13.sp, FontWeight.SemiBold), color = c.text, maxLines = 1)
                    Text(
                        stringResource(R.string.feed_progression_meta, stringResource(R.string.time_ago, relativeTime(progression.createdAt))),
                        style = jakarta(12.sp),
                        color = c.text3,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(progression.title, style = spaceGrotesk(17.sp), color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                progression.chords.forEach { chord ->
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .background(c.s3, RoundedCornerShape(14.dp))
                            .border(1.dp, c.text2.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 15.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(prettyChord(chord), style = spaceGrotesk(16.sp), color = c.text)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (progression.genre.isNotBlank()) {
                    Text(
                        progression.genre,
                        style = spaceGrotesk(11.sp, FontWeight.SemiBold),
                        color = c.text2,
                        modifier = Modifier
                            .background(c.s4, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                LevelTag(progression.difficulty)
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onToggleLike)
                        .semantics(mergeDescendants = true) { contentDescription = likesCd }
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painterResource(if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart),
                        contentDescription = null,
                        tint = if (isLiked) c.peach else c.text,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(compactCount(progression.likeCount), style = jakarta(13.sp), color = c.text)
                }
                Spacer(Modifier.width(4.dp))
                Row(Modifier.semantics(mergeDescendants = true) { contentDescription = commentsCd }, verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_comment), contentDescription = null, tint = c.text, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(compactCount(item.commentCount), style = jakarta(13.sp), color = c.text)
                }
                Spacer(Modifier.width(12.dp))
                Surface(
                    onClick = onPlay,
                    modifier = Modifier
                        .size(40.dp)
                        .semantics { contentDescription = playCd },
                    shape = CircleShape,
                    color = c.primary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_play), contentDescription = null, tint = c.onPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedFooter(state: HomeUiState) {
    val c = WakTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            state.isLoading && state.feed.isEmpty() -> CircularProgressIndicator(color = c.primary)
            state.isLoadingMore -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = c.primary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.loading_more), style = jakarta(13.sp), color = c.text3)
            }
            state.feed.isEmpty() -> Text(stringResource(R.string.feed_empty), style = jakarta(13.sp), color = c.text3, textAlign = TextAlign.Center)
            state.endReached -> Text(stringResource(R.string.feed_end), style = jakarta(13.sp), color = c.text3)
        }
    }
}

private val previewSongs = listOf(
    Song("golden-hour-glow", "Golden Hour Glow", "Koro Acoustic Trio", "G", difficulty = Difficulty.EASY, genre = "Acoustic", sections = listOf(SongSection("Verse", listOf(SongLine(listOf(ChordSegment("G", "a "), ChordSegment("D", "b "), ChordSegment("Em7", "c "), ChordSegment("Cadd9", "d "))))))),
    Song("midnight-waves", "Midnight Waves", "Neon Mirage", "E", difficulty = Difficulty.INTERMEDIATE, genre = "Neo-Soul", sections = listOf(SongSection("Verse", listOf(SongLine(listOf(ChordSegment("Emaj7", "a "), ChordSegment("G#m7", "b "), ChordSegment("F#m7", "c "), ChordSegment("B7", "d "))))))),
    Song("subtle-resonance", "Subtle Resonance", "Alex Vance ft. Koro", "Eb", difficulty = Difficulty.INTERMEDIATE, genre = "Neo-Soul"),
)

private val previewProgressions = listOf(
    ChordProgression("late-night-tension", "Late night tension", "C", 72, "Neo-Soul", Difficulty.ADVANCED, listOf("Fmaj9", "Em7", "Ebdim7", "Dm7"), "julian", "Julian Keys", likeCount = 342, createdAt = System.currentTimeMillis() - 4 * 3_600_000),
    ChordProgression("dream-pop-lift", "Dream pop lift", "A", 90, "Dream Pop", Difficulty.EASY, listOf("Asus2", "C#m7", "Badd11", "E5"), "aria", "Aria Chen", likeCount = 518, createdAt = System.currentTimeMillis() - 26 * 3_600_000),
)

private val previewState = HomeUiState(
    userName = "Keane Wilson",
    recommended = previewSongs.take(2),
    savedChords = listOf(
        SavedChord("Cadd9", ChordVoicing(listOf(-1, 3, 2, 0, 3, 3), listOf(0, 2, 1, 0, 3, 4), 1, 0)),
        SavedChord("Am7", ChordVoicing(listOf(-1, 0, 2, 0, 1, 0), listOf(0, 0, 2, 0, 1, 0), 1, 0)),
        SavedChord("Fmaj7", ChordVoicing(listOf(-1, -1, 3, 2, 1, 0), listOf(0, 0, 3, 2, 1, 0), 1, 0)),
    ),
    likedIds = setOf("late-night-tension"),
    feed = listOf(
        FeedItem.SongItem(previewSongs[2]),
        FeedItem.ProgressionItem(previewProgressions[0], commentCount = 28),
        FeedItem.SongItem(previewSongs[1]),
        FeedItem.SongItem(previewSongs[0]),
        FeedItem.ProgressionItem(previewProgressions[1], commentCount = 41),
    ),
    workout = DailyWorkout(minutesDone = 5, minutesGoal = 8, streakDays = 5, skill = "Intervals", level = 2, accuracy = 94),
    isLoading = false,
    isLoadingMore = true,
)

@Preview(name = "Home", heightDp = 1600)
@Composable
private fun HomePreview() {
    WAKordTheme(darkTheme = true) {
        HomeScreen(previewState, {}, {}, {}, {}, {}, {}, {}, unreadNotifications = 3)
    }
}

@Preview(name = "Home - new user", heightDp = 900)
@Composable
private fun HomeNewUserPreview() {
    WAKordTheme(darkTheme = true) {
        HomeScreen(
            HomeUiState(userName = "Keane Wilson", isLoading = false, endReached = true),
            {},
            {},
            {},
            {},
            {},
            {},
            {},
        )
    }
}

@Preview(name = "Home - light", heightDp = 1600)
@Composable
private fun HomeLightPreview() {
    WAKordTheme(darkTheme = false) {
        HomeScreen(previewState, {}, {}, {}, {}, {}, {}, {}, unreadNotifications = 3)
    }
}
