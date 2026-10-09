package edu.bluejack26_1.wakord.data.repository

import edu.bluejack26_1.wakord.data.local.SettingsDao
import edu.bluejack26_1.wakord.data.local.SettingsEntity
import edu.bluejack26_1.wakord.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomSettingsRepositoryTest {

    private class FakeSettingsDao : SettingsDao {
        val row = MutableStateFlow<SettingsEntity?>(null)
        override fun observe(): Flow<SettingsEntity?> = row
        override suspend fun get(): SettingsEntity? = row.value
        override suspend fun upsert(entity: SettingsEntity) {
            row.value = entity
        }
    }

    private val dao = FakeSettingsDao()
    private val repository = RoomSettingsRepository(dao)

    @Test
    fun `defaults when nothing is stored`() = runTest {
        assertEquals(ThemeMode.SYSTEM, repository.themeMode.first())
        assertEquals(440f, repository.referencePitch.first())
    }

    @Test
    fun `theme choice is kept`() = runTest {
        repository.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, repository.themeMode.first())
        assertEquals("LIGHT", dao.row.value?.themeMode)
    }

    @Test
    fun `changing one setting keeps the others`() = runTest {
        repository.setThemeMode(ThemeMode.DARK)
        repository.setReferencePitch(442f)
        repository.setPushEnabled(false)
        assertEquals(SettingsEntity(themeMode = "DARK", referencePitch = 442f, pushEnabled = false), dao.row.value)
    }

    @Test
    fun `reference pitch is clamped to a sane range`() = runTest {
        repository.setReferencePitch(1000f)
        assertEquals(466f, repository.referencePitch.first())
    }

    @Test
    fun `saved items stay offline until turned off`() = runTest {
        assertEquals(true, repository.keepOffline.first())
        repository.setKeepOffline(false)
        assertEquals(false, repository.keepOffline.first())
        assertEquals(false, dao.row.value?.keepOffline)
    }

    @Test
    fun `unknown stored theme falls back to system`() = runTest {
        dao.row.value = SettingsEntity(themeMode = "PURPLE")
        assertEquals(ThemeMode.SYSTEM, repository.themeMode.first())
    }
}
