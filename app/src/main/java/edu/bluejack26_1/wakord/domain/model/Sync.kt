package edu.bluejack26_1.wakord.domain.model

data class SyncStatus(
    val savedSongs: Int = 0,
    val savedChords: Int = 0,
    val isOnline: Boolean = false,
    val lastSyncedAt: Long? = null,
)
