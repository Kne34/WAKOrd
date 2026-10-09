package edu.bluejack26_1.wakord.ui.feature.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.domain.model.ChordProgression
import edu.bluejack26_1.wakord.domain.model.Difficulty
import edu.bluejack26_1.wakord.domain.model.Song
import edu.bluejack26_1.wakord.ui.components.GlassIconButton
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.spaceGrotesk

@Composable
fun ExploreSectionScreen(
    section: ExploreSection,
    state: ExploreUiState,
    onAction: (ExploreAction) -> Unit,
    onBack: () -> Unit,
    onOpenSong: (String) -> Unit,
    onOpenProgression: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = WakTheme.colors
    val listState = rememberLazyListState()
    val current by rememberUpdatedState(state)
    if (section == ExploreSection.TOP_SONGS) {
        LaunchedEffect(listState) {
            snapshotFlow {
                val info = listState.layoutInfo
                val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                info.totalItemsCount > 0 && last >= info.totalItemsCount - 2
            }.collect { nearEnd ->
                if (nearEnd && current.topThisWeek.isNotEmpty()) onAction(ExploreAction.LoadMore)
            }
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconButton(painterResource(R.drawable.ic_back), stringResource(R.string.back), onBack)
            Spacer(Modifier.width(14.dp))
            Text(section.title(), style = spaceGrotesk(20.sp), color = c.text)
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        ) {
            when (section) {
                ExploreSection.TRENDING_SONGS -> {
                    if (state.trendingSongs.isEmpty()) item { EmptyNote() }
                    items(state.trendingSongs, key = { it.id }) { song ->
                        SongListRow(song, onClick = { onOpenSong(song.id) })
                    }
                }
                ExploreSection.TRENDING_PROGRESSIONS -> {
                    if (state.trendingProgressions.isEmpty()) item { EmptyNote() }
                    items(state.trendingProgressions, key = { it.id }) { progression ->
                        ProgressionTile(
                            progression,
                            onClick = { onOpenProgression(progression.id) },
                            modifier = Modifier
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                                .fillMaxWidth(),
                        )
                    }
                }
                ExploreSection.TOP_SONGS -> {
                    if (state.topThisWeek.isEmpty()) item { EmptyNote(stringResource(R.string.explore_top_empty)) }
                    itemsIndexed(state.topThisWeek, key = { _, song -> song.id }) { index, song ->
                        SongListRow(song, onClick = { onOpenSong(song.id) }, rank = index + 1)
                    }
                    if (state.isLoadingMore) item { LoadingMore() }
                }
            }
        }
    }
}

@Composable
private fun EmptyNote(text: String = stringResource(R.string.explore_section_empty)) {
    Box(Modifier.padding(top = 12.dp)) { SectionNote(text) }
}

@Preview(name = "See all - top songs", heightDp = 844)
@Composable
private fun ExploreSectionPreview() {
    WAKordTheme(darkTheme = true) {
        ExploreSectionScreen(
            section = ExploreSection.TOP_SONGS,
            state = ExploreUiState(
                isLoading = false,
                isLoadingMore = true,
                topThisWeek = listOf(
                    Song("harmonic-drift", "Harmonic Drift", "Neon Mirage", "A", difficulty = Difficulty.INTERMEDIATE),
                    Song("golden-hour-glow", "Golden Hour Glow", "Koro Acoustic Trio", "G", difficulty = Difficulty.EASY),
                    Song("cold-coffee", "Cold Coffee Blues", "Marcus Vance", "E", difficulty = Difficulty.ADVANCED),
                ),
            ),
            onAction = {},
            onBack = {},
            onOpenSong = {},
            onOpenProgression = {},
        )
    }
}

@Preview(name = "See all - progressions", heightDp = 844)
@Composable
private fun ExploreSectionProgressionsPreview() {
    WAKordTheme(darkTheme = true) {
        ExploreSectionScreen(
            section = ExploreSection.TRENDING_PROGRESSIONS,
            state = ExploreUiState(
                isLoading = false,
                trendingProgressions = listOf(
                    ChordProgression("neo-soul-04", "Neo-Soul Progression #04", "C", 84, "Neo-Soul", Difficulty.ADVANCED, listOf("Dm9", "G13", "Cmaj9"), "julian", "Julian Keys", playCount = 1800),
                ),
            ),
            onAction = {},
            onBack = {},
            onOpenSong = {},
            onOpenProgression = {},
        )
    }
}
