package edu.bluejack26_1.wakord.ui.feature.song

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
import edu.bluejack26_1.wakord.core.music.ChordTransposer
import edu.bluejack26_1.wakord.domain.model.ChordSegment
import edu.bluejack26_1.wakord.domain.model.Comment
import edu.bluejack26_1.wakord.domain.model.Difficulty
import edu.bluejack26_1.wakord.domain.model.Song
import edu.bluejack26_1.wakord.domain.model.SongLine
import edu.bluejack26_1.wakord.domain.model.SongSection
import edu.bluejack26_1.wakord.ui.components.AvatarSmall
import edu.bluejack26_1.wakord.ui.components.BackgroundArt
import edu.bluejack26_1.wakord.ui.components.GlassIconButton
import edu.bluejack26_1.wakord.ui.components.UserAvatar
import edu.bluejack26_1.wakord.ui.components.compactCount
import edu.bluejack26_1.wakord.ui.components.glassBorder
import edu.bluejack26_1.wakord.ui.components.glassColor
import edu.bluejack26_1.wakord.ui.components.keyLabel
import edu.bluejack26_1.wakord.ui.components.levelColor
import edu.bluejack26_1.wakord.ui.components.levelLabel
import edu.bluejack26_1.wakord.ui.components.prettyChord
import edu.bluejack26_1.wakord.ui.components.relativeTime
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.jakarta
import edu.bluejack26_1.wakord.ui.theme.spaceGrotesk
import java.util.Locale

private const val BEATS_PER_CHORD = 2

@Composable
fun SongDetailScreen(
    state: SongDetailUiState,
    onAction: (SongDetailAction) -> Unit,
    onBack: () -> Unit,
    onOpenComments: () -> Unit,
    onShare: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = WakTheme.colors
    val song = state.song
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        if (song != null) {
            BackgroundArt(songBackground(song.id), height = 700.dp)
        }
        when {
            song != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(bottom = if (state.playingPosition != null) 230.dp else 188.dp),
                ) {
                    SongHeader(onBack = onBack, onReset = { onAction(SongDetailAction.ResetTranspose) }, onShare = { onShare(song) }, canReset = state.transposeSteps != 0)
                    SongHero(song)
                    SongInfo(song)
                    SongActions(state, song, onAction, onOpenComments, onShare = { onShare(song) })
                    TransposeCard(state, onAction)
                    song.sections.forEachIndexed { index, section ->
                        SectionBlock(index, section, state)
                    }
                    CommentsPreview(state, onOpenComments)
                }
                PlayerBar(
                    state = state,
                    song = song,
                    onAction = onAction,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                )
            }
            state.isLoading -> CircularProgressIndicator(color = c.primary, modifier = Modifier.align(Alignment.Center))
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                SongHeader(onBack = onBack, onReset = {}, onShare = {}, canReset = false, showMenu = false)
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.song_not_found), style = jakarta(14.sp), color = c.text2, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun SongHeader(onBack: () -> Unit, onReset: () -> Unit, onShare: () -> Unit, canReset: Boolean, showMenu: Boolean = true) {
    val c = WakTheme.colors
    var menuOpen by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(44.dp),
    ) {
        GlassIconButton(painterResource(R.drawable.ic_back), stringResource(R.string.back), onBack, Modifier.align(Alignment.CenterStart))
        Text(
            stringResource(R.string.song_chord_sheet),
            style = spaceGrotesk(14.sp),
            color = c.text,
            modifier = Modifier.align(Alignment.Center),
        )
        if (showMenu) {
            Box(Modifier.align(Alignment.CenterEnd)) {
                GlassIconButton(painterResource(R.drawable.ic_more), stringResource(R.string.more_options), { menuOpen = true })
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = c.s3) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.song_reset_transpose), style = jakarta(14.sp), color = if (canReset) c.text else c.text3) },
                        enabled = canReset,
                        onClick = {
                            menuOpen = false
                            onReset()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.share), style = jakarta(14.sp), color = c.text) },
                        onClick = {
                            menuOpen = false
                            onShare()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SongHero(song: Song) {
    val c = WakTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(songCover(song.id)),
            contentDescription = stringResource(R.string.song_cover_cd, song.title),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(208.dp)
                .clip(RoundedCornerShape(30.dp)),
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .height(36.dp)
                .background(glassColor(c), CircleShape)
                .border(glassBorder(c), CircleShape)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_heart_filled), contentDescription = null, tint = c.peach, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.song_stats, compactCount(song.likeCount), compactCount(song.saveCount)),
                style = jakarta(12.sp),
                color = c.text2,
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            song.title,
            style = spaceGrotesk(26.sp),
            color = c.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(song.artist, style = jakarta(14.sp), color = c.text2, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SongInfo(song: Song) {
    val c = WakTheme.colors
    Row(
        modifier = Modifier
            .padding(start = 20.dp, end = 20.dp, top = 20.dp)
            .fillMaxWidth()
            .height(64.dp)
            .background(glassColor(c), RoundedCornerShape(22.dp))
            .border(glassBorder(c), RoundedCornerShape(22.dp))
            .padding(vertical = 13.dp, horizontal = 5.dp),
    ) {
        InfoCell(stringResource(R.string.song_key), prettyChord(song.key), c.text, divider = false)
        InfoCell(stringResource(R.string.song_capo), capoLabel(song.capo), c.text)
        InfoCell(stringResource(R.string.song_tempo), song.tempo.toString(), c.text)
        InfoCell(stringResource(R.string.song_level), levelLabel(song.difficulty, short = true), levelColor(song.difficulty, c))
    }
}

@Composable
private fun RowScope.InfoCell(label: String, value: String, valueColor: Color, divider: Boolean = true) {
    val c = WakTheme.colors
    Row(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
    ) {
        if (divider) {
            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(c.text2.copy(alpha = 0.10f)),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, style = spaceGrotesk(11.sp, FontWeight.Medium), color = c.text3)
            Spacer(Modifier.height(3.dp))
            Text(value, style = spaceGrotesk(16.sp), color = valueColor, maxLines = 1)
        }
    }
}

