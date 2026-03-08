package dev.jdtech.jellyfin.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.jdtech.jellyfin.cache.LibraryCacheStatusStore
import dev.jdtech.jellyfin.cache.LibraryCacheSyncManager
import kotlinx.coroutines.sync.Mutex
import timber.log.Timber

@HiltWorker
class LibraryCacheSyncWorker
@AssistedInject
constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncManager: LibraryCacheSyncManager,
    private val statusStore: LibraryCacheStatusStore,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!syncMutex.tryLock()) {
            return Result.success()
        }

        return runCatching {
            try {
                syncManager.syncCurrentServer()
                Result.success()
            } finally {
                syncMutex.unlock()
            }
        }.getOrElse { error ->
            Timber.e(error, "Library cache sync failed")
            statusStore.markError(error.message ?: error.javaClass.simpleName)
            Result.retry()
        }
    }

    private companion object {
        private val syncMutex = Mutex()
    }
}
