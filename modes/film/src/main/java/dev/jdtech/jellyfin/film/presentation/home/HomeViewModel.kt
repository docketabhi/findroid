package dev.jdtech.jellyfin.film.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.film.R as FilmR
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.FindroidCollection
import dev.jdtech.jellyfin.models.HomeItem
import dev.jdtech.jellyfin.models.HomeSection
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.repository.JellyfinRepositoryOfflineImpl
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.utils.toView
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jellyfin.sdk.model.api.BaseItemDto
import timber.log.Timber

@HiltViewModel
class HomeViewModel
@Inject
constructor(
    val repository: JellyfinRepository,
    val offlineRepository: JellyfinRepositoryOfflineImpl,
    val appPreferences: AppPreferences,
    val database: ServerDatabaseDao,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    private val uuidSuggestions = UUID.fromString("31e47044-9b79-4bb0-99d0-0e477ed65420")
    private val uuidContinueWatching =
        UUID(4937169328197226115, -4704919157662094443) // 44845958-8326-4e83-beb4-c4f42e9eeb95
    private val uuidNextUp =
        UUID(1783371395749072194, -6164625418200444295) // 18bfced5-f237-4d42-aa72-d9d7fed19279

    private val uiTextContinueWatching = UiText.StringResource(FilmR.string.continue_watching)
    private val uiTextNextUp = UiText.StringResource(FilmR.string.next_up)

    private var loadJob: Job? = null

    fun loadData(forceRefresh: Boolean = true) {
        if (_state.value.isLoading) return

        Timber.i("Loading data")
        loadJob?.cancel()
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val showLibrariesFirstRow =
                    appPreferences.getValue(appPreferences.homeLibrariesFirstRow)
                val currentServer = appPreferences.getValue(appPreferences.currentServer)
                val newServer = currentServer?.let { serverId -> database.get(serverId) }
                val cachedLibraries =
                    runCatching { offlineRepository.getLibraries() }.getOrDefault(emptyList())
                val cachedResumeSection =
                    runCatching { loadCachedResumeItemsData() }.getOrNull()
                val cachedNextUpSection =
                    runCatching { loadCachedNextUpItemsData() }.getOrNull()

                _state.update {
                    it.copy(
                        server = newServer,
                        libraries =
                            if (cachedLibraries.isNotEmpty()) cachedLibraries else it.libraries,
                        resumeSection = cachedResumeSection ?: it.resumeSection,
                        nextUpSection = cachedNextUpSection ?: it.nextUpSection,
                        showLibrariesFirstRow = showLibrariesFirstRow,
                        isLoading = true,
                        error = null,
                    )
                }

                val (libraries, suggestionsSection, resumeSection, nextUpSection, supportedViews) =
                    coroutineScope {
                        val librariesDeferred =
                            async {
                                runCatching { loadLibrariesData() }
                                    .getOrElse { e ->
                                        Timber.w(e, "Failed loading libraries")
                                        emptyList()
                                    }
                            }
                        val suggestionsDeferred =
                            async {
                                runCatching { loadSuggestionsData() }
                                    .getOrElse { e ->
                                        Timber.w(e, "Failed loading suggestions")
                                        null
                                    }
                            }
                        val resumeDeferred =
                            async {
                                runCatching { loadResumeItemsData() }
                                    .getOrElse { e ->
                                        Timber.w(e, "Failed loading resume items")
                                        null
                                    }
                            }
                        val nextUpDeferred =
                            async {
                                runCatching { loadNextUpItemsData() }
                                    .getOrElse { e ->
                                        Timber.w(e, "Failed loading next up items")
                                        null
                                    }
                            }
                        val supportedViewsDeferred =
                            async {
                                runCatching { loadSupportedViews() }
                                    .getOrElse { e ->
                                        Timber.w(e, "Failed loading supported views")
                                        emptyList()
                                    }
                            }
                        Quintuple(
                            librariesDeferred.await(),
                            suggestionsDeferred.await(),
                            resumeDeferred.await(),
                            nextUpDeferred.await(),
                            supportedViewsDeferred.await(),
                        )
                    }

                // Render the first rows early, then continue loading the rest in background.
                val initialViews = supportedViews.take(INITIAL_VIEWS_BATCH_SIZE)
                val remainingViews = supportedViews.drop(INITIAL_VIEWS_BATCH_SIZE)
                val initialViewItems = loadViewsData(initialViews)

                _state.update {
                    it.copy(
                        server = newServer,
                        libraries = libraries,
                        suggestionsSection = suggestionsSection,
                        resumeSection = resumeSection,
                        nextUpSection = nextUpSection,
                        views = initialViewItems,
                        showLibrariesFirstRow = showLibrariesFirstRow,
                        isLoading = false,
                    )
                }

                if (remainingViews.isNotEmpty()) {
                    val additionalViewItems = loadViewsData(remainingViews)
                    if (additionalViewItems.isNotEmpty()) {
                        _state.update { state ->
                            state.copy(
                                views =
                                    (state.views + additionalViewItems)
                                        .distinctBy { viewItem -> viewItem.id }
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e, isLoading = false) }
            }
        }
    }

    private suspend fun loadSuggestionsData(): HomeItem.Suggestions? {
        if (!appPreferences.getValue(appPreferences.homeSuggestions)) {
            return null
        }

        val items = repository.getSuggestions()

        return if (items.isEmpty()) {
            null
        } else {
            HomeItem.Suggestions(id = uuidSuggestions, items = items)
        }
    }

    private suspend fun loadLibrariesData(): List<FindroidCollection> {
        return repository.getLibraries()
    }

    private suspend fun loadResumeItemsData(): HomeItem.Section? {
        if (!appPreferences.getValue(appPreferences.homeContinueWatching)) {
            return null
        }

        val resumeItems = repository.getResumeItems()

        return if (resumeItems.isEmpty()) {
            null
        } else {
            HomeItem.Section(
                HomeSection(uuidContinueWatching, uiTextContinueWatching, resumeItems)
            )
        }
    }

    private suspend fun loadCachedResumeItemsData(): HomeItem.Section? {
        if (!appPreferences.getValue(appPreferences.homeContinueWatching)) {
            return null
        }

        val resumeItems = offlineRepository.getResumeItems()

        return if (resumeItems.isEmpty()) {
            null
        } else {
            HomeItem.Section(HomeSection(uuidContinueWatching, uiTextContinueWatching, resumeItems))
        }
    }

    private suspend fun loadNextUpItemsData(): HomeItem.Section? {
        if (!appPreferences.getValue(appPreferences.homeNextUp)) {
            return null
        }

        val nextUpItems = repository.getNextUp()

        return if (nextUpItems.isEmpty()) {
            null
        } else {
            HomeItem.Section(HomeSection(uuidNextUp, uiTextNextUp, nextUpItems))
        }
    }

    private suspend fun loadCachedNextUpItemsData(): HomeItem.Section? {
        if (!appPreferences.getValue(appPreferences.homeNextUp)) {
            return null
        }

        val nextUpItems = offlineRepository.getNextUp()

        return if (nextUpItems.isEmpty()) {
            null
        } else {
            HomeItem.Section(HomeSection(uuidNextUp, uiTextNextUp, nextUpItems))
        }
    }

    private suspend fun loadSupportedViews(): List<BaseItemDto> {
        if (!appPreferences.getValue(appPreferences.homeLatest)) return emptyList()

        return repository.getUserViews().filter { view ->
            CollectionType.fromString(view.collectionType?.serialName) in CollectionType.supported
        }
    }

    private suspend fun loadViewsData(views: List<BaseItemDto>): List<HomeItem.ViewItem> {
        if (views.isEmpty()) return emptyList()

        val semaphore = Semaphore(VIEW_FETCH_PARALLELISM)
        return coroutineScope {
            views.map { view ->
                async {
                    semaphore.withPermit {
                        val latest =
                            runCatching { repository.getLatestMedia(view.id) }
                                .getOrElse { e ->
                                    Timber.w(e, "Failed loading latest for view=${view.id}")
                                    emptyList()
                                }
                        if (latest.isEmpty()) {
                            null
                        } else {
                            HomeItem.ViewItem(view.toView(latest))
                        }
                    }
                }
            }.awaitAll().filterNotNull()
        }
    }

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.OnRetryClick -> {
                loadData(forceRefresh = true)
            }
            else -> Unit
        }
    }

    private companion object {
        private const val INITIAL_VIEWS_BATCH_SIZE = 2
        private const val VIEW_FETCH_PARALLELISM = 3
    }
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
)
