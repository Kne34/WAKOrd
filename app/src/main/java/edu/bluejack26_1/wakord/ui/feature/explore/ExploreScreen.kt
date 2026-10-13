package edu.bluejack26_1.wakord.ui.feature.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.core.music.VoicingGenerator
import edu.bluejack26_1.wakord.domain.model.ArtistResult
import edu.bluejack26_1.wakord.domain.model.ChordProgression
import edu.bluejack26_1.wakord.domain.model.Difficulty
import edu.bluejack26_1.wakord.domain.model.LyricMatch
import edu.bluejack26_1.wakord.domain.model.SearchResults
import edu.bluejack26_1.wakord.domain.model.Song
import edu.bluejack26_1.wakord.ui.components.ChordCard
import edu.bluejack26_1.wakord.ui.components.ChordSheet
import edu.bluejack26_1.wakord.ui.components.ErrorState
import edu.bluejack26_1.wakord.ui.components.GlassIconButton
import edu.bluejack26_1.wakord.ui.components.InitialsAvatar
import edu.bluejack26_1.wakord.ui.components.NotificationBell
import edu.bluejack26_1.wakord.ui.components.levelLabel
import edu.bluejack26_1.wakord.ui.components.prettyChord
import edu.bluejack26_1.wakord.ui.feature.chord.ChordSheetAction
import edu.bluejack26_1.wakord.ui.feature.chord.ChordSheetUiState
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.jakarta
import edu.bluejack26_1.wakord.ui.theme.spaceGrotesk

@Composable
fun ExploreScreen(
    state: ExploreUiState,
    onAction: (ExploreAction) -> Unit,
    chordSheet: ChordSheetUiState,
    onChordAction: (ChordSheetAction) -> Unit,
    onOpenSong: (String) -> Unit,
    onOpenProgression: (String) -> Unit,
    onOpenSection: (ExploreSection) -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier,
    unreadNotifications: Int = 0,
) {
    val c = WakTheme.colors
    var showFilters by rememberSaveable { mutableStateOf(true) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding(),
    ) {
        AnimatedVisibility(visible = !state.showResults) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.explore_title), style = spaceGrotesk(26.sp), color = c.text, modifier = Modifier.weight(1f))
                NotificationBell(unreadCount = unreadNotifications, onClick = onOpenNotifications)
            }
        }
        SearchRow(
            query = state.query,
            searching = state.showResults,
            onQueryChange = { onAction(ExploreAction.QueryChanged(it)) },
            onClear = { onAction(ExploreAction.ClearQuery) },
            onToggleFilters = { showFilters = !showFilters },
        )
        if (state.showResults) {
            SearchResultsContent(state, onAction, onOpenSong, onOpenProgression, onOpenChord = { onChordAction(ChordSheetAction.Open(it)) })
        } else {
            BrowseContent(state, onAction, showFilters, onOpenSong, onOpenProgression, onOpenSection)
        }
    }

    val symbol = chordSheet.symbol
    if (symbol != null) {
        ChordSheet(
            symbol = symbol,
            voicings = chordSheet.voicings,
            selectedVoicing = chordSheet.selectedVoicing,
            isSaved = chordSheet.isSaved,
            onSelectVoicing = { onChordAction(ChordSheetAction.SelectVoicing(it)) },
            onPreview = { onChordAction(ChordSheetAction.Preview) },
            onToggleSaved = { onChordAction(ChordSheetAction.ToggleSaved) },
            onClose = { onChordAction(ChordSheetAction.Close) },
        )
    }
}

