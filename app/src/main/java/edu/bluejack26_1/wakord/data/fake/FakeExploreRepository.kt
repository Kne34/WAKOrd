package edu.bluejack26_1.wakord.data.fake

import edu.bluejack26_1.wakord.core.error.AppError
import edu.bluejack26_1.wakord.domain.model.Catalog
import edu.bluejack26_1.wakord.domain.model.ChordProgression
import edu.bluejack26_1.wakord.domain.model.Song
import edu.bluejack26_1.wakord.domain.repository.ExploreRepository

class FakeExploreRepository(
    var songs: List<Song> = SampleDiscover.songs,
    var progressions: List<ChordProgression> = SampleDiscover.progressions,
    var weeklyTop: List<Song> = SampleDiscover.songs.take(2),
) : ExploreRepository {
    var offline = false
    var catalogLoads = 0

    override suspend fun trendingSongs(limit: Int): Result<List<Song>> =
        guard { songs.sortedByDescending { it.likeCount }.take(limit) }

    override suspend fun trendingProgressions(limit: Int): Result<List<ChordProgression>> =
        guard { progressions.sortedByDescending { it.likeCount }.take(limit) }

    override suspend fun topSongsThisWeek(limit: Int): Result<List<Song>> = guard { weeklyTop.take(limit) }

    override suspend fun catalog(): Result<Catalog> = guard {
        catalogLoads++
        Catalog(songs, progressions)
    }

    private fun <T> guard(block: () -> T): Result<T> =
        if (offline) Result.failure(AppError.Network) else Result.success(block())
}
