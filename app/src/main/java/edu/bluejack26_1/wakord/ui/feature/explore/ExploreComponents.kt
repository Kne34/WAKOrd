package edu.bluejack26_1.wakord.ui.feature.explore

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.domain.model.ChordProgression
import edu.bluejack26_1.wakord.domain.model.Difficulty
import edu.bluejack26_1.wakord.domain.model.Song
import edu.bluejack26_1.wakord.ui.components.compactCount
import edu.bluejack26_1.wakord.ui.components.levelColor
import edu.bluejack26_1.wakord.ui.components.levelContainer
import edu.bluejack26_1.wakord.ui.components.levelLabel
import edu.bluejack26_1.wakord.ui.components.prettyChord
import edu.bluejack26_1.wakord.ui.feature.song.songCover
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.jakarta
import edu.bluejack26_1.wakord.ui.theme.spaceGrotesk

enum class ExploreSection { TRENDING_SONGS, TRENDING_PROGRESSIONS, TOP_SONGS }

@Composable
fun ExploreSection.title(): String = stringResource(
    when (this) {
        ExploreSection.TRENDING_SONGS -> R.string.explore_trending_now
        ExploreSection.TRENDING_PROGRESSIONS -> R.string.explore_trending_progressions
        ExploreSection.TOP_SONGS -> R.string.explore_top_week
    },
)

@Composable
fun SectionHeader(title: String, onSeeAll: (() -> Unit)?, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = spaceGrotesk(20.sp), color = c.text, modifier = Modifier.weight(1f))
        if (onSeeAll != null) {
            Text(
                stringResource(R.string.see_all),
                style = jakarta(13.sp, FontWeight.SemiBold),
                color = c.text,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onSeeAll)
                    .padding(12.dp),
            )
        }
    }
}

@Composable
fun LevelTag(difficulty: Difficulty) {
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

@Composable
fun TrendingTile(song: Song, onClick: () -> Unit) {
    val c = WakTheme.colors
    Column(
        modifier = Modifier
            .width(92.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(RoundedCornerShape(18.dp)),
        ) {
            Image(painterResource(songCover(song.id)), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .background(Color(0xFF080D1D).copy(alpha = 0.62f), CircleShape)
                    .padding(horizontal = 7.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_heart_filled), contentDescription = null, tint = c.peach, modifier = Modifier.size(10.dp))
                Spacer(Modifier.width(4.dp))
                Text(compactCount(song.likeCount), style = spaceGrotesk(10.sp), color = Color(0xFFDEE1F9))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(song.title, style = jakarta(12.sp), color = c.text2, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgressionTile(progression: ChordProgression, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier.height(170.dp),
        shape = RoundedCornerShape(22.dp),
        color = c.s2,
        border = BorderStroke(1.dp, c.text2.copy(alpha = 0.10f)),
    ) {
        Column(Modifier.padding(17.dp)) {
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
            }
            Spacer(Modifier.height(12.dp))
            Text(progression.title, style = spaceGrotesk(17.sp), color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), maxLines = 1) {
                progression.chords.forEach { chord ->
                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .background(c.s3, RoundedCornerShape(12.dp))
                            .border(1.dp, c.text2.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 11.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(prettyChord(chord), style = spaceGrotesk(14.sp), color = c.text, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.explore_progression_meta, progression.authorName, compactCount(progression.playCount)),
                style = jakarta(12.sp),
                color = c.text3,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun SongListRow(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rank: Int? = null,
    subtitle: String? = null,
    highlightTitle: Boolean = false,
) {
    val c = WakTheme.colors
    val rankCd = rank?.let { stringResource(R.string.explore_rank_cd, it, song.title) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(if (rankCd != null) Modifier.semantics(mergeDescendants = true) { contentDescription = rankCd } else Modifier)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (rank != null) {
            Text(rank.toString(), style = spaceGrotesk(16.sp), color = c.text3, modifier = Modifier.width(32.dp))
        }
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
            Text(
                song.title,
                style = spaceGrotesk(16.sp),
                color = if (highlightTitle) c.primaryText else c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                subtitle ?: stringResource(R.string.explore_song_meta, song.artist, prettyChord(song.key)),
                style = jakarta(12.sp),
                color = c.text3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        LevelTag(song.difficulty)
    }
}

@Composable
fun LoadingMore(modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(color = c.primary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.loading_more), style = jakarta(12.sp), color = c.text3)
    }
}

@Composable
fun SectionNote(text: String, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    Text(
        text,
        style = jakarta(13.sp),
        color = c.text3,
        modifier = modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .background(c.s2, RoundedCornerShape(20.dp))
            .border(1.dp, c.text2.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
            .padding(18.dp),
    )
}
