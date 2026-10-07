package edu.bluejack26_1.wakord.domain.repository

import edu.bluejack26_1.wakord.domain.model.Catalog
import edu.bluejack26_1.wakord.domain.model.ChordProgression
import edu.bluejack26_1.wakord.domain.model.Song

interface ExploreRepository {
    suspend fun trendingSongs(limit: Int): Result<List<Song>>
    suspend fun trendingProgressions(limit: Int): Result<List<ChordProgression>>
    suspend fun topSongsThisWeek(limit: Int): Result<List<Song>>
    suspend fun catalog(): Result<Catalog>
}
