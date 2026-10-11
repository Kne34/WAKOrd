package edu.bluejack26_1.wakord.ui.components

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PianoKeyboardTest {
    private val width = 14 * 20f + 13 * 2f + 8f
    private val height = 176f

    private fun keyAt(x: Float, y: Float) = pianoKeyAt(Offset(x, y), width, height, 4f, 2f, 48, 2)

    @Test
    fun `tapping the lower half of a white key returns that key`() {
        assertEquals(48, keyAt(14f, 160f))
        assertEquals(50, keyAt(36f, 160f))
        assertEquals(71, keyAt(width - 10f, 160f))
    }

    @Test
    fun `tapping the upper half between white keys returns the black key`() {
        assertEquals(49, keyAt(25f, 20f))
        assertEquals(54, keyAt(4f + 4 * 22f - 1f, 20f))
    }

    @Test
    fun `there is no black key between e and f`() {
        assertEquals(52, keyAt(4f + 2 * 22f + 10f, 20f))
    }

    @Test
    fun `taps outside the keys return nothing`() {
        assertNull(keyAt(1f, 80f))
        assertNull(keyAt(50f, 2f))
    }
}