@Composable
private fun SearchRow(
    query: String,
    searching: Boolean,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onToggleFilters: () -> Unit,
) {
    val c = WakTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(visible = searching) {
            Row {
                GlassIconButton(painterResource(R.drawable.ic_back), stringResource(R.string.back), onClear)
                Spacer(Modifier.width(10.dp))
            }
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .background(c.s2, shape)
                .border(1.dp, if (searching) c.primary else c.outline, shape)
                .padding(start = 17.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_search), contentDescription = null, tint = if (searching) c.primaryText else c.text3, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(stringResource(R.string.explore_search_placeholder), style = jakarta(15.sp), color = c.text3, maxLines = 1)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = jakarta(15.sp).copy(color = c.text),
                    cursorBrush = SolidColor(c.primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotEmpty()) {
                Icon(
                    painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.explore_clear_search),
                    tint = c.text2,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onClear)
                        .padding(9.dp),
                )
            }
        }
        AnimatedVisibility(visible = !searching) {
            Row {
                Spacer(Modifier.width(10.dp))
                Surface(
                    onClick = onToggleFilters,
                    modifier = Modifier.size(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = c.s3,
                    border = BorderStroke(1.dp, c.line),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_filter), contentDescription = stringResource(R.string.explore_filters), tint = c.text, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit, height: Dp, filled: Boolean) {
    val c = WakTheme.colors
    Surface(
        onClick = onClick,
        modifier = Modifier
            .height(height)
            .semantics { this.selected = selected },
        shape = CircleShape,
        color = when {
            selected -> c.primary
            filled -> c.s3
            else -> Color.Transparent
        },
        border = BorderStroke(1.dp, if (selected) c.primary else if (filled) c.line else c.outline),
    ) {
        Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(label, style = jakarta(16.sp), color = if (selected) c.onPrimary else c.text, maxLines = 1)
        }
    }
}

@Composable
private fun Filters(state: ExploreUiState, onAction: (ExploreAction) -> Unit) {
    val c = WakTheme.colors
    Column(Modifier.padding(top = 12.dp)) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(stringResource(R.string.explore_all), state.selectedGenre == null, { onAction(ExploreAction.SelectGenre(null)) }, 38.dp, filled = false)
            }
            items(state.genres) { genre ->
                FilterChip(genre, state.selectedGenre == genre, { onAction(ExploreAction.SelectGenre(genre)) }, 38.dp, filled = false)
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item {
                Text(stringResource(R.string.explore_level), style = spaceGrotesk(11.sp, FontWeight.Medium), color = c.text3, modifier = Modifier.padding(end = 4.dp))
            }
            item {
                FilterChip(stringResource(R.string.explore_any), state.selectedLevel == null, { onAction(ExploreAction.SelectLevel(null)) }, 34.dp, filled = true)
            }
            items(Difficulty.entries) { level ->
                FilterChip(levelLabel(level), state.selectedLevel == level, { onAction(ExploreAction.SelectLevel(level)) }, 34.dp, filled = true)
            }
        }
    }
}

@Composable
private fun BrowseContent(
    state: ExploreUiState,
    onAction: (ExploreAction) -> Unit,
    showFilters: Boolean,
    onOpenSong: (String) -> Unit,
    onOpenProgression: (String) -> Unit,
    onOpenSection: (ExploreSection) -> Unit,
) {
    val c = WakTheme.colors
    val listState = rememberLazyListState()
    val current by rememberUpdatedState(state)
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 2
        }.collect { nearEnd ->
            if (nearEnd && current.topThisWeek.isNotEmpty()) onAction(ExploreAction.LoadMore)
        }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        if (showFilters) {
            item(key = "filters") { Filters(state, onAction) }
        }
        val nothingLoaded = state.trendingSongs.isEmpty() && state.trendingProgressions.isEmpty() && state.topThisWeek.isEmpty()
        if (state.loadError != null && nothingLoaded && !state.isLoading) {
            item(key = "error") {
                ErrorState(stringResource(state.loadError), { onAction(ExploreAction.Refresh) }, Modifier.padding(top = 40.dp))
            }
            return@LazyColumn
        }
        if (state.isLoading && nothingLoaded) {
            item(key = "loading") {
                Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = c.primary)
                }
            }
            return@LazyColumn
        }
        item(key = "trending-songs") {
            Column(Modifier.padding(top = 26.dp)) {
                SectionHeader(
                    title = ExploreSection.TRENDING_SONGS.title(),
                    onSeeAll = if (state.trendingSongs.isNotEmpty()) {
                        { onOpenSection(ExploreSection.TRENDING_SONGS) }
                    } else {
                        null
                    },
                )
                Spacer(Modifier.height(12.dp))
                if (state.trendingSongs.isEmpty()) {
                    SectionNote(stringResource(R.string.explore_section_empty))
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.trendingSongs.take(ExploreViewModel.SECTION_SIZE), key = { it.id }) { song ->
                            TrendingTile(song, onClick = { onOpenSong(song.id) })
                        }
                    }
                }
            }
        }
        item(key = "trending-progressions") {
            Column(Modifier.padding(top = 30.dp)) {
                SectionHeader(
                    title = ExploreSection.TRENDING_PROGRESSIONS.title(),
                    onSeeAll = if (state.trendingProgressions.isNotEmpty()) {
                        { onOpenSection(ExploreSection.TRENDING_PROGRESSIONS) }
                    } else {
                        null
                    },
                )
                Spacer(Modifier.height(12.dp))
                if (state.trendingProgressions.isEmpty()) {
                    SectionNote(stringResource(R.string.explore_section_empty))
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.trendingProgressions.take(ExploreViewModel.SECTION_SIZE), key = { it.id }) { progression ->
                            ProgressionTile(progression, onClick = { onOpenProgression(progression.id) }, modifier = Modifier.width(250.dp))
                        }
                    }
                }
            }
        }
        item(key = "top-header") {
            SectionHeader(
                title = ExploreSection.TOP_SONGS.title(),
                onSeeAll = if (state.topThisWeek.isNotEmpty()) {
                    { onOpenSection(ExploreSection.TOP_SONGS) }
                } else {
                    null
                },
                modifier = Modifier.padding(top = 30.dp, bottom = 8.dp),
            )
        }
        if (state.topThisWeek.isEmpty()) {
            item(key = "top-empty") { SectionNote(stringResource(R.string.explore_top_empty)) }
        }
        itemsIndexed(state.topThisWeek, key = { _, song -> "top-${song.id}" }) { index, song ->
            SongListRow(song, onClick = { onOpenSong(song.id) }, rank = index + 1)
        }
        if (state.isLoadingMore) {
            item(key = "loading-more") { LoadingMore() }
        }
    }
}

