package edu.bluejack26_1.wakord.domain.repository

import edu.bluejack26_1.wakord.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

interface SyncRepository {
    val status: Flow<SyncStatus>
}
