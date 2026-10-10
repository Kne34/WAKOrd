package edu.bluejack26_1.wakord.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SettingsEntity::class,
        TrainingSessionEntity::class,
        AnalysisEntity::class,
        SavedItemEntity::class,
        SyncStateEntity::class,
    ],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
    ],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
    abstract fun trainingDao(): TrainingDao
    abstract fun analysisDao(): AnalysisDao
    abstract fun savedItemDao(): SavedItemDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "wakord.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