private enum class ResultsView { LOADING, EMPTY, LIST }

@Composable
private fun SearchResultsContent(
    state: ExploreUiState,
    onAction: (ExploreAction) -> Unit,
    onOpenSong: (String) -> Unit,
    onOpenProgression: (String) -> Unit,
    onOpenChord: (String) -> Unit,
) {
    val c = WakTheme.colors
    Column(Modifier.fillMaxSize()) {
        Tabs(state, onSelect = { onAction(ExploreAction.SelectTab(it)) })
        val view = when {
            state.isSearching && state.results.isEmpty -> ResultsView.LOADING
            state.results.isEmpty -> ResultsView.EMPTY
            else -> ResultsView.LIST
        }
        when (view) {
            ResultsView.LOADING -> Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.primary)
            }
            ResultsView.EMPTY -> NoResults(state.query, onBack = { onAction(ExploreAction.ClearQuery) })
            ResultsView.LIST -> when (state.selectedTab) {
                SearchTab.SONGS -> SongResults(state.query, state.results, onOpenSong)
                SearchTab.CHORDS -> ChordResults(state.results, onOpenChord, onOpenProgression)
                SearchTab.ARTISTS -> ArtistResults(state.results.artists) { artist ->
                    onAction(ExploreAction.QueryChanged(artist.name))
                    onAction(ExploreAction.SelectTab(SearchTab.SONGS))
                }
            }
        }
    }
}

