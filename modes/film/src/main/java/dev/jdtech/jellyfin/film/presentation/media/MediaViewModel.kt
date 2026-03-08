package dev.jdtech.jellyfin.film.presentation.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.repository.JellyfinRepositoryOfflineImpl
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MediaViewModel
@Inject
constructor(
    private val repository: JellyfinRepository,
    private val offlineRepository: JellyfinRepositoryOfflineImpl,
) : ViewModel() {
    private val _state = MutableStateFlow(MediaState())
    val state = _state.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            val cachedLibraries =
                runCatching { offlineRepository.getLibraries() }.getOrDefault(emptyList())
            _state.emit(
                _state.value.copy(
                    libraries = if (cachedLibraries.isNotEmpty()) cachedLibraries else _state.value.libraries,
                    isLoading = true,
                    error = null,
                )
            )
            try {
                val libraries = repository.getLibraries()
                _state.emit(_state.value.copy(libraries = libraries))
            } catch (e: Exception) {
                _state.emit(_state.value.copy(error = e))
            }
            _state.emit(_state.value.copy(isLoading = false))
        }
    }

    fun onAction(action: MediaAction) {
        when (action) {
            is MediaAction.OnRetryClick -> {
                loadData()
            }
            else -> Unit
        }
    }
}
