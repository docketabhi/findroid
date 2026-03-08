package dev.jdtech.jellyfin.presentation.film

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.dummy.dummyMovies
import dev.jdtech.jellyfin.film.presentation.library.LibraryAction
import dev.jdtech.jellyfin.film.presentation.library.LibraryState
import dev.jdtech.jellyfin.film.presentation.library.LibraryViewModel
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.models.FindroidFolder
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.presentation.film.components.SortByDialog
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.ui.components.Direction
import dev.jdtech.jellyfin.ui.components.ItemCard
import dev.jdtech.jellyfin.ui.components.MusicListColumnsHeader
import dev.jdtech.jellyfin.ui.components.MusicListItem
import dev.jdtech.jellyfin.ui.components.StatusContent
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaStreamType

@Composable
fun LibraryScreen(
    libraryId: UUID,
    libraryName: String,
    libraryType: CollectionType,
    navigateToLibrary: (libraryId: UUID, libraryName: String, libraryType: CollectionType) -> Unit,
    navigateToMovie: (itemId: UUID) -> Unit,
    navigateToShow: (itemId: UUID) -> Unit,
    navigateToPlayer: (itemId: UUID, itemKind: BaseItemKind, queueParentId: UUID?) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    var initialLoad by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(true) {
        viewModel.setup(parentId = libraryId, libraryType = libraryType)
        if (initialLoad) {
            viewModel.loadItems()
            initialLoad = false
        }
    }

    LibraryScreenLayout(
        libraryName = libraryName,
        libraryType = libraryType,
        state = state,
        onRetry = { viewModel.loadItems() },
        onAction = { action ->
            when (action) {
                is LibraryAction.OnItemClick -> {
                    when (action.item) {
                        is FindroidMovie -> {
                            val movieItem = action.item as FindroidMovie
                            if (libraryType == CollectionType.Music || libraryType == CollectionType.MusicVideos || libraryType == CollectionType.HomeVideos) {
                                navigateToPlayer(
                                    movieItem.id,
                                    resolvePlayableKind(movieItem, libraryType),
                                    libraryId,
                                )
                            } else {
                                navigateToMovie(movieItem.id)
                            }
                        }
                        is FindroidShow -> navigateToShow(action.item.id)
                        is FindroidFolder ->
                            navigateToLibrary(action.item.id, action.item.name, libraryType)
                    }
                }
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

@Composable
private fun LibraryScreenLayout(
    libraryName: String,
    libraryType: CollectionType,
    state: LibraryState,
    onRetry: () -> Unit,
    onAction: (LibraryAction) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    val items = state.items.collectAsLazyPagingItems()
    val isMusicLibrary = libraryType == CollectionType.Music
    val gridSpacing = if (isMusicLibrary) MaterialTheme.spacings.small else MaterialTheme.spacings.default
    val gridContentPadding =
        if (isMusicLibrary) {
            PaddingValues(
                horizontal = MaterialTheme.spacings.default * 2,
                vertical = MaterialTheme.spacings.medium,
            )
        } else {
            PaddingValues(
                horizontal = MaterialTheme.spacings.default * 2,
                vertical = MaterialTheme.spacings.large,
            )
        }

    var showSortByDialog by remember { mutableStateOf(false) }
    val refreshError = items.loadState.refresh as? LoadState.Error

    LazyVerticalGrid(
        columns = GridCells.Fixed(if (isMusicLibrary) 1 else NON_MUSIC_LIBRARY_GRID_COLUMNS),
        horizontalArrangement = Arrangement.spacedBy(gridSpacing),
        verticalArrangement = Arrangement.spacedBy(gridSpacing),
        contentPadding = gridContentPadding,
        modifier = Modifier.fillMaxSize().focusRequester(focusRequester),
    ) {
        item(span = { GridItemSpan(this.maxLineSpan) }) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = libraryName, style = MaterialTheme.typography.displayMedium)
                Button(onClick = { showSortByDialog = true }) {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_arrow_down_up),
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(text = stringResource(CoreR.string.sort_by))
                }
            }
        }
        if (isMusicLibrary) {
            item(span = { GridItemSpan(this.maxLineSpan) }) {
                MusicListColumnsHeader()
            }
        }
        if (items.itemCount == 0 && (refreshError != null || state.error != null) && !state.isLoading) {
            item(span = { GridItemSpan(this.maxLineSpan) }) {
                StatusContent(
                    title = stringResource(CoreR.string.error_loading_data),
                    message =
                        (refreshError?.error ?: state.error)?.localizedMessage
                            ?: stringResource(CoreR.string.unknown_error),
                    actionLabel = stringResource(CoreR.string.retry),
                    onAction = {
                        if (refreshError != null) {
                            items.retry()
                        } else {
                            onRetry()
                        }
                    },
                )
            }
        }
        if (
            items.itemCount == 0 &&
                items.loadState.refresh is LoadState.NotLoading &&
                state.error == null &&
                !state.isLoading
        ) {
            item(span = { GridItemSpan(this.maxLineSpan) }) {
                Text(
                    text = stringResource(CoreR.string.library_no_media),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                )
            }
        }
        items(items.itemCount) { i ->
            val item = items[i]
            item?.let {
                if (isMusicLibrary) {
                    MusicListItem(
                        index = i,
                        item = item,
                        onClick = { onAction(LibraryAction.OnItemClick(item)) },
                        modifier = Modifier.animateItem(),
                    )
                } else {
                    ItemCard(
                        item = item,
                        direction = Direction.VERTICAL,
                        cardWidthDp = LIBRARY_CARD_WIDTH_DP,
                        onClick = { onAction(LibraryAction.OnItemClick(item)) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    if (showSortByDialog) {
        SortByDialog(
            currentSortBy = state.sortBy,
            currentSortOrder = state.sortOrder,
            onUpdate = { sortBy, sortOrder ->
                onAction(LibraryAction.ChangeSorting(sortBy, sortOrder))
            },
            onDismissRequest = { showSortByDialog = false },
        )
    }

    LaunchedEffect(items.itemCount > 0) {
        if (items.itemCount > 0) {
            focusRequester.requestFocus()
        }
    }
}

private const val NON_MUSIC_LIBRARY_GRID_COLUMNS = 6
private const val LIBRARY_CARD_WIDTH_DP = 116

@Preview(device = "id:tv_1080p")
@Composable
private fun LibraryScreenLayoutPreview() {
    val items: Flow<PagingData<FindroidItem>> = flowOf(PagingData.from(dummyMovies))
    FindroidTheme {
        LibraryScreenLayout(
            libraryName = "Movies",
            libraryType = CollectionType.Movies,
            state = LibraryState(items = items),
            onRetry = {},
            onAction = {},
        )
    }
}

private fun resolvePlayableKind(item: FindroidMovie, libraryType: CollectionType): BaseItemKind {
    val hasVideoStream =
        item.sources.any { source ->
            source.mediaStreams.any { stream -> stream.type == MediaStreamType.VIDEO }
        }

    return when (libraryType) {
        CollectionType.Music -> BaseItemKind.AUDIO
        CollectionType.MusicVideos -> if (hasVideoStream) BaseItemKind.MUSIC_VIDEO else BaseItemKind.AUDIO
        CollectionType.HomeVideos -> if (hasVideoStream) BaseItemKind.VIDEO else BaseItemKind.AUDIO
        else -> if (hasVideoStream) BaseItemKind.VIDEO else BaseItemKind.AUDIO
    }
}