@Composable
private fun Tabs(state: ExploreUiState, onSelect: (SearchTab) -> Unit) {
    val c = WakTheme.colors
    Column(Modifier.padding(top = 8.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            SearchTab.entries.forEach { tab ->
                val selected = tab == state.selectedTab
                val label = when (tab) {
                    SearchTab.SONGS -> stringResource(R.string.search_tab_songs, state.songCount)
                    SearchTab.CHORDS -> stringResource(R.string.search_tab_chords, state.chordCount)
                    SearchTab.ARTISTS -> stringResource(R.string.search_tab_artists, state.artistCount)
                }
                Column(
                    modifier = Modifier
                        .clickable { onSelect(tab) }
                        .semantics {
                            role = Role.Tab
                            this.selected = selected
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        label,
                        style = jakarta(14.sp, if (selected) FontWeight.SemiBold else FontWeight.Normal),
                        color = if (selected) c.text else c.text3,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                    Box(
                        Modifier
                            .height(2.dp)
                            .width(if (selected) 56.dp else 0.dp)
                            .background(c.primary, RoundedCornerShape(2.dp)),
                    )
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(c.line),
        )
    }
}

@Composable
private fun SongResults(query: String, results: SearchResults, onOpenSong: (String) -> Unit) {
    val lyricOnly = results.lyricMatches.filter { match -> results.songs.none { it.id == match.song.id } }
    if (results.songs.isEmpty() && lyricOnly.isEmpty()) {
        TabEmpty()
        return
    }
    LazyColumn(contentPadding = PaddingValues(vertical = 10.dp)) {
        items(results.songs, key = { "s-${it.id}" }) { song ->
            SongListRow(
                song = song,
                onClick = { onOpenSong(song.id) },
                highlightTitle = song.title.contains(query.trim(), ignoreCase = true),
            )
        }
        items(lyricOnly, key = { "l-${it.song.id}" }) { match: LyricMatch ->
            SongListRow(
                song = match.song,
                onClick = { onOpenSong(match.song.id) },
                subtitle = stringResource(R.string.search_lyric_match, match.snippet),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChordResults(results: SearchResults, onOpenChord: (String) -> Unit, onOpenProgression: (String) -> Unit) {
    val c = WakTheme.colors
    if (results.chords.isEmpty() && results.progressions.isEmpty()) {
        TabEmpty()
        return
    }
    LazyColumn(contentPadding = PaddingValues(vertical = 14.dp)) {
        if (results.chords.isNotEmpty()) {
            item(key = "chords") {
                FlowRow(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    results.chords.forEach { symbol -> ChordResult(symbol, onClick = { onOpenChord(symbol) }) }
                }
            }
        }
        if (results.progressions.isNotEmpty()) {
            item(key = "progressions-header") {
                Text(
                    stringResource(R.string.search_progressions),
                    style = spaceGrotesk(16.sp),
                    color = c.text,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp),
                )
            }
            items(results.progressions, key = { "p-${it.id}" }) { progression: ChordProgression ->
                ProgressionTile(
                    progression,
                    onClick = { onOpenProgression(progression.id) },
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 5.dp)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ChordResult(symbol: String, onClick: () -> Unit) {
    val c = WakTheme.colors
    val voicing = remember(symbol) { VoicingGenerator.voicings(symbol, limit = 1).firstOrNull() }
    if (voicing != null) {
        ChordCard(
            name = prettyChord(symbol),
            frets = voicing.frets,
            fingers = voicing.fingers,
            baseFret = voicing.baseFret,
            onClick = onClick,
        )
    } else {
        Surface(
            onClick = onClick,
            modifier = Modifier.height(44.dp),
            shape = RoundedCornerShape(14.dp),
            color = c.s3,
            border = BorderStroke(1.dp, c.line),
        ) {
            Box(Modifier.padding(horizontal = 15.dp), contentAlignment = Alignment.Center) {
                Text(prettyChord(symbol), style = spaceGrotesk(16.sp), color = c.text)
            }
        }
    }
}

@Composable
private fun ArtistResults(artists: List<ArtistResult>, onOpen: (ArtistResult) -> Unit) {
    val c = WakTheme.colors
    if (artists.isEmpty()) {
        TabEmpty()
        return
    }
    LazyColumn(contentPadding = PaddingValues(vertical = 10.dp)) {
        items(artists, key = { it.name }) { artist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(artist) }
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InitialsAvatar(artist.name, artist.name, 52.dp, fontSize = 15.sp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(artist.name, style = spaceGrotesk(16.sp), color = c.text, maxLines = 1)
                    Spacer(Modifier.height(3.dp))
                    val songs = pluralStringResource(R.plurals.search_artist_songs, artist.songCount, artist.songCount)
                    Text(
                        if (artist.genres.isEmpty()) songs else stringResource(R.string.search_artist_meta, songs, artist.genres.joinToString(", ")),
                        style = jakarta(12.sp),
                        color = c.text3,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun TabEmpty() {
    val c = WakTheme.colors
    Text(
        stringResource(R.string.search_tab_empty),
        style = jakarta(13.sp),
        color = c.text3,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 40.dp),
    )
}

@Composable
private fun NoResults(query: String, onBack: () -> Unit) {
    val c = WakTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp)
            .padding(top = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(c.s3, RoundedCornerShape(32.dp))
                .border(1.dp, c.line, RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_search), contentDescription = null, tint = c.text3, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.search_no_results_title, query.trim()), style = spaceGrotesk(20.sp), color = c.text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.search_no_results_message), style = jakarta(14.sp), color = c.text2, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Surface(
            onClick = onBack,
            modifier = Modifier.height(40.dp),
            shape = RoundedCornerShape(14.dp),
            color = c.s3,
            border = BorderStroke(1.dp, c.outline),
        ) {
            Box(Modifier.padding(horizontal = 17.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.search_back_to_explore), style = spaceGrotesk(14.sp), color = c.text)
            }
        }
    }
}

private val previewSongs = listOf(
    Song("subtle-resonance", "Subtle Resonance", "Alex Vance ft. Koro", "Eb", difficulty = Difficulty.INTERMEDIATE, likeCount = 22500, genre = "Neo-Soul"),
    Song("midnight-waves", "Midnight Waves", "Neon Mirage", "E", difficulty = Difficulty.INTERMEDIATE, likeCount = 18100, genre = "Neo-Soul"),
    Song("golden-hour-glow", "Golden Hour Glow", "Koro Acoustic Trio", "G", difficulty = Difficulty.EASY, likeCount = 9400, genre = "Acoustic"),
)

private val previewProgressions = listOf(
    ChordProgression("neo-soul-04", "Neo-Soul Progression #04", "C", 84, "Neo-Soul", Difficulty.ADVANCED, listOf("Dm9", "G13", "Cmaj9", "Am7"), "julian", "Julian Keys", playCount = 1800),
    ChordProgression("anthem", "Four-chord anthem", "C", 100, "Pop", Difficulty.EASY, listOf("C", "G", "Am", "F"), "maya", "Maya Sol", playCount = 3200),
)

@Preview(name = "Explore", heightDp = 1400)
@Composable
private fun ExplorePreview() {
    WAKordTheme(darkTheme = true) {
        ExploreScreen(
            state = ExploreUiState(
                trendingSongs = previewSongs,
                trendingProgressions = previewProgressions,
                topThisWeek = previewSongs,
                isLoading = false,
                isLoadingMore = true,
            ),
            onAction = {},
            chordSheet = ChordSheetUiState(),
            onChordAction = {},
            onOpenSong = {},
            onOpenProgression = {},
            onOpenSection = {},
            onOpenNotifications = {},
            unreadNotifications = 3,
        )
    }
}

@Preview(name = "Explore - search results", heightDp = 844)
@Composable
private fun ExploreResultsPreview() {
    WAKordTheme(darkTheme = true) {
        ExploreScreen(
            state = ExploreUiState(
                query = "midnight",
                isLoading = false,
                results = SearchResults(
                    songs = previewSongs.take(2),
                    lyricMatches = listOf(LyricMatch(previewSongs[2], "Wandering through the midnight glow")),
                    chords = listOf("Cmaj7"),
                    artists = listOf(ArtistResult("Neon Mirage", 2, listOf("Neo-Soul"))),
                ),
            ),
            onAction = {},
            chordSheet = ChordSheetUiState(),
            onChordAction = {},
            onOpenSong = {},
            onOpenProgression = {},
            onOpenSection = {},
            onOpenNotifications = {},
        )
    }
}

@Preview(name = "Explore - chords tab", heightDp = 844)
@Composable
private fun ExploreChordsPreview() {
    WAKordTheme(darkTheme = true) {
        ExploreScreen(
            state = ExploreUiState(
                query = "maj",
                isLoading = false,
                selectedTab = SearchTab.CHORDS,
                results = SearchResults(chords = listOf("Cmaj7", "Fmaj7", "Cmaj9"), progressions = previewProgressions.take(1)),
            ),
            onAction = {},
            chordSheet = ChordSheetUiState(),
            onChordAction = {},
            onOpenSong = {},
            onOpenProgression = {},
            onOpenSection = {},
            onOpenNotifications = {},
        )
    }
}

@Preview(name = "Explore - no results", heightDp = 844)
@Composable
private fun ExploreEmptyPreview() {
    WAKordTheme(darkTheme = true) {
        ExploreScreen(
            state = ExploreUiState(query = "dangdut jazz fusion", isLoading = false),
            onAction = {},
            chordSheet = ChordSheetUiState(),
            onChordAction = {},
            onOpenSong = {},
            onOpenProgression = {},
            onOpenSection = {},
            onOpenNotifications = {},
        )
    }
}

@Preview(name = "Explore - light", heightDp = 1400)
@Composable
private fun ExploreLightPreview() {
    WAKordTheme(darkTheme = false) {
        ExploreScreen(
            state = ExploreUiState(trendingSongs = previewSongs, trendingProgressions = previewProgressions, topThisWeek = previewSongs, isLoading = false),
            onAction = {},
            chordSheet = ChordSheetUiState(),
            onChordAction = {},
            onOpenSong = {},
            onOpenProgression = {},
            onOpenSection = {},
            onOpenNotifications = {},
        )
    }
}