@Composable
private fun SongActions(
    state: SongDetailUiState,
    song: Song,
    onAction: (SongDetailAction) -> Unit,
    onOpenComments: () -> Unit,
    onShare: () -> Unit,
) {
    val c = WakTheme.colors
    val likes = compactCount(song.likeCount)
    val likeCd = stringResource(if (state.isLiked) R.string.song_unlike_cd else R.string.song_like_cd, likes)
    val commentsCd = stringResource(R.string.song_comments_cd, state.comments.size)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionChip(
            icon = if (state.isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart,
            iconTint = if (state.isLiked) c.peach else c.text,
            text = likes,
            onClick = { onAction(SongDetailAction.ToggleLike) },
            modifier = Modifier.semantics { contentDescription = likeCd },
        )
        ActionChip(
            icon = if (state.isSaved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark,
            iconTint = if (state.isSaved) c.primaryText else c.text,
            text = stringResource(if (state.isSaved) R.string.song_saved else R.string.song_save),
            onClick = { onAction(SongDetailAction.ToggleSave) },
        )
        ActionChip(
            icon = R.drawable.ic_comment,
            iconTint = c.text,
            text = compactCount(state.comments.size),
            onClick = onOpenComments,
            modifier = Modifier.semantics { contentDescription = commentsCd },
        )
        Spacer(Modifier.weight(1f))
        Surface(
            onClick = onShare,
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = c.s3,
            border = BorderStroke(1.dp, c.text2.copy(alpha = 0.10f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_share), contentDescription = stringResource(R.string.share), tint = c.text, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ActionChip(icon: Int, iconTint: Color, text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = CircleShape,
        color = c.s3,
        border = BorderStroke(1.dp, c.text2.copy(alpha = 0.10f)),
    ) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(icon), contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, style = jakarta(16.sp), color = c.text, maxLines = 1)
        }
    }
}

@Composable
private fun TransposeCard(state: SongDetailUiState, onAction: (SongDetailAction) -> Unit) {
    val c = WakTheme.colors
    val original = keyLabel(state.originalKey)
    val subtitle = if (state.transposeSteps == 0) {
        stringResource(R.string.transpose_original, original)
    } else {
        stringResource(R.string.transpose_change, original, keyLabel(state.song?.key.orEmpty()))
    }
    Row(
        modifier = Modifier
            .padding(start = 20.dp, end = 20.dp, top = 14.dp)
            .fillMaxWidth()
            .height(60.dp)
            .background(c.s2, RoundedCornerShape(20.dp))
            .border(1.dp, c.text2.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
            .padding(start = 17.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.transpose), style = spaceGrotesk(14.sp), color = c.text)
            Spacer(Modifier.height(1.dp))
            Text(subtitle, style = jakarta(12.sp), color = c.text3, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        StepButton(R.drawable.ic_minus, stringResource(R.string.transpose_down_cd)) { onAction(SongDetailAction.Transpose(-1)) }
        Text(
            signedSteps(state.transposeSteps),
            style = spaceGrotesk(16.sp),
            color = if (state.transposeSteps == 0) c.text else c.primaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(44.dp),
        )
        StepButton(R.drawable.ic_plus, stringResource(R.string.transpose_up_cd)) { onAction(SongDetailAction.Transpose(1)) }
    }
}

@Composable
private fun StepButton(icon: Int, description: String, onClick: () -> Unit) {
    val c = WakTheme.colors
    Surface(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = c.s3,
        border = BorderStroke(1.dp, c.text2.copy(alpha = 0.10f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = description, tint = c.text, modifier = Modifier.size(18.dp))
        }
    }
}

private enum class ChordMark { NONE, NOW, NEXT }

private fun markOf(state: SongDetailUiState, position: ChordPosition): ChordMark = when (position) {
    state.playingPosition -> ChordMark.NOW
    state.nextPosition -> ChordMark.NEXT
    else -> ChordMark.NONE
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SectionBlock(sectionIndex: Int, section: SongSection, state: SongDetailUiState) {
    val c = WakTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 28.dp),
    ) {
        Text(section.name, style = spaceGrotesk(11.sp, FontWeight.Medium), color = c.text3)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            section.lines.forEachIndexed { lineIndex, line ->
                if (line.isChordOnly()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        line.segments.forEachIndexed { segmentIndex, segment ->
                            val chord = segment.chord ?: return@forEachIndexed
                            ChordChip(chord, markOf(state, ChordPosition(sectionIndex, lineIndex, segmentIndex)))
                        }
                    }
                } else {
                    FlowRow(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        line.segments.forEachIndexed { segmentIndex, segment ->
                            LyricSegment(segment, markOf(state, ChordPosition(sectionIndex, lineIndex, segmentIndex)))
                        }
                    }
                }
            }
        }
    }
}

