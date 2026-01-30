package dev.jdtech.jellyfin.presentation.film

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.dummy.dummyEpisode
import dev.jdtech.jellyfin.core.presentation.theme.Yellow
import dev.jdtech.jellyfin.film.presentation.episode.EpisodeAction
import dev.jdtech.jellyfin.film.presentation.episode.EpisodeState
import dev.jdtech.jellyfin.film.presentation.episode.EpisodeViewModel
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.utils.format
import java.util.UUID

@Composable
fun EpisodeScreen(
    episodeId: UUID,
    navigateToPlayer: (itemId: UUID) -> Unit,
    viewModel: EpisodeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) { viewModel.loadEpisode(episodeId = episodeId) }

    EpisodeScreenLayout(
        state = state,
        onAction = { action ->
            when (action) {
                is EpisodeAction.Play -> {
                    navigateToPlayer(episodeId)
                }
                else -> Unit
            }
            viewModel.onAction(action)
        },
    )
}

@Composable
private fun EpisodeScreenLayout(state: EpisodeState, onAction: (EpisodeAction) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val configuration = LocalConfiguration.current
    val locale = configuration.locales.get(0)

    Box(modifier = Modifier.fillMaxSize()) {
        state.episode?.let { episode ->
            var size by remember { mutableStateOf(Size.Zero) }
            Box(
                modifier =
                    Modifier.fillMaxSize().onGloballyPositioned { coordinates ->
                        size = coordinates.size.toSize()
                    }
            ) {
                AsyncImage(
                    model = episode.images.backdrop ?: episode.images.primary,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (size != Size.Zero) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color.Black.copy(alpha = .2f), Color.Black),
                                        center = Offset(size.width, 0f),
                                        radius = size.width * .8f,
                                    )
                                )
                    )
                }
                Column(
                    modifier =
                        Modifier.padding(
                            start = MaterialTheme.spacings.default * 2,
                            top = 112.dp,
                            end = MaterialTheme.spacings.default * 2,
                        )
                ) {
                    val seasonName =
                        episode.seasonName
                            ?: run {
                                stringResource(
                                    CoreR.string.season_number,
                                    episode.parentIndexNumber,
                                )
                            }
                    Text(
                        text =
                            "$seasonName - " +
                                stringResource(
                                    id = CoreR.string.episode_number,
                                    episode.indexNumber,
                                ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = episode.name,
                        style = MaterialTheme.typography.displayMedium,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2,
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacings.small))
                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(MaterialTheme.spacings.small)
                    ) {
                        episode.premiereDate?.let { premiereDate ->
                            Text(
                                text = premiereDate.format(),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        Text(
                            text =
                                stringResource(
                                    CoreR.string.runtime_minutes,
                                    episode.runtimeTicks.div(600000000),
                                ),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        episode.communityRating?.let { communityRating ->
                            Row {
                                Icon(
                                    painter = painterResource(id = CoreR.drawable.ic_star),
                                    contentDescription = null,
                                    tint = Yellow,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(
                                    modifier =
                                        Modifier.width(MaterialTheme.spacings.extraSmall)
                                )
                                Text(
                                    text = String.format(locale, "%.1f", communityRating),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
                    Text(
                        text = episode.overview,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(640.dp),
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacings.default))
                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(MaterialTheme.spacings.medium)
                    ) {
                        Button(
                            onClick = { onAction(EpisodeAction.Play()) },
                            modifier = Modifier.focusRequester(focusRequester),
                        ) {
                            Icon(
                                painter = painterResource(id = CoreR.drawable.ic_play),
                                contentDescription = null,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = stringResource(id = CoreR.string.play))
                        }
                        Button(
                            onClick = {
                                when (episode.played) {
                                    true -> onAction(EpisodeAction.UnmarkAsPlayed)
                                    false -> onAction(EpisodeAction.MarkAsPlayed)
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = CoreR.drawable.ic_check),
                                contentDescription = null,
                                tint =
                                    if (episode.played) Color.Red
                                    else LocalContentColor.current,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text =
                                    stringResource(
                                        id =
                                            if (episode.played) CoreR.string.unmark_as_played
                                            else CoreR.string.mark_as_played
                                    )
                            )
                        }
                        Button(
                            onClick = {
                                when (episode.favorite) {
                                    true -> onAction(EpisodeAction.UnmarkAsFavorite)
                                    false -> onAction(EpisodeAction.MarkAsFavorite)
                                }
                            }
                        ) {
                            Icon(
                                painter =
                                    painterResource(
                                        id =
                                            if (episode.favorite)
                                                CoreR.drawable.ic_heart_filled
                                            else CoreR.drawable.ic_heart
                                    ),
                                contentDescription = null,
                                tint =
                                    if (episode.favorite) Color.Red
                                    else LocalContentColor.current,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text =
                                    stringResource(
                                        id =
                                            if (episode.favorite)
                                                CoreR.string.remove_from_favorites
                                            else CoreR.string.add_to_favorites
                                    )
                            )
                        }
                    }
                }
            }

            LaunchedEffect(true) { focusRequester.requestFocus() }
        } ?: run { CircularProgressIndicator(modifier = Modifier.align(Alignment.Center)) }
    }
}

@Preview(device = "id:tv_1080p")
@Composable
private fun EpisodeScreenLayoutPreview() {
    FindroidTheme {
        EpisodeScreenLayout(state = EpisodeState(episode = dummyEpisode), onAction = {})
    }
}
