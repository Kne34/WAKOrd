package edu.bluejack26_1.wakord.ui.feature.analyze

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.core.analysis.AnalysisInsights
import edu.bluejack26_1.wakord.core.music.Chord
import edu.bluejack26_1.wakord.core.music.ChordTheory
import edu.bluejack26_1.wakord.core.music.NoteNames
import edu.bluejack26_1.wakord.domain.model.AnalysisRecommendations
import edu.bluejack26_1.wakord.domain.model.AnalysisResult
import edu.bluejack26_1.wakord.domain.model.ChordProgression
import edu.bluejack26_1.wakord.domain.model.Difficulty
import edu.bluejack26_1.wakord.domain.model.Song
import edu.bluejack26_1.wakord.ui.components.BackgroundArt
import edu.bluejack26_1.wakord.ui.components.GlassIconButton
import edu.bluejack26_1.wakord.ui.components.InitialsAvatar
import edu.bluejack26_1.wakord.ui.components.WakButton
import edu.bluejack26_1.wakord.ui.components.WakButtonVariant
import edu.bluejack26_1.wakord.ui.components.WakDialog
import edu.bluejack26_1.wakord.ui.components.WakTextField
import edu.bluejack26_1.wakord.ui.components.alertContainer
import edu.bluejack26_1.wakord.ui.components.glassBorder
import edu.bluejack26_1.wakord.ui.components.glassColor
import edu.bluejack26_1.wakord.ui.components.keyLabel
import edu.bluejack26_1.wakord.ui.components.prettyChord
import edu.bluejack26_1.wakord.ui.feature.explore.SongListRow
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakColors
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.jakarta
import edu.bluejack26_1.wakord.ui.theme.spaceGrotesk
import kotlin.math.roundToInt

private const val PREVIEW_SONGS = 3

private fun chordIdentity(symbol: String): Pair<Int, Set<Int>>? =
    Chord.parse(symbol)?.let { NoteNames.pitchClass(it.root) to ChordTheory.intervals(it.suffix).take(3).toSet() }

@Composable
fun AnalysisResultScreen(
    state: AnalyzeUiState,
    onAction: (AnalyzeAction) -> Unit,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onOpenChord: (String) -> Unit,
    onOpenSong: (String) -> Unit,
    onOpenProgression: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = WakTheme.colors
    val result = state.result ?: return
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var dismissedSaved by rememberSaveable(state.savedProgressionId) { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        BackgroundArt(R.drawable.bg_analyze, height = 520.dp)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                GlassIconButton(painterResource(R.drawable.ic_back), stringResource(R.string.back), onBack, Modifier.align(Alignment.CenterStart))
                Text(stringResource(R.string.result_title), style = spaceGrotesk(14.sp), color = c.text)
                GlassIconButton(painterResource(R.drawable.ic_share), stringResource(R.string.result_share_cd), onShare, Modifier.align(Alignment.CenterEnd))
            }
            Column(Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.result_detected, keyLabel(result.key), (result.confidence * 100).roundToInt()).uppercase(),
                    style = spaceGrotesk(11.sp).copy(letterSpacing = 1.1.sp),
                    color = c.text3,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    pluralStringResource(R.plurals.result_chords_found, result.uniqueChords.size, result.uniqueChords.size),
                    style = spaceGrotesk(34.sp),
                    color = c.text,
                )
                if (state.usedFallback) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.result_fallback),
                        style = jakarta(13.sp),
                        color = c.peachText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(alertContainer(c), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                ResultCard(
                    state = state,
                    result = result,
                    onOpenChord = onOpenChord,
                    onPlay = { onAction(AnalyzeAction.PlayResult) },
                    onSave = { showSaveDialog = true },
                )
            }
            SongsSection(result, state.recommendations.songs, onOpenSong)
            ArtistsSection(state.recommendations.artists, onOpenArtist)
            ProgressionsSection(state.recommendations.progressions, onOpenProgression)
        }
    }

    if (showSaveDialog) {
        SaveDialog(
            isSaving = state.isSaving,
            onSave = { title -> onAction(AnalyzeAction.SaveAsProgression(title)) },
            onDismiss = { showSaveDialog = false },
        )
    }
    val savedId = state.savedProgressionId
    LaunchedEffect(savedId) {
        if (savedId != null) showSaveDialog = false
    }
    if (savedId != null && !dismissedSaved) {
        WakDialog(
            title = stringResource(R.string.result_saved_title),
            message = stringResource(R.string.result_saved_message),
            primaryText = stringResource(R.string.result_open),
            onPrimary = {
                dismissedSaved = true
                onOpenProgression(savedId)
            },
            onDismissRequest = { dismissedSaved = true },
            secondaryText = stringResource(R.string.not_now),
            icon = painterResource(R.drawable.ic_check_circle),
        )
    }
}

