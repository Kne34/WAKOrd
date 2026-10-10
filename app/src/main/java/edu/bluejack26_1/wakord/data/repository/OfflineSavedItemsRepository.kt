package edu.bluejack26_1.wakord.data.repository

import edu.bluejack26_1.wakord.core.error.AppError
import edu.bluejack26_1.wakord.data.local.SavedItemDao
import edu.bluejack26_1.wakord.data.local.SavedItemEntity
import edu.bluejack26_1.wakord.data.local.SyncState
import edu.bluejack26_1.wakord.data.local.SyncStateEntity
import edu.bluejack26_1.wakord.domain.model.SavedEntry
import edu.bluejack26_1.wakord.domain.model.SavedType
import edu.bluejack26_1.wakord.domain.repository.SavedItemsRepository
import edu.bluejack26_1.wakord.domain.repository.SavedLibraryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class RemoteSavedItem(val id: String, val savedAt: Long)

data class ItemDetails(val title: String, val subtitle: String)

interface SavedItemsRemote {
    fun currentUid(): Flow<String?>
    suspend fun push(type: SavedType, id: String, saved: Boolean): Result<Unit>
    suspend fun fetch(type: SavedType): Result<List<RemoteSavedItem>>
    suspend fun details(type: SavedType, id: String): ItemDetails
}

fun interface SyncScheduler {
    fun schedule()
}

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineSavedItemsRepository(
    private val dao: SavedItemDao,
    private val remote: SavedItemsRemote,
    private val scheduler: SyncScheduler,
    private val clock: () -> Long = System::currentTimeMillis,
) : SavedItemsRepository, SavedLibraryRepository {
    private val syncLock = Mutex()

    private val rows: Flow<List<SavedItemEntity>> = remote.currentUid().flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList()) else dao.observeAll(uid)
    }

    override fun isSaved(type: SavedType, id: String): Flow<Boolean> = visible(type).map { list -> list.any { it.itemId == id } }.distinctUntilChanged()

    override fun saved(type: SavedType): Flow<List<String>> = visible(type).map { list -> list.map { it.itemId } }

    override fun entries(type: SavedType): Flow<List<SavedEntry>> = visible(type).map { list ->
        list.map { SavedEntry(type, it.itemId, it.title.ifEmpty { it.itemId }, it.subtitle, it.savedAt, it.state != SyncState.SYNCED.name) }
    }

    override val pendingCount: Flow<Int> = rows.map { list -> list.count { it.state != SyncState.SYNCED.name } }

    override val lastSyncedAt: Flow<Long?> = remote.currentUid().flatMapLatest { uid ->
        if (uid == null) flowOf(null) else dao.observeSyncState(uid).map { it?.lastSyncedAt }
    }

    override suspend fun setSaved(type: SavedType, id: String, saved: Boolean): Result<Unit> {
        val uid = uid() ?: return Result.failure(AppError.NotLoggedIn)
        val existing = dao.all(uid).firstOrNull { it.type == type.name && it.itemId == id }
        if (saved && existing?.state == SyncState.SYNCED.name) return Result.success(Unit)
        when {
            saved -> dao.upsert(
                listOf(
                    SavedItemEntity(
                        ownerUid = uid,
                        type = type.name,
                        itemId = id,
                        title = existing?.title.orEmpty(),
                        subtitle = existing?.subtitle.orEmpty(),
                        savedAt = clock(),
                        state = if (existing?.state == SyncState.PENDING_REMOVE.name) SyncState.SYNCED.name else SyncState.PENDING_SAVE.name,
                    ),
                ),
            )
            existing == null -> return Result.success(Unit)
            existing.state == SyncState.PENDING_SAVE.name -> dao.delete(uid, type.name, id)
            else -> dao.upsert(listOf(existing.copy(state = SyncState.PENDING_REMOVE.name)))
        }
        if (pushPending(uid) > 0) scheduler.schedule()
        return Result.success(Unit)
    }

    override suspend fun syncNow(): Result<Int> = runCatching {
        val uid = uid() ?: throw AppError.NotLoggedIn
        syncLock.withLock {
            val left = pushPending(uid)
            if (left > 0) {
                scheduler.schedule()
                return@withLock left
            }
            SavedType.entries.forEach { type -> pull(uid, type) }
            dao.upsertSyncState(SyncStateEntity(uid, clock()))
            0
        }
    }

    private fun visible(type: SavedType): Flow<List<SavedItemEntity>> = rows.map { list ->
        list.filter { it.type == type.name && it.state != SyncState.PENDING_REMOVE.name }
    }

    private suspend fun uid(): String? = remote.currentUid().first()

    private suspend fun pushPending(uid: String): Int {
        val pending = dao.all(uid).filter { it.state != SyncState.SYNCED.name }
        var failed = 0
        pending.forEach { row ->
            val type = SavedType.valueOf(row.type)
            val save = row.state == SyncState.PENDING_SAVE.name
            remote.push(type, row.itemId, save)
                .onSuccess {
                    if (save) {
                        val details = if (row.title.isEmpty()) remote.details(type, row.itemId) else ItemDetails(row.title, row.subtitle)
                        dao.upsert(listOf(row.copy(state = SyncState.SYNCED.name, title = details.title, subtitle = details.subtitle)))
                    } else {
                        dao.delete(uid, row.type, row.itemId)
                    }
                }
                .onFailure { failed++ }
        }
        return failed
    }

    private suspend fun pull(uid: String, type: SavedType) {
        val remoteItems = remote.fetch(type).getOrThrow()
        val local = dao.all(uid).filter { it.type == type.name }.associateBy { it.itemId }
        val remoteIds = remoteItems.map { it.id }.toSet()
        local.values
            .filter { it.state == SyncState.SYNCED.name && it.itemId !in remoteIds }
            .forEach { dao.delete(uid, type.name, it.itemId) }
        val updates = remoteItems.mapNotNull { item ->
            val row = local[item.id]
            if (row != null && row.state != SyncState.SYNCED.name) return@mapNotNull null
            val details = if (row == null || row.title.isEmpty()) remote.details(type, item.id) else ItemDetails(row.title, row.subtitle)
            SavedItemEntity(uid, type.name, item.id, details.title, details.subtitle, item.savedAt, SyncState.SYNCED.name)
        }
        dao.upsert(updates)
    }
}
