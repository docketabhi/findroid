package dev.jdtech.jellyfin.presentation.film

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.presentation.search.SearchAction
import dev.jdtech.jellyfin.film.presentation.search.SearchViewModel
import dev.jdtech.jellyfin.models.FindroidBoxSet
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.ui.components.Direction
import dev.jdtech.jellyfin.ui.components.ItemCard
import dev.jdtech.jellyfin.ui.components.LoadingIndicator
import dev.jdtech.jellyfin.ui.components.StatusContent
import java.util.UUID
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaStreamType

@Composable
fun SearchScreen(
    navigateToCollection: (collectionId: UUID, collectionName: String) -> Unit,
    navigateToMovie: (itemId: UUID) -> Unit,
    navigateToShow: (itemId: UUID) -> Unit,
    navigateToPlayer: (itemId: UUID, itemKind: BaseItemKind) -> Unit,
    firstContentFocusRequester: FocusRequester? = null,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val fieldFocusRequester = firstContentFocusRequester ?: remember { FocusRequester() }
    val supportedResults = state.items.filter(::supportsSearchNavigation)

    Column(
        modifier = Modifier.fillMaxSize().padding(MaterialTheme.spacings.large),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { value ->
                query = value
                viewModel.onAction(SearchAction.Search(value))
            },
            modifier = Modifier.fillMaxWidth().focusRequester(fieldFocusRequester),
            placeholder = { androidx.compose.material3.Text(text = stringResource(CoreR.string.search_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        )

        when {
            query.isBlank() -> {
                StatusContent(
                    title = stringResource(CoreR.string.search),
                    message = stringResource(CoreR.string.search_empty_prompt),
                    modifier = Modifier.padding(top = MaterialTheme.spacings.large),
                )
            }
            state.loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            }
            supportedResults.isEmpty() -> {
                StatusContent(
                    title = stringResource(CoreR.string.no_search_results),
                    message = stringResource(CoreR.string.search_no_matches, query),
                    modifier = Modifier.padding(top = MaterialTheme.spacings.large),
                )
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
                    contentPadding = PaddingValues(bottom = MaterialTheme.spacings.large),
                ) {
                    items(supportedResults, key = { it.id }) { item ->
                        ItemCard(
                            item = item,
                            direction =
                                if (item is FindroidEpisode) Direction.HORIZONTAL else Direction.VERTICAL,
                            cardWidthDp =
                                if (item is FindroidEpisode) 170 else 116,
                            onClick = {
                                when (item) {
                                    is FindroidBoxSet -> navigateToCollection(item.id, item.name)
                                    is FindroidMovie -> {
                                        if (isAudioLikeSearchItem(item)) {
                                            navigateToPlayer(item.id, BaseItemKind.AUDIO)
                                        } else {
                                            navigateToMovie(item.id)
                                        }
                                    }
                                    is FindroidShow -> navigateToShow(item.id)
                                    is FindroidEpisode -> navigateToPlayer(item.id, BaseItemKind.EPISODE)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun supportsSearchNavigation(item: FindroidItem): Boolean {
    return item is FindroidBoxSet ||
        item is FindroidMovie ||
        item is FindroidShow ||
        item is FindroidEpisode
}

private fun isAudioLikeSearchItem(item: FindroidMovie): Boolean {
    val hasMusicMetadata =
        !item.album.isNullOrBlank() ||
            !item.albumArtist.isNullOrBlank() ||
            item.artists.isNotEmpty()
    val hasAudioStream =
        item.sources.any { source ->
            source.mediaStreams.any { stream -> stream.type == MediaStreamType.AUDIO }
        }
    val hasVideoStream =
        item.sources.any { source ->
            source.mediaStreams.any { stream -> stream.type == MediaStreamType.VIDEO }
        }

    return hasMusicMetadata || (hasAudioStream && !hasVideoStream)
}
