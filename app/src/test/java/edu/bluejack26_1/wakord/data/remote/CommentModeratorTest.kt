package edu.bluejack26_1.wakord.data.remote

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommentModeratorTest {
    private val moderator = WordListModerator()

    @Test
    fun `friendly and critical comments pass`() = runTest {
        assertTrue(moderator.isAllowed("That G13 voicing is perfect"))
        assertTrue(moderator.isAllowed("The bridge drags a bit, try a faster tempo"))
        assertTrue(moderator.isAllowed("Lagunya enak banget, makasih chordnya"))
    }

    @Test
    fun `insults are blocked in english and indonesian even when disguised`() = runTest {
        assertFalse(moderator.isAllowed("you are a bitch"))
        assertFalse(moderator.isAllowed("dasar g0bl0k"))
        assertFalse(moderator.isAllowed("SH1T song"))
        assertFalse(moderator.isAllowed("k i l l your self"))
    }

    @Test
    fun `model verdict is read from json`() {
        assertFalse(GeminiCommentModerator.parseVerdict("{\"allowed\": false}"))
        assertTrue(GeminiCommentModerator.parseVerdict("```json\n{\"allowed\": true}\n```"))
        assertTrue(GeminiCommentModerator.parseVerdict("{}"))
    }
}
