package dev.jdtech.jellyfin.presentation.film

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.dummy.dummyMovies
import dev.jdtech.jellyfin.core.presentation.dummy.dummyPersonDetail
import dev.jdtech.jellyfin.film.presentation.person.PersonAction
import dev.jdtech.jellyfin.film.presentation.person.PersonState
import dev.jdtech.jellyfin.film.presentation.person.PersonViewModel
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidPerson
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.ui.components.Direction
import dev.jdtech.jellyfin.ui.components.ItemCard
import java.util.UUID

@Composable
fun PersonScreen(
    personId: UUID,
    navigateToItem: (item: FindroidItem) -> Unit,
    viewModel: PersonViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) { viewModel.loadPerson(personId) }

    PersonScreenLayout(
        state = state,
        onAction = { action ->
            when (action) {
                is PersonAction.NavigateToItem -> navigateToItem(action.item)
                else -> Unit
            }
        },
    )
}

@Composable
private fun PersonScreenLayout(state: PersonState, onAction: (PersonAction) -> Unit) {
    val focusRequester = remember { FocusRequester() }

    Box(modifier = Modifier.fillMaxSize()) {
        state.person?.let { person ->
            LazyColumn(
                contentPadding =
                    PaddingValues(
                        top = MaterialTheme.spacings.large,
                        bottom = MaterialTheme.spacings.large,
                    ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.medium),
            ) {
                item {
                    Row(
                        modifier =
                            Modifier.padding(
                                start = MaterialTheme.spacings.default * 2,
                                end = MaterialTheme.spacings.default * 2,
                            ),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.large),
                    ) {
                        PersonImage(person)
                        Column(
                            verticalArrangement =
                                Arrangement.spacedBy(MaterialTheme.spacings.medium),
                        ) {
                            Text(
                                text = person.name,
                                style = MaterialTheme.typography.displayMedium,
                            )
                            if (person.overview.isNotBlank()) {
                                Text(
                                    text = person.overview,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 6,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(640.dp),
                                )
                            }
                        }
                    }
                }

                if (state.starredInMovies.isNotEmpty()) {
                    item {
                        Column {
                            Text(
                                text = stringResource(CoreR.string.movies_label),
                                style = MaterialTheme.typography.headlineMedium,
                                modifier =
                                    Modifier.padding(
                                        horizontal = MaterialTheme.spacings.default * 2,
                                    ),
                            )
                            Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
                            LazyRow(
                                horizontalArrangement =
                                    Arrangement.spacedBy(MaterialTheme.spacings.default),
                                contentPadding =
                                    PaddingValues(
                                        horizontal = MaterialTheme.spacings.default * 2,
                                    ),
                                modifier = Modifier.focusRequester(focusRequester),
                            ) {
                                items(state.starredInMovies, key = { it.id }) { item ->
                                    ItemCard(
                                        item = item,
                                        direction = Direction.VERTICAL,
                                        onClick = { onAction(PersonAction.NavigateToItem(item)) },
                                    )
                                }
                            }
                        }
                    }
                }

                if (state.starredInShows.isNotEmpty()) {
                    item {
                        Column {
                            Text(
                                text = stringResource(CoreR.string.shows_label),
                                style = MaterialTheme.typography.headlineMedium,
                                modifier =
                                    Modifier.padding(
                                        horizontal = MaterialTheme.spacings.default * 2,
                                    ),
                            )
                            Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
                            LazyRow(
                                horizontalArrangement =
                                    Arrangement.spacedBy(MaterialTheme.spacings.default),
                                contentPadding =
                                    PaddingValues(
                                        horizontal = MaterialTheme.spacings.default * 2,
                                    ),
                                modifier =
                                    if (state.starredInMovies.isEmpty()) {
                                        Modifier.focusRequester(focusRequester)
                                    } else {
                                        Modifier
                                    },
                            ) {
                                items(state.starredInShows, key = { it.id }) { item ->
                                    ItemCard(
                                        item = item,
                                        direction = Direction.VERTICAL,
                                        onClick = { onAction(PersonAction.NavigateToItem(item)) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            LaunchedEffect(state.starredInMovies, state.starredInShows) {
                if (state.starredInMovies.isNotEmpty() || state.starredInShows.isNotEmpty()) {
                    focusRequester.requestFocus()
                }
            }
        } ?: run { CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) }
    }
}

@Composable
private fun PersonImage(person: FindroidPerson, modifier: Modifier = Modifier) {
    AsyncImage(
        model = person.images.primary,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier =
            modifier
                .height(320.dp)
                .aspectRatio(0.66f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

@Preview(device = "id:tv_1080p")
@Composable
private fun PersonScreenLayoutPreview() {
    FindroidTheme {
        PersonScreenLayout(
            state = PersonState(person = dummyPersonDetail, starredInMovies = dummyMovies),
            onAction = {},
        )
    }
}
