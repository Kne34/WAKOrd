package edu.bluejack26_1.wakord.ui.feature.progression

import androidx.annotation.DrawableRes
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import edu.bluejack26_1.wakord.domain.model.ChordProgression
import edu.bluejack26_1.wakord.domain.model.ChordVoicing
import edu.bluejack26_1.wakord.domain.model.Comment
import edu.bluejack26_1.wakord.domain.model.Difficulty
import edu.bluejack26_1.wakord.ui.components.BackgroundArt
import edu.bluejack26_1.wakord.ui.components.ChordDiagram
import edu.bluejack26_1.wakord.ui.components.GlassIconButton
import edu.bluejack26_1.wakord.ui.components.InitialsAvatar
import edu.bluejack26_1.wakord.ui.components.levelContainer
import edu.bluejack26_1.wakord.ui.components.compactCount
import edu.bluejack26_1.wakord.ui.components.keyLabel
import edu.bluejack26_1.wakord.ui.components.levelColor
import edu.bluejack26_1.wakord.ui.components.levelLabel
import edu.bluejack26_1.wakord.ui.components.prettyChord
import edu.bluejack26_1.wakord.ui.components.relativeTime
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.jakarta
import edu.bluejack26_1.wakord.ui.theme.spaceGrotesk

private val Backgrounds = mapOf(
    "neo-soul-04" to R.drawable.bg_prog_neo_soul_04,
    "late-night-tension" to R.drawable.bg_prog_late_night_tension,
    "rainy-window-loop" to R.drawable.bg_prog_rainy_window_loop,
)

private val Covers = mapOf(
    "neo-soul-04" to R.drawable.cover_prog_neo_soul_04,
    "late-night-tension" to R.drawable.cover_prog_late_night_tension,
    "rainy-window-loop" to R.drawable.cover_prog_rainy_window_loop,
)

@DrawableRes
fun progressionCover(id: String): Int =
    Covers[id] ?: Covers.values.toList()[Math.floorMod(id.hashCode(), Covers.size)]

@DrawableRes
fun progressionBackground(id: String): Int =
    Backgrounds[id] ?: Backgrounds.values.toList()[Math.floorMod(id.hashCode(), Backgrounds.size)]

@Composable
fun ProgressionDetailScreen(
    state: ProgressionDetailUiState,
    onAction: (ProgressionDetailAction) -> Unit,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onOpenComments: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = WakTheme.colors
    val progression = state.progression
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        if (progression != null) {
            BackgroundArt(progressionBackground(progression.id), height = 560.dp)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .height(44.dp),
            ) {
                GlassIconButton(painterResource(R.drawable.ic_back), stringResource(R.string.back), onBack, Modifier.align(Alignment.CenterStart))
                Text(
                    stringResource(R.string.progression_header),
                    style = spaceGrotesk(14.sp),
                    color = c.text,
                    modifier = Modifier.align(Alignment.Center),
                )
                if (progression != null) {
                    GlassIconButton(painterResource(R.drawable.ic_share), stringResource(R.string.share), onShare, Modifier.align(Alignment.CenterEnd))
                }
            }
            when {
                progression != null -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp),
                ) {
                    ProgressionInfo(progression)
                    ChordGrid(progression.chords, state.chordDiagrams, state.playingIndex, onOpen = { onAction(ProgressionDetailAction.OpenChord(it)) })
                    ProgressionActions(state, progression, onAction)
                    CommentsSection(state.comments, onOpenComments)
                }
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = c.primary)
                }
                else -> Box(Modifier.fillMaxSize().padding(horizontal = 40.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.progression_not_found), style = jakarta(14.sp), color = c.text2, textAlign = TextAlign.Center)
                }
            }
        }
    }

    if (state.selectedChord != null) {
        ChordDetailSheet(state, onAction)
    }
}

@Composable
private fun ProgressionInfo(progression: ChordProgression) {
    val c = WakTheme.colors
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            InfoTag(progression.genre, c.s4, c.text2)
            InfoTag(levelLabel(progression.difficulty), levelContainer(progression.difficulty, c), levelColor(progression.difficulty, c))
            InfoTag(stringResource(R.string.progression_key_bpm, keyLabel(progression.key), progression.bpm), c.s4, c.text2)
        }
        Spacer(Modifier.height(14.dp))
        Text(progression.title, style = spaceGrotesk(30.sp).copy(lineHeight = 38.sp), color = c.text)
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            InitialsAvatar(progression.authorName, progression.authorId, 36.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(progression.authorName, style = jakarta(13.sp, FontWeight.SemiBold), color = c.text, maxLines = 1)
                Text(
                    stringResource(R.string.progression_author_stats, compactCount(progression.playCount), compactCount(progression.saveCount)),
                    style = jakarta(12.sp),
                    color = c.text3,
                )
            }
        }
    }
}

