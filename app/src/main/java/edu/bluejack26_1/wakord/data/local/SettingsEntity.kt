package edu.bluejack26_1.wakord.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import edu.bluejack26_1.wakord.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 0,
    val themeMode: String = ThemeMode.SYSTEM.name,
    val referencePitch: Float = 440f,
    val pushEnabled: Boolean = true,
)

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 0")
    fun observe(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 0")
    suspend fun get(): SettingsEntity?

    @Upsert
    suspend fun upsert(entity: SettingsEntity)
}
