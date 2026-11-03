package edu.bluejack26_1.wakord.data.remote

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

sealed interface NodeOrder {
    data object Key : NodeOrder
    data object Value : NodeOrder
    data class Child(val name: String) : NodeOrder
}

data class NodeQuery(
    val path: String,
    val orderBy: NodeOrder = NodeOrder.Key,
    val equalTo: Any? = null,
    val endBefore: Double? = null,
    val limitToLast: Int? = null,
) {
    val isFiltered: Boolean get() = equalTo != null || endBefore != null || limitToLast != null

    fun applyTo(node: DataNode): DataNode {
        if (orderBy == NodeOrder.Key && !isFiltered) return node
        val sorted = node.children
            .map { child -> child to sortValue(child) }
            .filter { (_, v) -> equalTo == null || compareValues(v, equalTo) == 0 }
            .filter { (_, v) -> endBefore == null || (v is Number && v.toDouble() < endBefore) }
            .sortedWith { a, b -> compareValues(a.second, b.second).takeIf { it != 0 } ?: a.first.key.orEmpty().compareTo(b.first.key.orEmpty()) }
            .map { it.first }
        val limited = limitToLast?.let { sorted.takeLast(it) } ?: sorted
        return DataNode(node.key, limited.associateTo(LinkedHashMap()) { it.key.orEmpty() to it.value }.ifEmpty { null })
    }

    private fun sortValue(child: DataNode): Any? = when (orderBy) {
        NodeOrder.Key -> child.key
        NodeOrder.Value -> child.value
        is NodeOrder.Child -> child.child(orderBy.name).value
    }

    private fun compareValues(a: Any?, b: Any?): Int {
        val rank = rank(a).compareTo(rank(b))
        if (rank != 0) return rank
        return when {
            a is Boolean && b is Boolean -> a.compareTo(b)
            a is Number && b is Number -> a.toDouble().compareTo(b.toDouble())
            a is String && b is String -> a.compareTo(b)
            else -> 0
        }
    }

    private fun rank(value: Any?): Int = when (value) {
        null -> 0
        is Boolean -> 1
        is Number -> 2
        is String -> 3
        else -> 4
    }
}

interface RemoteDatabase {
    suspend fun get(query: NodeQuery): DataNode
    fun observe(query: NodeQuery): Flow<DataNode>
    suspend fun update(updates: Map<String, Any?>)
    fun newKey(path: String): String
}

class FirebaseRemoteDatabase(private val database: FirebaseDatabase) : RemoteDatabase {
    override suspend fun get(query: NodeQuery): DataNode = query.toFirebase().get().await().toNode()

    override fun observe(query: NodeQuery): Flow<DataNode> = query.toFirebase().valueFlow().map { it.toNode() }

    override suspend fun update(updates: Map<String, Any?>) {
        database.reference.updateChildren(updates).await()
    }

    override fun newKey(path: String): String = requireNotNull(database.getReference(path).push().key)

    private fun NodeQuery.toFirebase(): Query {
        val reference = database.getReference(path)
        var query: Query = when (val order = orderBy) {
            NodeOrder.Key -> reference
            NodeOrder.Value -> reference.orderByValue()
            is NodeOrder.Child -> reference.orderByChild(order.name)
        }
        when (val value = equalTo) {
            is String -> query = query.equalTo(value)
            is Boolean -> query = query.equalTo(value)
            is Number -> query = query.equalTo(value.toDouble())
        }
        endBefore?.let { query = query.endBefore(it) }
        limitToLast?.let { query = query.limitToLast(it) }
        return query
    }
}