private val timelineColors: List<(WakColors) -> Color> = listOf({ it.primary }, { it.lavender }, { it.peach }, { it.primary2 })

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultCard(
    state: AnalyzeUiState,
    result: AnalysisResult,
    onOpenChord: (String) -> Unit,
    onPlay: () -> Unit,
    onSave: () -> Unit,
) {
    val c = WakTheme.colors
    val playingSymbol = state.playingIndex?.let { state.chordTrack.getOrNull(it)?.symbol }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = glassColor(c),
        border = glassBorder(c),
    ) {
        Column(Modifier.padding(17.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                result.uniqueChords.forEach { symbol ->
                    val active = symbol == playingSymbol
                    Surface(
                        onClick = { onOpenChord(symbol) },
                        modifier = Modifier
                            .widthIn(min = 64.dp)
                            .height(64.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = if (active) c.primary else c.s3,
                        border = BorderStroke(1.dp, if (active) c.primary else c.text2.copy(alpha = 0.10f)),
                    ) {
                        Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                            Text(prettyChord(symbol), style = spaceGrotesk(20.sp), color = if (active) c.onPrimary else c.text)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Timeline(result, state)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth()) {
                Text(formatClock(0), style = spaceGrotesk(12.sp), color = c.text3, modifier = Modifier.weight(1f))
                Text(formatClock(result.durationMs), style = spaceGrotesk(12.sp), color = c.text3)
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallButton(
                    text = stringResource(if (state.isPlayingResult) R.string.result_stop else R.string.result_play),
                    icon = if (state.isPlayingResult) R.drawable.ic_stop else R.drawable.ic_play,
                    primary = true,
                    onClick = onPlay,
                )
                val saved = state.savedProgressionId != null
                SmallButton(
                    text = stringResource(if (saved) R.string.result_saved else R.string.result_save),
                    icon = if (saved) R.drawable.ic_check else R.drawable.ic_share,
                    primary = false,
                    onClick = onSave,
                    enabled = !saved && state.chordTrack.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun Timeline(result: AnalysisResult, state: AnalyzeUiState) {
    val c = WakTheme.colors
    val total = result.durationMs.coerceAtLeast(1)
    val description = stringResource(R.string.result_timeline_cd)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .semantics { contentDescription = description },
    ) {
        Row(Modifier.fillMaxSize()) {
            result.chords.forEachIndexed { i, chord ->
                val weight = ((chord.endMs - chord.startMs).toFloat() / total).coerceAtLeast(0.01f)
                Box(
                    Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .background(timelineColors[i % timelineColors.size](c), RoundedCornerShape(6.dp)),
                )
            }
        }
        val index = state.playingIndex
        if (index != null && state.chordTrack.isNotEmpty()) {
            val fraction = (index + 0.5f) / state.chordTrack.size
            val x = maxWidth * fraction
            Box(
                Modifier
                    .offset { IntOffset(x.roundToPx(), 0) }
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(c.text, CircleShape),
            )
        }
    }
}

@Composable
private fun SmallButton(text: String, icon: Int, primary: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = WakTheme.colors
    val content = when {
        primary -> c.onPrimary
        enabled -> c.text
        else -> c.text3
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (primary) c.primary else c.s3,
        border = if (primary) null else BorderStroke(1.dp, c.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, style = jakarta(14.sp), color = content, maxLines = 1)
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String? = null, onAction: () -> Unit = {}) {
    val c = WakTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 30.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = spaceGrotesk(20.sp), color = c.text, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(
                action,
                style = jakarta(13.sp),
                color = c.text,
                modifier = Modifier
                    .clickable(onClick = onAction)
                    .padding(12.dp),
            )
        }
    }
}

@Composable
private fun SongsSection(result: AnalysisResult, songs: List<Song>, onOpenSong: (String) -> Unit) {
    val c = WakTheme.colors
    var expanded by rememberSaveable { mutableStateOf(false) }
    SectionHeader(
        title = stringResource(R.string.result_songs),
        action = if (songs.size > PREVIEW_SONGS) stringResource(if (expanded) R.string.see_less else R.string.see_all) else null,
        onAction = { expanded = !expanded },
    )
    if (songs.isEmpty()) {
        Text(stringResource(R.string.result_songs_empty), style = jakarta(13.sp), color = c.text3, modifier = Modifier.padding(horizontal = 20.dp))
        return
    }
    val detected = result.uniqueChords.mapNotNull(::chordIdentity).toSet()
    (if (expanded) songs else songs.take(PREVIEW_SONGS)).forEach { song ->
        val songChords = AnalysisInsights.songChords(song).mapNotNull(::chordIdentity).toSet()
        val matched = detected.count { it in songChords }
        SongListRow(
            song = song,
            onClick = { onOpenSong(song.id) },
            subtitle = if (matched == detected.size) {
                stringResource(R.string.result_full_match, song.artist)
            } else {
                pluralStringResource(R.plurals.result_partial_match, detected.size, song.artist, matched, detected.size)
            },
        )
    }
}

@Composable
private fun ArtistsSection(artists: List<String>, onOpen: (String) -> Unit) {
    val c = WakTheme.colors
    if (artists.isEmpty()) return
    SectionHeader(stringResource(R.string.result_artists))
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        artists.forEach { name ->
            val description = stringResource(R.string.result_search_artist_cd, name)
            Column(
                modifier = Modifier
                    .width(76.dp)
                    .clickable { onOpen(name) }
                    .semantics(mergeDescendants = true) { contentDescription = description },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                InitialsAvatar(name = name, seed = name, size = 68.dp, fontSize = 20.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    name,
                    style = jakarta(12.sp),
                    color = c.text2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProgressionsSection(progressions: List<ChordProgression>, onOpen: (String) -> Unit) {
    val c = WakTheme.colors
    if (progressions.isEmpty()) return
    SectionHeader(stringResource(R.string.result_progressions))
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        progressions.forEach { progression ->
            Surface(
                onClick = { onOpen(progression.id) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = c.s2,
                border = BorderStroke(1.dp, c.text2.copy(alpha = 0.10f)),
            ) {
                Row(Modifier.padding(horizontal = 17.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(progression.title, style = spaceGrotesk(15.sp), color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), maxLines = 1) {
                            progression.chords.forEach { chord ->
                                Box(
                                    modifier = Modifier
                                        .height(34.dp)
                                        .background(c.s3, RoundedCornerShape(14.dp))
                                        .border(1.dp, c.text2.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
                                        .padding(horizontal = 11.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(prettyChord(chord), style = spaceGrotesk(13.sp), color = c.text, maxLines = 1)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(c.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_play),
                            contentDescription = stringResource(R.string.result_open_progression_cd, progression.title),
                            tint = c.onPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveDialog(isSaving: Boolean, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    val c = WakTheme.colors
    var title by rememberSaveable { mutableStateOf("") }
    Dialog(onDismissRequest = { if (!isSaving) onDismiss() }) {
        Surface(
            modifier = Modifier.widthIn(max = 326.dp),
            shape = RoundedCornerShape(30.dp),
            color = c.s2,
            border = BorderStroke(1.dp, c.text2.copy(alpha = 0.10f)),
        ) {
            Column(Modifier.padding(start = 23.dp, end = 23.dp, top = 27.dp, bottom = 21.dp)) {
                Text(stringResource(R.string.result_save_title), style = spaceGrotesk(20.sp), color = c.text)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.result_save_message), style = jakarta(14.sp), color = c.text2)
                Spacer(Modifier.height(16.dp))
                WakTextField(
                    value = title,
                    onValueChange = { title = it.take(60) },
                    label = stringResource(R.string.result_save_label),
                    placeholder = stringResource(R.string.result_save_placeholder),
                    onImeAction = { if (title.isNotBlank()) onSave(title.trim()) },
                    modifier = Modifier.heightIn(min = 56.dp),
                )
                Spacer(Modifier.height(20.dp))
                WakButton(
                    text = stringResource(R.string.result_save),
                    onClick = { onSave(title.trim()) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = title.isNotBlank(),
                    loading = isSaving,
                )
                Spacer(Modifier.height(8.dp))
                WakButton(
                    text = stringResource(R.string.cancel),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    variant = WakButtonVariant.Ghost,
                    enabled = !isSaving,
                )
            }
        }
    }
}

private val previewSongs = listOf(
    Song("golden-hour-glow", "Golden Hour Glow", "Koro Acoustic Trio", "G", difficulty = Difficulty.EASY),
    Song("midnight-waves", "Midnight Waves", "Neon Mirage", "E", difficulty = Difficulty.INTERMEDIATE),
)

@Preview(name = "Analysis result", heightDp = 1300)
@Composable
private fun AnalysisResultPreview() {
    WAKordTheme(darkTheme = true) {
        AnalysisResultScreen(
            state = AnalyzeUiState(
                result = previewAnalysis,
                recommendations = AnalysisRecommendations(
                    songs = previewSongs,
                    artists = listOf("Koro Trio", "Sora Lane", "Neon Mirage", "Aria Chen"),
                    progressions = listOf(
                        ChordProgression("campfire", "Campfire classic", "G", 96, "Folk", Difficulty.EASY, listOf("G", "Em", "C", "D"), "u", "User"),
                    ),
                ),
                isPlayingResult = true,
                playingIndex = 2,
                chordTrack = listOf(),
            ),
            onAction = {},
            onBack = {},
            onShare = {},
            onOpenChord = {},
            onOpenSong = {},
            onOpenProgression = {},
            onOpenArtist = {},
        )
    }
}
