package edu.bluejack26_1.wakord.data.repository

import edu.bluejack26_1.wakord.domain.repository.ProfileRepository
import edu.bluejack26_1.wakord.domain.repository.UserPhotoRepository
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class CachedUserPhotoRepository(
    private val profileRepository: ProfileRepository,
    private val scope: CoroutineScope,
) : UserPhotoRepository {
    private val photos = ConcurrentHashMap<String, StateFlow<String?>>()

    override fun photo(uid: String): Flow<String?> = photos.getOrPut(uid) {
        profileRepository.user(uid)
            .map { it?.photoUrl }
            .catch { emit(null) }
            .stateIn(scope, SharingStarted.WhileSubscribed(STOP_MS), null)
    }

    private companion object {
        const val STOP_MS = 5_000L
    }
}