private fun SongLine.isChordOnly() = segments.isNotEmpty() && segments.all { it.lyric.isBlank() }

@Composable
private fun ChordChip(chord: String, mark: ChordMark) {
    val c = WakTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .height(36.dp)
            .background(if (mark == ChordMark.NOW) c.primary else c.s3, shape)
            .border(1.dp, if (mark == ChordMark.NEXT) c.primary else c.text2.copy(alpha = 0.10f), shape)
            .padding(horizontal = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(prettyChord(chord), style = spaceGrotesk(14.sp), color = if (mark == ChordMark.NOW) c.onPrimary else c.primaryText)
    }
}

@Composable
private fun LyricSegment(segment: ChordSegment, mark: ChordMark) {
    val c = WakTheme.colors
    Column(Modifier.width(IntrinsicSize.Max)) {
        val chord = segment.chord
        val shape = RoundedCornerShape(6.dp)
        Box(
            Modifier
                .height(18.dp)
                .padding(end = 6.dp),
        ) {
            if (chord != null) {
                Text(
                    prettyChord(chord),
                    style = spaceGrotesk(13.sp),
                    color = if (mark == ChordMark.NOW) c.onPrimary else c.primaryText,
                    maxLines = 1,
                    modifier = Modifier
                        .background(if (mark == ChordMark.NOW) c.primary else Color.Transparent, shape)
                        .border(1.dp, if (mark == ChordMark.NEXT) c.primary else Color.Transparent, shape)
                        .padding(horizontal = if (mark == ChordMark.NONE) 0.dp else 4.dp),
                )
            }
        }
        Text(segment.lyric, style = jakarta(16.sp), color = c.text)
    }
}