@Composable
private fun InfoTag(text: String, container: Color, content: Color) {
    if (text.isBlank()) return
    Text(
        text,
        style = spaceGrotesk(11.sp, FontWeight.SemiBold),
        color = content,
        maxLines = 1,
        modifier = Modifier
            .background(container, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
    )
}

@Composable
private fun ChordGrid(chords: List<String>, diagrams: List<ChordVoicing?>, playingIndex: Int?, onOpen: (String) -> Unit) {
    Column(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        chords.withIndex().chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (index, chord) ->
                    ProgressionChordCard(
                        chord = chord,
                        voicing = diagrams.getOrNull(index),
                        playing = index == playingIndex,
                        onClick = { onOpen(chord) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProgressionChordCard(chord: String, voicing: ChordVoicing?, playing: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    val description = stringResource(R.string.progression_chord_cd, prettyChord(chord))
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(192.dp)
            .semantics { contentDescription = description },
        shape = RoundedCornerShape(22.dp),
        color = if (playing) c.s3 else c.s2,
        border = BorderStroke(1.dp, if (playing) c.primary else c.text2.copy(alpha = 0.10f)),
    ) {
        Column(Modifier.padding(start = 15.dp, end = 15.dp, top = 13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(prettyChord(chord), style = spaceGrotesk(20.sp), color = c.text, modifier = Modifier.weight(1f), maxLines = 1)
                if (voicing != null) {
                    Text(
                        if (voicing.baseFret <= 1) stringResource(R.string.chord_open) else stringResource(R.string.chord_base_fret, voicing.baseFret),
                        style = spaceGrotesk(12.sp, FontWeight.Medium),
                        color = c.text3,
                    )
                }
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (voicing != null) {
                    ChordDiagram(
                        frets = voicing.frets,
                        fingers = voicing.fingers,
                        baseFret = voicing.baseFret,
                        chordName = chord,
                        width = 112.dp,
                    )
                } else {
                    Text("–", style = spaceGrotesk(20.sp), color = c.text3, modifier = Modifier.padding(top = 50.dp))
                }
            }
        }
    }
}

@Composable
private fun ProgressionActions(state: ProgressionDetailUiState, progression: ChordProgression, onAction: (ProgressionDetailAction) -> Unit) {
    val c = WakTheme.colors
    val likes = compactCount(progression.likeCount)
    Row(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            onClick = { onAction(ProgressionDetailAction.TogglePlay) },
            modifier = Modifier
                .weight(1f)
                .height(54.dp),
            shape = RoundedCornerShape(18.dp),
            color = c.primary,
        ) {
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                    contentDescription = null,
                    tint = c.onPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(if (state.isPlaying) R.string.progression_pause else R.string.progression_play),
                    style = jakarta(16.sp, FontWeight.SemiBold),
                    color = c.onPrimary,
                )
            }
        }
        SquareAction(
            icon = if (state.isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart,
            tint = if (state.isLiked) c.peach else c.text,
            description = stringResource(if (state.isLiked) R.string.progression_unlike_cd else R.string.progression_like_cd, likes),
            onClick = { onAction(ProgressionDetailAction.ToggleLike) },
        )
        SquareAction(
            icon = if (state.isSaved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark,
            tint = if (state.isSaved) c.primaryText else c.text,
            description = stringResource(if (state.isSaved) R.string.progression_unsave_cd else R.string.progression_save_cd),
            onClick = { onAction(ProgressionDetailAction.ToggleSave) },
        )
    }
    Row(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_heart), contentDescription = null, tint = c.text3, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text(stringResource(R.string.progression_likes, likes), style = jakarta(12.sp), color = c.text3)
        Spacer(Modifier.width(16.dp))
        Icon(painterResource(R.drawable.ic_comment), contentDescription = null, tint = c.text3, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text(stringResource(R.string.progression_comments, compactCount(state.comments.size)), style = jakarta(12.sp), color = c.text3)
    }
}

@Composable
private fun SquareAction(@DrawableRes icon: Int, tint: Color, description: String, onClick: () -> Unit) {
    val c = WakTheme.colors
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(54.dp)
            .semantics { contentDescription = description },
        shape = RoundedCornerShape(18.dp),
        color = c.s3,
        border = BorderStroke(1.dp, c.text2.copy(alpha = 0.10f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun CommentsSection(comments: List<Comment>, onOpenComments: () -> Unit) {
    val c = WakTheme.colors
    val preview = comments.filter { it.parentId == null }.sortedByDescending { it.createdAt }.take(2)
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.comments), style = spaceGrotesk(20.sp), color = c.text, modifier = Modifier.weight(1f))
            if (comments.isNotEmpty()) {
                Text(
                    stringResource(R.string.progression_view_all, compactCount(comments.size)),
                    style = jakarta(13.sp, FontWeight.SemiBold),
                    color = c.text,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onOpenComments)
                        .padding(12.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        preview.forEach { comment ->
            Row(Modifier.padding(bottom = 12.dp)) {
                InitialsAvatar(comment.authorName, comment.authorId, 34.dp, fontSize = 11.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(comment.authorName, style = jakarta(13.sp, FontWeight.SemiBold), color = c.text, maxLines = 1)
                        Spacer(Modifier.width(6.dp))
                        Text(relativeTime(comment.createdAt), style = jakarta(12.sp), color = c.text3)
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(comment.text, style = jakarta(14.sp), color = c.text, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
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
            Text(stringResource(R.string.progression_add_comment), style = jakarta(13.sp), color = c.text3, modifier = Modifier.weight(1f))
            Icon(painterResource(R.drawable.ic_send), contentDescription = null, tint = c.text, modifier = Modifier.size(18.dp))
        }
    }
}

internal val PreviewProgression = ChordProgression(
    id = "neo-soul-04",
    title = "Neo-Soul Progression #04",
    key = "C",
    bpm = 84,
    genre = "Neo-Soul",
    difficulty = Difficulty.ADVANCED,
    chords = listOf("Dm9", "G13", "Cmaj9", "Am7"),
    authorId = "julian",
    authorName = "Julian Keys",
    playCount = 1800,
    likeCount = 342,
    saveCount = 89,
)

internal val PreviewDiagrams = listOf(
    ChordVoicing(listOf(-1, 5, 3, 5, 5, -1), listOf(0, 2, 1, 3, 4, 0), 3, 3),
    ChordVoicing(listOf(3, -1, 3, 4, 5, 5), listOf(1, 0, 1, 2, 3, 4), 3, 3),
    ChordVoicing(listOf(-1, 3, 2, 4, 3, -1), listOf(0, 2, 1, 4, 3, 0), 1, 0),
    ChordVoicing(listOf(-1, 0, 2, 0, 1, 0), listOf(0, 0, 2, 0, 1, 0), 1, 0),
)

private val previewComments = listOf(
    Comment("p1", "aria", "Aria Chen", "That G13 into Cmaj9 is butter. Stealing this for my next cover.", createdAt = System.currentTimeMillis() - 3_600_000),
    Comment("p2", "marcus", "Marcus L.", "Try a half-step slide into the Dm9, it adds so much tension.", createdAt = System.currentTimeMillis() - 3 * 3_600_000),
)

@Preview(name = "Progression detail", heightDp = 1240)
@Composable
private fun ProgressionDetailPreview() {
    WAKordTheme(darkTheme = true) {
        ProgressionDetailScreen(
            state = ProgressionDetailUiState(
                isLoading = false,
                progression = PreviewProgression,
                chordDiagrams = PreviewDiagrams,
                isPlaying = true,
                playingIndex = 1,
                isLiked = true,
                comments = previewComments,
            ),
            onAction = {},
            onBack = {},
            onShare = {},
            onOpenComments = {},
        )
    }
}

@Preview(name = "Progression detail - light", heightDp = 1240)
@Composable
private fun ProgressionDetailLightPreview() {
    WAKordTheme(darkTheme = false) {
        ProgressionDetailScreen(
            state = ProgressionDetailUiState(isLoading = false, progression = PreviewProgression, chordDiagrams = PreviewDiagrams, isSaved = true),
            onAction = {},
            onBack = {},
            onShare = {},
            onOpenComments = {},
        )
    }
}
