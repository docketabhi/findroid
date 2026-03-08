package dev.jdtech.jellyfin.presentation.film

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.dummy.dummyMovies
import dev.jdtech.jellyfin.film.presentation.collection.CollectionAction
import dev.jdtech.jellyfin.film.presentation.collection.CollectionState
import dev.jdtech.jellyfin.film.presentation.collection.CollectionViewModel
import dev.jdtech.jellyfin.models.CollectionSection
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.ui.components.Direction
import dev.jdtech.jellyfin.ui.components.ItemCard
import dev.jdtech.jellyfin.ui.components.StatusContent
import java.util.UUID

@Composable
fun CollectionScreen(
    collectionId: UUID,
    collectionName: String,
    onItemClick: (item: FindroidItem) -> Unit,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) { viewModel.loadItems(collectionId) }

    CollectionScreenLayout(
        collectionName = collectionName,
        state = state,
        onRetry = { viewModel.loadItems(collectionId) },
        onAction = { action ->
            when (action) {
                is CollectionAction.OnItemClick -> onItemClick(action.item)
                is CollectionAction.OnBackClick -> Unit
            }
        },
    )
}

@Composable
internal fun CollectionScreenLayout(
    collectionName: String,
    state: CollectionState,
    firstContentFocusRequester: FocusRequester? = null,
    onRetry: (() -> Unit)? = null,
    onAction: (CollectionAction) -> Unit,
) {
    val focusRequester = firstContentFocusRequester ?: remember { FocusRequester() }
    val firstItemId =
        state.sections.asSequence().flatMap { section -> section.items.asSequence() }.firstOrNull()?.id

    Box(modifier = Modifier.fillMaxSize()) {
        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (state.error != null && state.sections.isEmpty()) {
            StatusContent(
                title = stringResource(CoreR.string.error_loading_data),
                message =
                    state.error?.localizedMessage
                        ?: stringResource(CoreR.string.unknown_error),
                actionLabel = onRetry?.let { stringResource(CoreR.string.retry) },
                onAction = onRetry,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(COLLECTION_GRID_COLUMNS),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
                contentPadding =
                    PaddingValues(
                        horizontal = MaterialTheme.spacings.default * 2,
                        vertical = MaterialTheme.spacings.large,
                    ),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(span = { GridItemSpan(this.maxLineSpan) }) {
                    Text(text = collectionName, style = MaterialTheme.typography.displayMedium)
                }
                if (state.sections.isEmpty()) {
                    item(span = { GridItemSpan(this.maxLineSpan) }) {
                        Text(
                            text = stringResource(CoreR.string.collection_no_media),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                        )
                    }
                }
                state.sections.forEach { section ->
                    item(span = { GridItemSpan(this.maxLineSpan) }) {
                        Text(
                            text = section.name.asString(),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                    items(section.items, key = { it.id }) { item ->
                        val direction =
                            if (item is FindroidEpisode) Direction.HORIZONTAL else Direction.VERTICAL
                        ItemCard(
                            item = item,
                            direction = direction,
                            cardWidthDp =
                                if (direction == Direction.HORIZONTAL) {
                                    COLLECTION_HORIZONTAL_CARD_WIDTH_DP
                                } else {
                                    COLLECTION_VERTICAL_CARD_WIDTH_DP
                                },
                            onClick = { onAction(CollectionAction.OnItemClick(item)) },
                            surfaceModifier =
                                if (item.id == firstItemId) {
                                    Modifier.focusRequester(focusRequester)
                                } else {
                                    Modifier
                                },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }

            LaunchedEffect(firstItemId) {
                if (firstItemId != null) {
                    runCatching { focusRequester.requestFocus() }
                }
            }
        }
    }
}

private const val COLLECTION_GRID_COLUMNS = 6
private const val COLLECTION_VERTICAL_CARD_WIDTH_DP = 116
private const val COLLECTION_HORIZONTAL_CARD_WIDTH_DP = 170

@Preview(device = "id:tv_1080p")
@Composable
private fun CollectionScreenLayoutPreview() {
    FindroidTheme {
        CollectionScreenLayout(
            collectionName = "Marvel",
            state =
                CollectionState(
                    sections =
                        listOf(
                            CollectionSection(
                                id = 0,
                                name = UiText.StringResource(CoreR.string.movies_label),
                                items = dummyMovies,
                            )
                        )
                ),
            onRetry = {},
            onAction = {},
        )
    }
}