@Composable
private fun CommentsPreview(state: SongDetailUiState, onOpenComments: () -> Unit) {
    val c = WakTheme.colors
    val top = state.comments.filter { it.parentId == null }.maxByOrNull { it.likeCount }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 34.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.comments_count, compactCount(state.comments.size)),
                style = spaceGrotesk(20.sp),
                color = c.text,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.view_all),
                style = jakarta(13.sp, FontWeight.SemiBold),
                color = c.text,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onOpenComments)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        if (top != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(c.s2, RoundedCornerShape(22.dp))
                    .border(1.dp, c.text2.copy(alpha = 0.10f), RoundedCornerShape(22.dp))
                    .clickable(onClick = onOpenComments)
                    .padding(17.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(top.authorName, top.authorId, AvatarSmall, fontSize = 11.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(top.authorName, style = jakarta(13.sp, FontWeight.SemiBold), color = c.text, maxLines = 1)
                        Text(stringResource(R.string.time_ago, relativeTime(top.createdAt)), style = jakarta(12.sp), color = c.text3)
                    }
                    Icon(
                        painterResource(if (top.likedByMe) R.drawable.ic_heart_filled else R.drawable.ic_heart),
                        contentDescription = null,
                        tint = if (top.likedByMe) c.peach else c.text3,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(top.likeCount.toString(), style = jakarta(12.sp), color = c.text3)
                }
                Spacer(Modifier.height(10.dp))
                Text(top.text, style = jakarta(14.sp), color = c.text, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(14.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(c.s2)
                .border(1.dp, c.outline, RoundedCornerShape(16.dp))
                .clickable(onClick = onOpenComments)
                .padding(horizontal = 17.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.comment_placeholder), style = jakarta(13.sp), color = c.text3, modifier = Modifier.weight(1f))
            Icon(painterResource(R.drawable.ic_send), contentDescription = null, tint = c.text, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PlayerBar(state: SongDetailUiState, song: Song, onAction: (SongDetailAction) -> Unit, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    val chordMs = (60_000L * BEATS_PER_CHORD / song.tempo.coerceAtLeast(1) / state.playbackSpeed).toLong()
    val total = if (state.totalMs > 0) state.totalMs else chordMs * SongDetailViewModel.chordPositions(song).size
    val target = if (total <= 0) 0f else ((state.elapsedMs + if (state.isPlaying) chordMs else 0L).toFloat() / total).coerceIn(0f, 1f)
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = if (state.isPlaying) chordMs.toInt() else 200, easing = LinearEasing),
        label = "progress",
    )
    val speedLabel = String.format(Locale.US, if (state.playbackSpeed == 0.75f) "%.2fx" else "%.1fx", state.playbackSpeed)
    val speedCd = stringResource(R.string.playback_speed_cd, speedLabel)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(24.dp, RoundedCornerShape(30.dp), ambientColor = Color.Black, spotColor = Color.Black)
            .background(c.s2, RoundedCornerShape(30.dp))
            .border(glassBorder(c), RoundedCornerShape(30.dp))
            .padding(17.dp),
    ) {
        AnimatedVisibility(visible = state.playingPosition != null) {
            Row(
                modifier = Modifier.padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.player_now), style = spaceGrotesk(11.sp, FontWeight.Medium), color = c.text3)
                Spacer(Modifier.width(8.dp))
                PlayerChord(song.chordAt(state.playingPosition), now = true)
                val next = song.chordAt(state.nextPosition)
                if (next != null) {
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.player_next), style = jakarta(12.sp), color = c.text3)
                    Spacer(Modifier.width(8.dp))
                    PlayerChord(next, now = false)
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(c.s5, CircleShape),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(c.primary, CircleShape),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(formatClock(state.elapsedMs), style = spaceGrotesk(12.sp, FontWeight.Medium), color = c.text3, modifier = Modifier.weight(1f))
            Text(formatClock(total), style = spaceGrotesk(12.sp, FontWeight.Medium), color = c.text3)
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerIconButton(
                icon = R.drawable.ic_loop,
                description = stringResource(if (state.isLooping) R.string.loop_on_cd else R.string.loop_off_cd),
                active = state.isLooping,
                onClick = { onAction(SongDetailAction.ToggleLoop) },
            )
            PlayerIconButton(
                icon = R.drawable.ic_previous,
                description = stringResource(R.string.previous_chord),
                onClick = { onAction(SongDetailAction.SkipChord(-1)) },
            )
            Surface(
                onClick = { onAction(SongDetailAction.TogglePlay) },
                modifier = Modifier.size(width = 76.dp, height = 54.dp),
                shape = RoundedCornerShape(20.dp),
                color = c.primary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painterResource(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                        contentDescription = stringResource(if (state.isPlaying) R.string.pause_progression else R.string.play_progression),
                        tint = c.onPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            PlayerIconButton(
                icon = R.drawable.ic_next,
                description = stringResource(R.string.next_chord),
                onClick = { onAction(SongDetailAction.SkipChord(1)) },
            )
            Text(
                speedLabel,
                style = spaceGrotesk(13.sp),
                color = c.text,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(48.dp)
                    .clip(CircleShape)
                    .clickable {
                        val index = PlaybackSpeeds.indexOf(state.playbackSpeed)
                        onAction(SongDetailAction.SetSpeed(PlaybackSpeeds[(index + 1) % PlaybackSpeeds.size]))
                    }
                    .semantics { contentDescription = speedCd }
                    .padding(vertical = 13.dp),
            )
        }
    }
}

@Composable
private fun PlayerIconButton(icon: Int, description: String, onClick: () -> Unit, active: Boolean = false) {
    val c = WakTheme.colors
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (active) c.primary.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = if (active) c.primaryText else c.text, modifier = Modifier.size(20.dp))
    }
}

