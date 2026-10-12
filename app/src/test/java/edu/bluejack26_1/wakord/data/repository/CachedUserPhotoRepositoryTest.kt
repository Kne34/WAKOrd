package edu.bluejack26_1.wakord.data.repository

import edu.bluejack26_1.wakord.data.fake.FakeProfileRepository
import edu.bluejack26_1.wakord.data.fake.SampleSocial
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class CachedUserPhotoRepositoryTest {
    @Test
    fun `shares one photo stream per user and follows photo changes`() = runTest {
        val profiles = FakeProfileRepository(listOf(SampleSocial.julian.copy(photoUrl = "data:image/jpeg;base64,AAAA")))
        val photos = CachedUserPhotoRepository(profiles, backgroundScope)

        assertSame(photos.photo("julian"), photos.photo("julian"))
        assertEquals("data:image/jpeg;base64,AAAA", photos.photo("julian").first { it != null })

        profiles.users.update { it + ("julian" to SampleSocial.julian.copy(photoUrl = "data:image/jpeg;base64,BBBB")) }
        assertEquals("data:image/jpeg;base64,BBBB", photos.photo("julian").first { it == "data:image/jpeg;base64,BBBB" })
        assertNull(photos.photo("nobody").first())
    }
}
