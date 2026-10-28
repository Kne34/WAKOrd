package edu.bluejack26_1.wakord.data.sync

import app.cash.turbine.test
import edu.bluejack26_1.wakord.core.error.AppError
import edu.bluejack26_1.wakord.data.local.BackupDao
import edu.bluejack26_1.wakord.data.local.BackupNodeEntity
import edu.bluejack26_1.wakord.data.local.PendingWriteEntity
import edu.bluejack26_1.wakord.data.remote.DataNode
import edu.bluejack26_1.wakord.data.remote.NodeOrder
import edu.bluejack26_1.wakord.data.remote.NodeQuery
import edu.bluejack26_1.wakord.data.remote.NodeTree
import edu.bluejack26_1.wakord.data.remote.RemoteDatabase
import edu.bluejack26_1.wakord.data.remote.long
import edu.bluejack26_1.wakord.data.remote.string
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FirebaseBackupTest {
    private class MemoryDao : BackupDao {
        val nodes = MutableStateFlow<Map<String, BackupNodeEntity>>(emptyMap())
        val writes = MutableStateFlow<List<PendingWriteEntity>>(emptyList())
        private var nextId = 1L

        override suspend fun nodes(): List<BackupNodeEntity> = nodes.value.values.toList()

        override fun observeNodes(): Flow<List<BackupNodeEntity>> = nodes.map { it.values.toList() }

        override suspend fun upsertNodes(nodes: List<BackupNodeEntity>) = this.nodes.update { map -> map + nodes.associateBy { it.path } }

        override suspend fun deleteNodes(paths: List<String>) = nodes.update { map -> map - paths.toSet() }

        override suspend fun enqueue(write: PendingWriteEntity) = writes.update { it + write.copy(id = nextId++) }

        override suspend fun pending(uid: String): List<PendingWriteEntity> = writes.value.filter { it.ownerUid == uid }

        override fun observePendingCount(uid: String): Flow<Int> = writes.map { list -> list.count { it.ownerUid == uid } }

        override suspend fun deletePending(id: Long) = writes.update { list -> list.filterNot { it.id == id } }
    }

    private class FakeRemote(initial: Map<String, Any?>) : RemoteDatabase {
        val tree = MutableStateFlow<Any?>(NodeTree.normalize(initial))
        var reachable = true
        var now = 9_000L
        val applied = mutableListOf<Map<String, Any?>>()
        private var keys = 0

        override suspend fun get(query: NodeQuery): DataNode {
            if (!reachable) throw AppError.Network
            return query.applyTo(nodeAt(tree.value, query.path))
        }

        override fun observe(query: NodeQuery): Flow<DataNode> = tree.map { query.applyTo(nodeAt(it, query.path)) }

        override suspend fun update(updates: Map<String, Any?>) {
            if (!reachable) throw AppError.Network
            applied += updates
            updates.forEach { (path, value) ->
                val segments = NodeTree.segments(path)
                tree.update { root -> NodeTree.set(root, segments, NodeTree.resolve(value, NodeTree.get(root, segments), now)) }
            }
        }

        override fun newKey(path: String): String = "k${++keys}"

        private fun nodeAt(root: Any?, path: String): DataNode {
            val segments = NodeTree.segments(path)
            return DataNode(segments.lastOrNull(), NodeTree.get(root, segments))
        }
    }

    private val dao = MemoryDao()
    private val online = MutableStateFlow(true)
    private var scheduled = 0
    private val remote = FakeRemote(
        mapOf(
            "songs" to mapOf(
                "s1" to mapOf("title" to "Midnight Waves", "likeCount" to 2),
                "s2" to mapOf("title" to "Golden Hour Glow", "likeCount" to 5),
            ),
            "progressions" to mapOf(
                "p1" to mapOf("title" to "One", "createdAt" to 100),
                "p2" to mapOf("title" to "Two", "createdAt" to 200),
                "p3" to mapOf("title" to "Three", "createdAt" to 300),
            ),
        ),
    )
    private val backup = FirebaseBackup(remote, dao, online, flowOf("me"), { scheduled++ }, clock = { 5_000L })

    @Test
    fun `data read online is backed up to the local database`() = runTest {
        backup.get("songs")

        online.value = false
        val song = backup.get("songs/s2")

        assertEquals("Golden Hour Glow", song.string("title"))
        assertEquals(listOf("songs"), dao.nodes.value.keys.toList())
    }

    @Test
    fun `reading offline without a backup fails with a network error`() = runTest {
        online.value = false
        try {
            backup.get("songs")
            fail("Expected a network error")
        } catch (e: AppError) {
            assertEquals(AppError.Network, e)
        }
    }

    @Test
    fun `an unreachable server falls back to the backup`() = runTest {
        backup.get("songs/s1")
        remote.reachable = false

        assertEquals("Midnight Waves", backup.get("songs/s1").string("title"))
    }

    @Test
    fun `offline writes show locally and wait in the queue`() = runTest {
        backup.get("songs")
        online.value = false

        backup.update(
            mapOf(
                "songs/s1/likeCount" to mapOf(".sv" to mapOf("increment" to 1L)),
                "userLikes/me/s1" to mapOf(".sv" to "timestamp"),
            ),
        )

        assertEquals(3L, backup.get("songs/s1").long("likeCount"))
        assertEquals(5_000L, backup.get("userLikes/me/s1").asLong())
        assertEquals(1, backup.pendingCount.first())
        assertEquals(1, scheduled)
        assertTrue(remote.applied.isEmpty())
    }

    @Test
    fun `queued writes reach Firebase in order once back online`() = runTest {
        online.value = false
        backup.update(mapOf("comments/s1/k1" to mapOf("text" to "first", "createdAt" to mapOf(".sv" to "timestamp"))))
        backup.update(mapOf("comments/s1/k1/text" to "edited"))

        online.value = true
        val left = backup.flush()

        assertEquals(0, left)
        assertEquals(0, backup.pendingCount.first())
        assertEquals(2, remote.applied.size)
        val comment = DataNode("k1", NodeTree.get(remote.tree.value, listOf("comments", "s1", "k1")))
        assertEquals("edited", comment.string("text"))
        assertEquals(9_000L, comment.long("createdAt"))
    }

    @Test
    fun `flush keeps the queue while the server is unreachable`() = runTest {
        online.value = false
        backup.update(mapOf("follows/me/u2" to true))
        online.value = true
        remote.reachable = false

        assertEquals(1, backup.flush())
        assertEquals(1, backup.pendingCount.first())
    }

    @Test
    fun `filtered queries are merged and replayed offline`() = runTest {
        val latest = NodeQuery("progressions", NodeOrder.Child("createdAt"), limitToLast = 2)
        val online = backup.get(latest)
        this@FirebaseBackupTest.online.value = false

        val offline = backup.get(latest)
        val older = backup.get(NodeQuery("progressions", NodeOrder.Child("createdAt"), endBefore = 200.0, limitToLast = 2))

        assertEquals(listOf("p2", "p3"), online.children.map { it.key })
        assertEquals(listOf("p2", "p3"), offline.children.map { it.key })
        assertTrue(older.children.isEmpty())
    }

    @Test
    fun `a full read replaces data removed on the server`() = runTest {
        backup.get("songs")
        remote.tree.update { NodeTree.set(it, listOf("songs", "s1"), null) }
        backup.get("songs")
        online.value = false

        assertFalse(backup.get("songs/s1").exists())
        assertTrue(backup.get("songs/s2").exists())
    }

    @Test
    fun `observing switches to the backup when the phone goes offline`() = runTest {
        backup.observe("songs/s1").test {
            assertEquals("Midnight Waves", awaitItem().string("title"))
            online.value = false
            backup.update(mapOf("songs/s1/title" to "Renamed offline"))
            assertEquals("Renamed offline", expectMostRecentItem().string("title"))
            cancelAndIgnoreRemainingEvents()
        }
    }
}
