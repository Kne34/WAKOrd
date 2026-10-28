package edu.bluejack26_1.wakord.data.sync

import edu.bluejack26_1.wakord.core.error.AppError
import edu.bluejack26_1.wakord.data.local.BackupDao
import edu.bluejack26_1.wakord.data.local.BackupNodeEntity
import edu.bluejack26_1.wakord.data.local.PendingWriteEntity
import edu.bluejack26_1.wakord.data.remote.DataNode
import edu.bluejack26_1.wakord.data.remote.NodeJson
import edu.bluejack26_1.wakord.data.remote.NodeQuery
import edu.bluejack26_1.wakord.data.remote.NodeTree
import edu.bluejack26_1.wakord.data.remote.RemoteDatabase
import edu.bluejack26_1.wakord.data.remote.toAppError
import edu.bluejack26_1.wakord.data.repository.SyncScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseBackup(
    private val remote: RemoteDatabase,
    private val dao: BackupDao,
    private val isOnline: StateFlow<Boolean>,
    private val userId: Flow<String?>,
    private val scheduler: SyncScheduler,
    private val clock: () -> Long = System::currentTimeMillis,
    private val timeoutMs: Long = TIMEOUT_MS,
) {
    private val lock = Mutex()
    private val flushLock = Mutex()

    val pendingCount: Flow<Int> = userId.flatMapLatest { uid -> if (uid == null) flowOf(0) else dao.observePendingCount(uid) }

    suspend fun get(path: String): DataNode = get(NodeQuery(path))

    suspend fun get(query: NodeQuery): DataNode {
        if (isOnline.value) {
            try {
                val node = withTimeout(timeoutMs) { remote.get(query) }
                store(query, node)
                return node
            } catch (e: TimeoutCancellationException) {
                return cached(query) ?: throw AppError.Network
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return cached(query) ?: throw e.toAppError()
            }
        }
        return cached(query) ?: throw AppError.Network
    }

    fun observe(path: String): Flow<DataNode> = observe(NodeQuery(path))

    fun observe(query: NodeQuery): Flow<DataNode> = isOnline.flatMapLatest { online ->
        if (online) {
            remote.observe(query)
                .onEach { store(query, it) }
                .onStart { cached(query)?.let { emit(it) } }
                .catch { emitAll(local(query)) }
        } else {
            local(query)
        }
    }.distinctUntilChanged()

    suspend fun update(updates: Map<String, Any?>) {
        val uid = userId.first() ?: throw AppError.NotLoggedIn
        lock.withLock { edit { applyWrites(updates) } }
        if (isOnline.value) {
            try {
                withTimeout(timeoutMs) { remote.update(updates) }
                return
            } catch (e: TimeoutCancellationException) {
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val error = e.toAppError()
                if (error != AppError.Network) throw error
            }
        }
        dao.enqueue(PendingWriteEntity(ownerUid = uid, updates = NodeJson.encode(updates), createdAt = clock()))
        scheduler.schedule()
    }

    fun newKey(path: String): String = remote.newKey(path)

    suspend fun flush(): Int = flushLock.withLock {
        val uid = userId.first() ?: return 0
        val pending = dao.pending(uid)
        pending.forEachIndexed { index, write ->
            try {
                withTimeout(timeoutMs) { remote.update(decodeUpdates(write.updates)) }
            } catch (_: TimeoutCancellationException) {
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e.toAppError() == AppError.Network) return pending.size - index
            }
            dao.deletePending(write.id)
        }
        0
    }

    suspend fun backup(paths: List<String>): Int = paths.count { path -> runCatching { get(path) }.isSuccess }

    private suspend fun cached(query: NodeQuery): DataNode? {
        val (covered, value) = BackupTree(dao.nodes()).read(query.path)
        return if (covered) query.applyTo(DataNode(lastKey(query.path), value)) else null
    }

    private fun local(query: NodeQuery): Flow<DataNode> = dao.observeNodes().map { rows ->
        val (_, value) = BackupTree(rows).read(query.path)
        query.applyTo(DataNode(lastKey(query.path), value))
    }

    private suspend fun store(query: NodeQuery, node: DataNode) = lock.withLock {
        edit {
            if (query.isFiltered) merge(query.path, node.children.associate { it.key.orEmpty() to it.value }) else set(query.path, node.value)
        }
    }

    private suspend fun edit(block: BackupTree.() -> Unit) {
        val tree = BackupTree(dao.nodes())
        tree.block()
        val now = clock()
        dao.replaceNodes(
            upserts = tree.changed.map { path -> BackupNodeEntity(path, NodeJson.encode(tree.values[path]), now) },
            deletes = tree.deleted.toList(),
        )
    }

    private fun BackupTree.applyWrites(updates: Map<String, Any?>) {
        val now = clock()
        updates.forEach { (path, value) -> set(path, NodeTree.resolve(value, read(path).second, now)) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun decodeUpdates(json: String): Map<String, Any?> = NodeJson.decode(json) as? Map<String, Any?> ?: emptyMap()

    private fun lastKey(path: String): String? = NodeTree.segments(path).lastOrNull()

    private companion object {
        const val TIMEOUT_MS = 8_000L
    }
}

internal class BackupTree(rows: List<BackupNodeEntity>) {
    val values: MutableMap<String, Any?> = rows.associateTo(LinkedHashMap()) { it.path to NodeJson.decode(it.json) }
    val changed = mutableSetOf<String>()
    val deleted = mutableSetOf<String>()

    fun read(path: String): Pair<Boolean, Any?> {
        val target = normalize(path)
        val ancestor = values.keys.filter { isAncestorOrSelf(it, target) }.maxByOrNull { it.length }
        var value = ancestor?.let { NodeTree.get(values[it], relative(it, target)) }
        val descendants = values.keys.filter { isStrictAncestor(target, it) }.sortedBy { it.length }
        descendants.forEach { row -> value = NodeTree.set(value, relative(target, row), values[row]) }
        return (ancestor != null || descendants.isNotEmpty()) to value
    }

    fun set(path: String, value: Any?) {
        val target = normalize(path)
        val ancestors = values.keys.filter { isAncestorOrSelf(it, target) }
        ancestors.forEach { row ->
            values[row] = NodeTree.set(values[row], relative(row, target), value)
            changed += row
        }
        removeDescendants(target)
        if (ancestors.isEmpty()) put(target, value)
    }

    fun merge(path: String, children: Map<String, Any?>) {
        val target = normalize(path)
        if (values.keys.any { isAncestorOrSelf(it, target) }) {
            children.forEach { (key, value) -> set("$target/$key", value) }
            return
        }
        val merged = children.entries.fold(read(target).second) { acc, (key, value) -> NodeTree.set(acc, listOf(key), value) }
        removeDescendants(target)
        put(target, merged)
    }

    private fun put(path: String, value: Any?) {
        values[path] = value
        changed += path
        deleted -= path
    }

    private fun removeDescendants(path: String) {
        values.keys.filter { isStrictAncestor(path, it) }.forEach { row ->
            values.remove(row)
            changed -= row
            deleted += row
        }
    }

    private fun normalize(path: String): String = NodeTree.join(NodeTree.segments(path))

    private fun isAncestorOrSelf(ancestor: String, path: String): Boolean = ancestor == path || isStrictAncestor(ancestor, path)

    private fun isStrictAncestor(ancestor: String, path: String): Boolean = path.startsWith("$ancestor/")

    private fun relative(ancestor: String, path: String): List<String> = NodeTree.segments(path.removePrefix(ancestor))
}
