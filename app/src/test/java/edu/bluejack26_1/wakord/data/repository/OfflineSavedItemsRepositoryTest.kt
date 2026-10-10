package edu.bluejack26_1.wakord.data.repository

import edu.bluejack26_1.wakord.core.error.AppError
import edu.bluejack26_1.wakord.data.local.SavedItemDao
import edu.bluejack26_1.wakord.data.local.SavedItemEntity
import edu.bluejack26_1.wakord.data.local.SyncStateEntity
import edu.bluejack26_1.wakord.domain.model.SavedType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineSavedItemsRepositoryTest {
    private class MemoryDao : SavedItemDao {
        val rows = MutableStateFlow<List<SavedItemEntity>>(emptyList())
        val sync = MutableStateFlow<SyncStateEntity?>(null)

        override fun observeAll(uid: String): Flow<List<SavedItemEntity>> =
            rows.map { list -> list.filter { it.ownerUid == uid }.sortedByDescending { it.savedAt } }

        override suspend fun all(uid: String) = rows.value.filter { it.ownerUid == uid }

        override suspend fun upsert(entities: List<SavedItemEntity>) = rows.update { list ->
            list.filterNot { old -> entities.any { it.ownerUid == old.ownerUid && it.type == old.type && it.itemId == old.itemId } } + entities
        }

        override suspend fun delete(uid: String, type: String, itemId: String) =
            rows.update { list -> list.filterNot { it.ownerUid == uid && it.type == type && it.itemId == itemId } }

        override fun observeSyncState(uid: String): Flow<SyncStateEntity?> = sync

        override suspend fun upsertSyncState(entity: SyncStateEntity) {
            sync.value = entity
        }
    }

    private class FakeRemote : SavedItemsRemote {
        var online = false
        val server = mutableMapOf<Pair<SavedType, String>, Long>()
        val pushes = mutableListOf<Triple<SavedType, String, Boolean>>()

        override fun currentUid(): Flow<String?> = MutableStateFlow("me")

        override suspend fun push(type: SavedType, id: String, saved: Boolean): Result<Unit> {
            if (!online) return Result.failure(AppError.Network)
            pushes += Triple(type, id, saved)
            if (saved) server[type to id] = 500L else server.remove(type to id)
            return Result.success(Unit)
        }

        override suspend fun fetch(type: SavedType): Result<List<RemoteSavedItem>> =
            if (!online) Result.failure(AppError.Network)
            else Result.success(server.filterKeys { it.first == type }.map { RemoteSavedItem(it.key.second, it.value) })

        override suspend fun details(type: SavedType, id: String) = ItemDetails("Title of $id", "Artist")
    }

    private val dao = MemoryDao()
    private val remote = FakeRemote()
    private var scheduled = 0
    private var now = 1_000L
    private val repository = OfflineSavedItemsRepository(dao, remote, { scheduled++ }, { now })

    @Test
    fun `saving offline shows right away and waits for the network`() = runTest {
        repository.setSaved(SavedType.SONG, "s1", true)
        assertTrue(repository.isSaved(SavedType.SONG, "s1").first())
        val entry = repository.entries(SavedType.SONG).first().single()
        assertTrue(entry.isPending)
        assertEquals(1, scheduled)
        assertEquals(1, repository.pendingCount.first())
    }

    @Test
    fun `sync pushes pending items and fills in their details`() = runTest {
        repository.setSaved(SavedType.SONG, "s1", true)
        remote.online = true
        now = 9_000L
        assertEquals(0, repository.syncNow().getOrThrow())
        val entry = repository.entries(SavedType.SONG).first().single()
        assertFalse(entry.isPending)
        assertEquals("Title of s1", entry.title)
        assertEquals(listOf(Triple(SavedType.SONG, "s1", true)), remote.pushes)
        assertEquals(9_000L, repository.lastSyncedAt.first())
    }

    @Test
    fun `removing offline hides the item and deletes it on sync`() = runTest {
        remote.online = true
        repository.setSaved(SavedType.CHORD, "G7", true)
        remote.online = false
        repository.setSaved(SavedType.CHORD, "G7", false)
        assertFalse(repository.isSaved(SavedType.CHORD, "G7").first())
        assertEquals(1, dao.rows.value.size)

        remote.online = true
        repository.syncNow()
        assertTrue(dao.rows.value.isEmpty())
        assertEquals(Triple(SavedType.CHORD, "G7", false), remote.pushes.last())
    }

    @Test
    fun `save then unsave before syncing never reaches the server`() = runTest {
        repository.setSaved(SavedType.PROGRESSION, "p1", true)
        repository.setSaved(SavedType.PROGRESSION, "p1", false)
        remote.online = true
        repository.syncNow()
        assertTrue(remote.pushes.isEmpty())
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test
    fun `changes from another device are pulled in`() = runTest {
        remote.online = true
        repository.setSaved(SavedType.SONG, "old", true)
        remote.server.remove(SavedType.SONG to "old")
        remote.server[SavedType.SONG to "fromTablet"] = 700L
        repository.syncNow()
        assertEquals(listOf("fromTablet"), repository.saved(SavedType.SONG).first())
    }

    @Test
    fun `saving twice does not count twice`() = runTest {
        remote.online = true
        repository.setSaved(SavedType.SONG, "s1", true)
        repository.setSaved(SavedType.SONG, "s1", true)
        assertEquals(1, remote.pushes.size)
    }
}
