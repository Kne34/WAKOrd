package edu.bluejack26_1.wakord.data.repository

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import edu.bluejack26_1.wakord.data.remote.valueFlow
import edu.bluejack26_1.wakord.domain.model.SavedType
import edu.bluejack26_1.wakord.domain.model.SyncStatus
import edu.bluejack26_1.wakord.domain.repository.AuthRepository
import edu.bluejack26_1.wakord.domain.repository.SavedItemsRepository
import edu.bluejack26_1.wakord.domain.repository.SavedLibraryRepository
import edu.bluejack26_1.wakord.domain.repository.SettingsRepository
import edu.bluejack26_1.wakord.domain.repository.SyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseSyncRepository(
    private val database: FirebaseDatabase,
    private val authRepository: AuthRepository,
    private val savedItemsRepository: SavedItemsRepository,
    settingsRepository: SettingsRepository,
    private val library: SavedLibraryRepository,
    scope: CoroutineScope,
) : SyncRepository {
    @Volatile
    private var lastSyncedAt: Long? = null

    init {
        scope.launch {
            var mirrored: DatabaseReference? = null
            combine(authRepository.currentUserId, settingsRepository.keepOffline) { uid, keep -> uid to keep }
                .distinctUntilChanged()
                .collect { (uid, keep) ->
                    mirrored?.keepSynced(false)
                    mirrored = uid?.let { database.getReference("saves/$it") }
                    mirrored?.keepSynced(keep)
                }
        }
    }

    private val connected: Flow<Boolean> = database.getReference(".info/connected").valueFlow()
        .map { it.getValue(Boolean::class.java) == true }
        .catch { emit(false) }

    override val status: Flow<SyncStatus> = authRepository.currentUserId.flatMapLatest { uid ->
        if (uid == null) {
            flowOf(SyncStatus())
        } else {
            combine(
                connected,
                savedItemsRepository.saved(SavedType.SONG).catch { emit(emptyList()) },
                savedItemsRepository.saved(SavedType.CHORD).catch { emit(emptyList()) },
                library.pendingCount.catch { emit(0) },
                library.lastSyncedAt.catch { emit(null) },
            ) { online, songs, chords, pending, synced ->
                if (online) lastSyncedAt = System.currentTimeMillis()
                SyncStatus(
                    savedSongs = songs.size,
                    savedChords = chords.size,
                    isOnline = online,
                    lastSyncedAt = synced ?: lastSyncedAt,
                    pendingCount = pending,
                )
            }
        }
    }
}
