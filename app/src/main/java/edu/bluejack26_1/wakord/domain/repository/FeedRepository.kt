package edu.bluejack26_1.wakord.domain.repository

import edu.bluejack26_1.wakord.domain.model.FeedPage
import edu.bluejack26_1.wakord.domain.model.Song
import edu.bluejack26_1.wakord.domain.model.User
import edu.bluejack26_1.wakord.domain.model.UserActivity
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    suspend fun songs(): Result<List<Song>>
    suspend fun progressionsPage(before: Long?, pageSize: Int): Result<FeedPage>
    suspend fun commentCounts(ids: List<String>): Result<Map<String, Int>>
    suspend fun activity(): Result<UserActivity>
}

interface UserRepository {
    fun currentUser(): Flow<User?>
}