private fun Song.chordAt(position: ChordPosition?): String? = position?.let {
    sections.getOrNull(it.section)?.lines?.getOrNull(it.line)?.segments?.getOrNull(it.segment)?.chord
}

@Composable
private fun PlayerChord(chord: String?, now: Boolean) {
    if (chord == null) return
    val c = WakTheme.colors
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .height(30.dp)
            .background(if (now) c.primary else c.s3, shape)
            .border(1.dp, if (now) c.primary else c.text2.copy(alpha = 0.10f), shape)
            .padding(horizontal = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(prettyChord(chord), style = spaceGrotesk(14.sp), color = if (now) c.onPrimary else c.text)
    }
}

internal val PreviewSong = Song(
    id = "subtle-resonance",
    title = "Subtle Resonance",
    artist = "Alex Vance ft. Koro",
    key = "Eb",
    capo = 1,
    tempo = 78,
    difficulty = Difficulty.INTERMEDIATE,
    likeCount = 2400,
    saveCount = 310,
    sections = listOf(
        SongSection("Intro", listOf(SongLine(listOf(ChordSegment("Ebmaj7", " "), ChordSegment("Gm7", " "), ChordSegment("Abmaj7", " "), ChordSegment("Bb7sus4", " "))))),
        SongSection(
            "Verse 1",
            listOf(
                SongLine(listOf(ChordSegment("Ebmaj7", "Wandering through the "), ChordSegment("Gm7", "midnight glow"))),
                SongLine(listOf(ChordSegment("Abmaj7", "Catching harmonics "), ChordSegment("Bb7sus4", "only we could know"))),
                SongLine(listOf(ChordSegment("Cm7", "The tape spins softly "), ChordSegment("Fm7", "in the dim blue haze"))),
            ),
        ),
        SongSection("Chorus", listOf(SongLine(listOf(ChordSegment("Ebmaj7", "Hold the frequency, "), ChordSegment("Bb7sus4", "never let it fade away"))))),
    ),
)

internal val PreviewComments = listOf(
    Comment("c1", "u1", "Marcus L.", "Try voicing the B♭7sus4 as an open chord from the 3rd fret. The top E♭ rings out so much sweeter.", likeCount = 29, createdAt = System.currentTimeMillis() - 2 * 3_600_000),
    Comment("c2", "u2", "Aria Chen", "Capo 1 and playing in D shapes makes the verse way easier for beginners.", likeCount = 14, createdAt = System.currentTimeMillis() - 5 * 3_600_000),
    Comment("c3", "u3", "Julian Keys", "The Cm7 to Fm7 move is the whole mood of this song.", likeCount = 9, likedByMe = true, createdAt = System.currentTimeMillis() - 26 * 3_600_000),
    Comment("c4", "u4", "Maya Sol", "Anyone have a fingerstyle version of the intro?", likeCount = 4, createdAt = System.currentTimeMillis() - 50 * 3_600_000),
    Comment("c5", "u1", "Marcus L.", "Try it with the low E muted too.", parentId = "c4", likeCount = 1, createdAt = System.currentTimeMillis() - 40 * 3_600_000),
)

private val previewState = SongDetailUiState(
    isLoading = false,
    song = PreviewSong,
    originalKey = "Eb",
    comments = PreviewComments,
    isSaved = true,
    isLiked = true,
)

@Preview(name = "Song detail", heightDp = 1600)
@Composable
private fun SongDetailPreview() {
    WAKordTheme(darkTheme = true) {
        SongDetailScreen(previewState, {}, {}, {}, {})
    }
}

@Preview(name = "Song detail - playing", heightDp = 1600)
@Composable
private fun SongDetailPlayingPreview() {
    WAKordTheme(darkTheme = true) {
        SongDetailScreen(
            previewState.copy(
                isPlaying = true,
                playingPosition = ChordPosition(1, 1, 0),
                nextPosition = ChordPosition(1, 1, 1),
                elapsedMs = 9_000,
                totalMs = 33_000,
            ),
            {},
            {},
            {},
            {},
        )
    }
}

@Preview(name = "Song detail - transposed", heightDp = 1600)
@Composable
private fun SongDetailTransposedPreview() {
    WAKordTheme(darkTheme = true) {
        SongDetailScreen(
            previewState.copy(
                transposeSteps = 2,
                song = ChordTransposer.transposeSong(PreviewSong, 2),
            ),
            {},
            {},
            {},
            {},
        )
    }
}

@Preview(name = "Song detail - light", heightDp = 1600)
@Composable
private fun SongDetailLightPreview() {
    WAKordTheme(darkTheme = false) {
        SongDetailScreen(previewState, {}, {}, {}, {})
    }
}
