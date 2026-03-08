package dev.jdtech.jellyfin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.viewmodels.MainViewModel
import dev.jdtech.jellyfin.work.LibraryCacheSyncWorker
import dev.jdtech.jellyfin.work.SyncWorker
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    companion object {
        private const val USER_DATA_SYNC_ONCE_WORK_NAME = "syncUserDataOnce"
        private const val USER_DATA_SYNC_PERIODIC_WORK_NAME = "syncUserDataPeriodic"
        private const val LIBRARY_CACHE_SYNC_ONCE_WORK_NAME = "libraryCacheSyncOnce"
        private const val LIBRARY_CACHE_SYNC_PERIODIC_WORK_NAME = "libraryCacheSyncPeriodic"
        private const val USER_DATA_SYNC_INTERVAL_HOURS = 6L
        private const val LIBRARY_CACHE_SYNC_INTERVAL_HOURS = 6L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()

            FindroidTheme {
                val navController = rememberNavController()
                if (!state.isLoading) {
                    NavigationRoot(
                        navController = navController,
                        hasServers = state.hasServers,
                        hasCurrentServer = state.hasCurrentServer,
                        hasCurrentUser = state.hasCurrentUser,
                    )
                }
            }
        }

        scheduleBackgroundSync()
    }

    private fun scheduleBackgroundSync() {
        val constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val workManager = WorkManager.getInstance(applicationContext)

        val oneTimeSync =
            OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(constraints).build()
        val periodicSync =
            PeriodicWorkRequestBuilder<SyncWorker>(
                USER_DATA_SYNC_INTERVAL_HOURS,
                TimeUnit.HOURS,
            )
                .setConstraints(constraints)
                .build()

        workManager.beginUniqueWork(
            USER_DATA_SYNC_ONCE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            oneTimeSync,
        ).enqueue()
        workManager.enqueueUniquePeriodicWork(
            USER_DATA_SYNC_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicSync,
        )

        val oneTimeLibraryCacheSync =
            OneTimeWorkRequestBuilder<LibraryCacheSyncWorker>().setConstraints(constraints).build()
        val periodicLibraryCacheSync =
            PeriodicWorkRequestBuilder<LibraryCacheSyncWorker>(
                LIBRARY_CACHE_SYNC_INTERVAL_HOURS,
                TimeUnit.HOURS,
            )
                .setConstraints(constraints)
                .setInitialDelay(LIBRARY_CACHE_SYNC_INTERVAL_HOURS, TimeUnit.HOURS)
                .build()

        workManager.beginUniqueWork(
            LIBRARY_CACHE_SYNC_ONCE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            oneTimeLibraryCacheSync,
        ).enqueue()
        workManager.enqueueUniquePeriodicWork(
            LIBRARY_CACHE_SYNC_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicLibraryCacheSync,
        )
    }
}
