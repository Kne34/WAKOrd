package edu.bluejack26_1.wakord.domain.repository

import edu.bluejack26_1.wakord.domain.model.AnalysisResult
import kotlinx.coroutines.flow.Flow

interface AnalysisRepository {
    val history: Flow<List<AnalysisResult>>
    suspend fun save(result: AnalysisResult): Result<Unit>
    suspend fun syncPending(): Result<Int>
}

interface ChordAnalyzer {
    suspend fun analyze(samples: FloatArray, sampleRate: Int): Result<AnalysisResult>
}

interface CommentModerator {
    suspend fun isAllowed(text: String): Boolean
}
