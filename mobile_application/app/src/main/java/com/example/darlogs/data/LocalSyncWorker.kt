package com.example.darlogs.data

import android.content.Context
import androidx.work.*
import com.example.darlogs.ApiClient
import com.example.darlogs.R
import java.util.concurrent.TimeUnit

class LocalSyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = RecordRepository(applicationContext)
        val pendingRecords = repository.getPendingSyncRecords()

        var hasError = false
        for (record in pendingRecords) {
            val success = when (record.syncStatus) {
                SyncStatus.PENDING_INSERT, SyncStatus.PENDING_UPDATE -> {
                    repository.syncSingleRecord(record)
                }
                SyncStatus.PENDING_DELETE -> {
                    syncDelete(record)
                }
                else -> true
            }
            if (!success) hasError = true
        }

        return if (hasError) Result.retry() else Result.success()
    }

    private suspend fun syncDelete(entity: RecordEntity): Boolean {
        if (entity.id == null) {
            AppDatabase.getDatabase(applicationContext).recordDao().deleteRecord(entity)
            return true
        }

        val url = "${applicationContext.getString(R.string.records_api_url)}?id=${entity.id}"
        val response = ApiClient.deleteJson(url)
        if (response.success && response.json?.optBoolean("success") == true) {
            AppDatabase.getDatabase(applicationContext).recordDao().deleteRecord(entity)
            return true
        }
        return false
    }

    companion object {
        fun scheduleSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<LocalSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "LocalSyncWork",
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
        }
    }
}
