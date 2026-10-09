package edu.bluejack26_1.wakord.data.fake

import edu.bluejack26_1.wakord.domain.model.SyncStatus
import edu.bluejack26_1.wakord.domain.repository.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSyncRepository : SyncRepository {
    override val status = MutableStateFlow(SyncStatus())
}
