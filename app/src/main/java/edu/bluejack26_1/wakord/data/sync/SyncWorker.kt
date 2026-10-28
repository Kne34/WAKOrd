package edu.bluejack26_1.wakord.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import edu.bluejack26_1.wakord.WAKordApp
import edu.bluejack26_1.wakord.core.error.AppError
import edu.bluejack26_1.wakord.data.repository.SyncScheduler
import java.util.concurrent.TimeUnit

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as WAKordApp).container
        val results = listOf(
            runCatching { container.firebaseBackup.flush() == 0 },
            container.savedLibraryRepository.syncNow().map { it == 0 },
            container.trainingRepository.syncPending().map { true },
            container.analysisRepository.syncPending().map { true },
        )
        container.backUpUserData()
        val needsRetry = results.any { r -> r.getOrNull() == false || r.exceptionOrNull().let { it != null && it !is AppError.NotLoggedIn } }
        return if (needsRetry) Result.retry() else Result.success()
    }
}

class WorkManagerSyncScheduler(private val context: Context) : SyncScheduler {
    override fun schedule() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    private companion object {
        const val WORK_NAME = "wakord-sync"
        const val BACKOFF_SECONDS = 30L
    }
}
