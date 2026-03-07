package dev.jdtech.jellyfin.presentation.film

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.dummy.dummyHomeSection
import dev.jdtech.jellyfin.core.presentation.dummy.dummyHomeSuggestions
import dev.jdtech.jellyfin.core.presentation.dummy.dummyHomeView
import dev.jdtech.jellyfin.film.R as FilmR
import dev.jdtech.jellyfin.film.presentation.home.HomeAction
import dev.jdtech.jellyfin.film.presentation.home.HomeState
import dev.jdtech.jellyfin.film.presentation.home.HomeViewModel
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.FindroidCollection
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.models.HomeSection as HomeSectionModel
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.presentation.film.components.HomeSection
import dev.jdtech.jellyfin.presentation.film.components.HomeView
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemKind

@Composable
fun HomeScreen(
    navigateToLibrary: (libraryId: UUID, libraryName: String, libraryType: CollectionType) -> Unit,
    navigateToMovie: (itemId: UUID) -> Unit,
    navigateToShow: (itemId: UUID) -> Unit,
    navigateToPlayer: (itemId: UUID, itemKind: BaseItemKind) -> Unit,
    firstContentFocusRequester: FocusRequester? = null,
    viewModel: HomeViewModel = hiltViewModel(),
    isLoading: (Boolean) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) { viewModel.loadData() }

    LaunchedEffect(state.isLoading) { isLoading(state.isLoading) }

    HomeScreenLayout(
        state = state,
        firstContentFocusRequester = firstContentFocusRequester,
        onAction = { action ->
            when (action) {
                is HomeAction.OnItemClick -> {
                    val item = action.item
                    when (item) {
                        is FindroidCollection -> navigateToLibrary(item.id, item.name, item.type)
                        is FindroidMovie -> navigateToMovie(item.id)
                        is FindroidShow -> navigateToShow(item.id)
                        is FindroidEpisode -> {
                            navigateToPlayer(item.id, BaseItemKind.EPISODE)
                        }
                    }
                }
                is HomeAction.OnLibraryClick ->
                    navigateToLibrary(
                        action.library.id,
                        action.library.name,
                        action.library.type,
                    )
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

@Composable
private fun HomeScreenLayout(
    state: HomeState,
    firstContentFocusRequester: FocusRequester? = null,
    onAction: (HomeAction) -> Unit,
) {
    val itemsPadding = PaddingValues(horizontal = MaterialTheme.spacings.large)
    val hasLibraries = state.libraries.isNotEmpty()
    val firstViewWithItemsId = state.views.firstOrNull { it.view.items.isNotEmpty() }?.id
    val hasSuggestions = !state.suggestionsSection?.items.isNullOrEmpty()
    val hasResume = !state.resumeSection?.homeSection?.items.isNullOrEmpty()
    val hasNextUp = !state.nextUpSection?.homeSection?.items.isNullOrEmpty()
    val firstFocusTargetKey =
        when {
            state.showLibrariesFirstRow && hasLibraries -> "libraries"
            state.showLibrariesFirstRow && hasResume -> "resume"
            hasSuggestions -> "suggestions"
            hasResume -> "resume"
            hasNextUp -> "nextup"
            firstViewWithItemsId != null -> "view:$firstViewWithItemsId"
            else -> null
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                top = MaterialTheme.spacings.extraSmall,
                bottom = MaterialTheme.spacings.large,
            ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.large),
    ) {
        if (
                state.suggestionsSection == null &&
                state.resumeSection == null &&
                state.nextUpSection == null &&
                state.libraries.isEmpty() &&
                state.views.isEmpty() &&
                !state.isLoading
        ) {
            item(key = "empty-home-state") {
                Text(
                    text = "No content available",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(itemsPadding),
                )
            }
        }
        if (state.showLibrariesFirstRow && hasLibraries) {
            item(key = HOME_LIBRARIES_ROW_ID) {
                HomeSection(
                    section =
                        HomeSectionModel(
                            id = HOME_LIBRARIES_ROW_ID,
                            name = UiText.StringResource(CoreR.string.libraries),
                            items = state.libraries,
                        ),
                    itemsPadding = itemsPadding,
                    onAction = onAction,
                    firstItemFocusRequester =
                        if (firstFocusTargetKey == "libraries") firstContentFocusRequester else null,
                    modifier = Modifier,
                )
            }
        }
        if (state.showLibrariesFirstRow) {
            state.resumeSection?.let { section ->
                item(key = section.id) {
                    HomeSection(
                        section = section.homeSection,
                        itemsPadding = itemsPadding,
                        onAction = onAction,
                        firstItemFocusRequester =
                            if (firstFocusTargetKey == "resume") firstContentFocusRequester
                            else null,
                        modifier = Modifier,
                    )
                }
            }
        }
        state.suggestionsSection?.let { section ->
            item(key = section.id) {
                HomeSection(
                    section =
                        HomeSectionModel(
                            id = section.id,
                            name = UiText.StringResource(FilmR.string.suggested_for_you),
                            items = section.items,
                        ),
                    itemsPadding = itemsPadding,
                    onAction = onAction,
                    firstItemFocusRequester =
                        if (firstFocusTargetKey == "suggestions") firstContentFocusRequester
                        else null,
                    modifier = Modifier,
                )
            }
        }
        if (!state.showLibrariesFirstRow) {
            state.resumeSection?.let { section ->
                item(key = section.id) {
                    HomeSection(
                        section = section.homeSection,
                        itemsPadding = itemsPadding,
                        onAction = onAction,
                        firstItemFocusRequester =
                            if (firstFocusTargetKey == "resume") firstContentFocusRequester
                            else null,
                        modifier = Modifier,
                    )
                }
            }
        }
        state.nextUpSection?.let { section ->
            item(key = section.id) {
                HomeSection(
                    section = section.homeSection,
                    itemsPadding = itemsPadding,
                    onAction = onAction,
                    firstItemFocusRequester =
                        if (firstFocusTargetKey == "nextup") firstContentFocusRequester else null,
                    modifier = Modifier,
                )
            }
        }
        items(state.views, key = { it.id }) { view ->
            HomeView(
                view = view,
                itemsPadding = itemsPadding,
                onAction = onAction,
                firstItemFocusRequester =
                    if (firstFocusTargetKey == "view:${view.id}") firstContentFocusRequester else null,
                modifier = Modifier,
            )
        }
    }
}

private val HOME_LIBRARIES_ROW_ID: UUID =
    UUID.fromString("ecce9cd1-66b0-43b9-a67d-0f4cc4f17347")

@Preview(device = "id:tv_1080p")
@Composable
private fun HomeScreenLayoutPreview() {
    FindroidTheme {
        HomeScreenLayout(
            state =
                HomeState(
                    suggestionsSection = dummyHomeSuggestions,
                    resumeSection = dummyHomeSection,
                    views = listOf(dummyHomeView),
                ),
            onAction = {},
        )
    }
}
