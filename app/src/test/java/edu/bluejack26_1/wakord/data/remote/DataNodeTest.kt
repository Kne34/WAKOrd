package edu.bluejack26_1.wakord.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class DataNodeTest {
    @Test
    fun `json keeps nested values, lists and numbers`() {
        val value = mapOf(
            "title" to "Sunset Pop Loop",
            "bpm" to 96,
            "rating" to 4.5,
            "public" to true,
            "chords" to listOf("C", "Em", "F"),
        )

        val node = DataNode("p1", NodeJson.decode(NodeJson.encode(value)))

        assertEquals("Sunset Pop Loop", node.string("title"))
        assertEquals(96L, node.child("bpm").asLong())
        assertEquals(4.5, node.child("rating").asDouble()!!, 0.0)
        assertEquals(true, node.child("public").asBoolean())
        assertEquals(listOf("C", "Em", "F"), node.strings("chords"))
    }

    @Test
    fun `setting null removes the value and empty parents`() {
        val tree = NodeTree.set(mapOf("a" to mapOf("b" to 1L)), listOf("a", "b"), null)

        assertNull(tree)
    }

    @Test
    fun `server values resolve against the current value`() {
        val resolved = NodeTree.resolve(
            mapOf("count" to mapOf(".sv" to mapOf("increment" to -1L)), "at" to mapOf(".sv" to "timestamp")),
            mapOf("count" to 4L),
            now = 77L,
        )

        assertEquals(mapOf("count" to 3L, "at" to 77L), resolved)
    }

    @Test
    fun `queries filter by child value and keep the last items`() {
        val notifications = DataNode(
            "me",
            mapOf(
                "n1" to mapOf("read" to true, "createdAt" to 1L),
                "n2" to mapOf("read" to false, "createdAt" to 3L),
                "n3" to mapOf("read" to false, "createdAt" to 2L),
            ),
        )

        val unread = NodeQuery("notifications/me", NodeOrder.Child("read"), equalTo = false).applyTo(notifications)
        val latest = NodeQuery("notifications/me", NodeOrder.Child("createdAt"), limitToLast = 1).applyTo(notifications)

        assertEquals(setOf("n2", "n3"), unread.children.map { it.key }.toSet())
        assertEquals(listOf("n2"), latest.children.map { it.key })
        assertFalse(DataNode("x", null).exists())
    }
}
