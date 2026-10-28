package edu.bluejack26_1.wakord.data.remote

import com.google.firebase.database.DataSnapshot
import org.json.JSONArray
import org.json.JSONObject

class DataNode(val key: String?, val value: Any?) {
    fun exists(): Boolean = value != null

    fun child(path: String): DataNode =
        NodeTree.segments(path).fold(this) { node, segment -> DataNode(segment, (node.value as? Map<*, *>)?.get(segment)) }

    val children: List<DataNode>
        get() = (value as? Map<*, *>)?.map { (k, v) -> DataNode(k.toString(), v) }.orEmpty()

    val childrenCount: Int get() = (value as? Map<*, *>)?.size ?: 0

    fun asString(): String? = value as? String

    fun asLong(): Long? = (value as? Number)?.toLong()

    fun asDouble(): Double? = (value as? Number)?.toDouble()

    fun asBoolean(): Boolean? = value as? Boolean

    override fun equals(other: Any?): Boolean = other is DataNode && other.key == key && other.value == value

    override fun hashCode(): Int = 31 * key.hashCode() + value.hashCode()
}

fun DataSnapshot.toNode(): DataNode = DataNode(key, plainValue())

private fun DataSnapshot.plainValue(): Any? =
    if (hasChildren()) children.associateTo(LinkedHashMap()) { it.key.orEmpty() to it.plainValue() } else value

object NodeJson {
    fun encode(value: Any?): String = JSONObject().put(ROOT, toJson(value)).toString()

    fun decode(json: String): Any? = fromJson(JSONObject(json).opt(ROOT))

    private fun toJson(value: Any?): Any = when (value) {
        null -> JSONObject.NULL
        is Map<*, *> -> JSONObject().apply { value.forEach { (k, v) -> put(k.toString(), toJson(v)) } }
        is List<*> -> JSONArray().apply { value.forEach { put(toJson(it)) } }
        is Int -> value.toLong()
        is Float -> value.toDouble()
        else -> value
    }

    private fun fromJson(value: Any?): Any? = when (value) {
        null, JSONObject.NULL -> null
        is JSONObject -> value.keys().asSequence().associateWithTo(LinkedHashMap()) { fromJson(value.get(it)) }
        is JSONArray -> (0 until value.length()).associateTo(LinkedHashMap()) { it.toString() to fromJson(value.get(it)) }
        is Int -> value.toLong()
        else -> value
    }

    private const val ROOT = "v"
}

object NodeTree {
    fun segments(path: String): List<String> = path.split('/').filter { it.isNotEmpty() }

    fun join(segments: List<String>): String = segments.joinToString("/")

    fun get(root: Any?, path: List<String>): Any? =
        path.fold(root) { node, segment -> (node as? Map<*, *>)?.get(segment) }

    fun set(root: Any?, path: List<String>, value: Any?): Any? {
        if (path.isEmpty()) return value
        val map = LinkedHashMap<String, Any?>()
        (root as? Map<*, *>)?.forEach { (k, v) -> map[k.toString()] = v }
        val child = set(map[path.first()], path.drop(1), value)
        if (child == null) map.remove(path.first()) else map[path.first()] = child
        return map.ifEmpty { null }
    }

    fun normalize(value: Any?): Any? = when (value) {
        is Map<*, *> -> value.entries
            .mapNotNull { (k, v) -> normalize(v)?.let { k.toString() to it } }
            .toMap(LinkedHashMap())
            .ifEmpty { null }
        is List<*> -> normalize(value.withIndex().associate { (i, v) -> i.toString() to v })
        is Int -> value.toLong()
        is Float -> value.toDouble()
        else -> value
    }

    fun resolve(value: Any?, current: Any?, now: Long): Any? {
        val map = value as? Map<*, *> ?: return normalize(value)
        val server = map[SERVER_VALUE]
        if (map.size == 1 && server != null) {
            return when {
                server == TIMESTAMP -> now
                server is Map<*, *> && server[INCREMENT] is Number -> increment(current as? Number, server[INCREMENT] as Number)
                else -> null
            }
        }
        val currentMap = current as? Map<*, *>
        return map.entries
            .mapNotNull { (k, v) -> resolve(v, currentMap?.get(k), now)?.let { k.toString() to it } }
            .toMap(LinkedHashMap())
            .ifEmpty { null }
    }

    private fun increment(current: Number?, delta: Number): Number =
        if (current is Double || delta is Double || current is Float || delta is Float) {
            (current?.toDouble() ?: 0.0) + delta.toDouble()
        } else {
            (current?.toLong() ?: 0L) + delta.toLong()
        }

    private const val SERVER_VALUE = ".sv"
    private const val TIMESTAMP = "timestamp"
    private const val INCREMENT = "increment"
}
