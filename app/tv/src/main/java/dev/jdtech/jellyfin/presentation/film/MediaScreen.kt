package dev.jdtech.jellyfin.presentation.film

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.MaterialTheme
import dev.jdtech.jellyfin.core.presentation.dummy.dummyCollections
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.presentation.media.MediaAction
import dev.jdtech.jellyfin.film.presentation.media.MediaState
import dev.jdtech.jellyfin.film.presentation.media.MediaViewModel
import dev.jdtech.jellyfin.models.CollectionType
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.ui.components.Direction
import dev.jdtech.jellyfin.ui.components.ItemCard
import dev.jdtech.jellyfin.ui.components.StatusContent
import java.util.UUID

@Composable
fun MediaScreen(
    navigateToLibrary: (libraryId: UUID, libraryName: String, libraryType: CollectionType) -> Unit,
    isLoading: (Boolean) -> Unit,
    firstContentFocusRequester: FocusRequester? = null,
    viewModel: MediaViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var preferredLibraryId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(true) { viewModel.loadData() }

    LaunchedEffect(state.isLoading) { isLoading(state.isLoading) }

    LibrariesScreenLayout(
        state = state,
        firstContentFocusRequester = firstContentFocusRequester,
        preferredLibraryId = preferredLibraryId?.let(UUID::fromString),
        onAction = { action ->
            when (action) {
                is MediaAction.OnItemClick -> {
                    preferredLibraryId = action.item.id.toString()
                    navigateToLibrary(action.item.id, action.item.name, action.item.type)
                }
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

@Composable
private fun LibrariesScreenLayout(
    state: MediaState,
    onAction: (MediaAction) -> Unit,
    firstContentFocusRequester: FocusRequester? = null,
    preferredLibraryId: UUID? = null,
) {
    val focusRequester = firstContentFocusRequester ?: remember { FocusRequester() }
    val targetLibraryId =
        if (preferredLibraryId != null && state.libraries.any { it.id == preferredLibraryId }) {
            preferredLibraryId
        } else {
            state.libraries.firstOrNull()?.id
        }

    LaunchedEffect(targetLibraryId, state.libraries.size) {
        if (targetLibraryId != null) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(LIBRARIES_GRID_COLUMNS),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.large),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.large),
        contentPadding =
            PaddingValues(
                start = MaterialTheme.spacings.large,
                top = MaterialTheme.spacings.small,
                end = MaterialTheme.spacings.large,
                bottom = MaterialTheme.spacings.large,
            ),
        modifier = Modifier,
    ) {
        if (state.error != null && state.libraries.isEmpty() && !state.isLoading) {
            item(span = { GridItemSpan(this.maxLineSpan) }) {
                StatusContent(
                    title = stringResource(CoreR.string.error_loading_data),
                    message =
                        state.error?.localizedMessage
                            ?: stringResource(CoreR.string.unknown_error),
                    actionLabel = stringResource(CoreR.string.retry),
                    onAction = { onAction(MediaAction.OnRetryClick) },
                )
            }
        }
        itemsIndexed(state.libraries, key = { _, library -> library.id }) { index, library ->
            ItemCard(
                item = library,
                direction = Direction.HORIZONTAL,
                cardWidthDp = LIBRARIES_CARD_WIDTH_DP,
                onClick = { onAction(MediaAction.OnItemClick(library)) },
                surfaceModifier =
                    if ((targetLibraryId == null && index == 0) || library.id == targetLibraryId) {
                        Modifier.focusRequester(focusRequester)
                    } else {
                        Modifier
                    },
            )
        }
    }
}

private const val LIBRARIES_GRID_COLUMNS = 5
private const val LIBRARIES_CARD_WIDTH_DP = 170

@Preview(device = "id:tv_1080p")
@Composable
private fun LibrariesScreenLayoutPreview() {
    FindroidTheme {
        LibrariesScreenLayout(state = MediaState(libraries = dummyCollections), onAction = {})
    }
}
