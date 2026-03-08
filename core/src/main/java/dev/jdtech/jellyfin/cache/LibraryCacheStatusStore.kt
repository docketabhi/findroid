package dev.jdtech.jellyfin.cache

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class LibraryCacheStatusStore @Inject constructor(private val sharedPreferences: SharedPreferences) {
    private val _status = MutableStateFlow(readStatus())
    val status: StateFlow<LibraryCacheStatus> = _status.asStateFlow()

    fun markSyncing() {
        persist(readStatus().copy(state = LibraryCacheState.SYNCING, lastError = null))
    }

    fun markReady(libraryCount: Int, itemCount: Int) {
        persist(
            LibraryCacheStatus(
                state = LibraryCacheState.READY,
                lastSuccessAtMs = System.currentTimeMillis(),
                lastError = null,
                libraryCount = libraryCount,
                itemCount = itemCount,
            )
        )
    }

    fun markError(message: String?) {
        persist(readStatus().copy(state = LibraryCacheState.ERROR, lastError = message))
    }

    private fun persist(status: LibraryCacheStatus) {
        sharedPreferences.edit()
            .putString(KEY_STATE, status.state.name)
            .putLong(KEY_LAST_SUCCESS_AT_MS, status.lastSuccessAtMs)
            .putString(KEY_LAST_ERROR, status.lastError)
            .putInt(KEY_LIBRARY_COUNT, status.libraryCount)
            .putInt(KEY_ITEM_COUNT, status.itemCount)
            .apply()
        _status.value = status
    }

    private fun readStatus(): LibraryCacheStatus {
        return LibraryCacheStatus(
            state =
                sharedPreferences.getString(KEY_STATE, LibraryCacheState.IDLE.name)?.let {
                    runCatching { LibraryCacheState.valueOf(it) }.getOrDefault(LibraryCacheState.IDLE)
                } ?: LibraryCacheState.IDLE,
            lastSuccessAtMs = sharedPreferences.getLong(KEY_LAST_SUCCESS_AT_MS, 0L),
            lastError = sharedPreferences.getString(KEY_LAST_ERROR, null),
            libraryCount = sharedPreferences.getInt(KEY_LIBRARY_COUNT, 0),
            itemCount = sharedPreferences.getInt(KEY_ITEM_COUNT, 0),
        )
    }

    private companion object {
        private const val KEY_STATE = "library_cache_status_state"
        private const val KEY_LAST_SUCCESS_AT_MS = "library_cache_status_last_success_at_ms"
        private const val KEY_LAST_ERROR = "library_cache_status_last_error"
        private const val KEY_LIBRARY_COUNT = "library_cache_status_library_count"
        private const val KEY_ITEM_COUNT = "library_cache_status_item_count"
    }
}

enum class LibraryCacheState {
    IDLE,
    SYNCING,
    READY,
    ERROR,
}

data class LibraryCacheStatus(
    val state: LibraryCacheState = LibraryCacheState.IDLE,
    val lastSuccessAtMs: Long = 0L,
    val lastError: String? = null,
    val libraryCount: Int = 0,
    val itemCount: Int = 0,
)
