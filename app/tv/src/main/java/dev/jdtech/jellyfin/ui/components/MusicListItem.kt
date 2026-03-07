package dev.jdtech.jellyfin.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ClickableSurfaceScale
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.core.presentation.dummy.dummyMovie
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme

@Composable
fun MusicListColumnsHeader(modifier: Modifier = Modifier) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MUSIC_ROW_HORIZONTAL_PADDING_DP.dp,
                    vertical = MUSIC_HEADER_VERTICAL_PADDING_DP.dp,
                ),
        horizontalArrangement = Arrangement.spacedBy(MUSIC_ROW_GAP_DP.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "#",
            style = MaterialTheme.typography.labelMedium,
            color = SpotifyMuted,
            modifier = Modifier.width(MUSIC_TRACK_NUMBER_WIDTH_DP.dp),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.size(MUSIC_THUMBNAIL_SIZE_DP.dp))
        Text(
            text = "Title",
            style = MaterialTheme.typography.labelMedium,
            color = SpotifyMuted,
            modifier = Modifier.weight(1.5f),
        )
        Text(
            text = "Album",
            style = MaterialTheme.typography.labelMedium,
            color = SpotifyMuted,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "Time",
            style = MaterialTheme.typography.labelMedium,
            color = SpotifyMuted,
            modifier = Modifier.width(MUSIC_TIME_WIDTH_DP.dp),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
fun MusicListItem(
    index: Int,
    item: FindroidItem,
    onClick: (FindroidItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackNumber = (index + 1).toString()
    val metadata = trackMetadata(item)
    val imageUri = item.images.primary ?: item.images.backdrop

    Surface(
        onClick = { onClick(item) },
        modifier = modifier.fillMaxWidth(),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = SpotifyFocusContainer,
            ),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
        border =
            ClickableSurfaceDefaults.border(
                focusedBorder =
                    Border(
                        BorderStroke(2.dp, SpotifyGreen),
                        shape = RoundedCornerShape(8.dp),
                    )
            ),
        scale = ClickableSurfaceScale.None,
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .heightIn(min = MUSIC_ROW_MIN_HEIGHT_DP.dp)
                    .padding(
                        horizontal = MUSIC_ROW_HORIZONTAL_PADDING_DP.dp,
                        vertical = MUSIC_ROW_VERTICAL_PADDING_DP.dp,
                    ),
            horizontalArrangement = Arrangement.spacedBy(MUSIC_ROW_GAP_DP.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = trackNumber,
                style = MaterialTheme.typography.bodyMedium,
                color = SpotifyMuted,
                modifier = Modifier.width(MUSIC_TRACK_NUMBER_WIDTH_DP.dp),
                textAlign = TextAlign.Center,
            )

            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = null,
                    modifier =
                        Modifier.size(MUSIC_THUMBNAIL_SIZE_DP.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            } else {
                DiscPlaceholderThumbnail(modifier = Modifier.size(MUSIC_THUMBNAIL_SIZE_DP.dp))
            }

            Column(modifier = Modifier.weight(1.5f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (metadata.subtitle.isNotBlank()) {
                    Text(
                        text = metadata.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = SpotifyMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Text(
                text = metadata.album,
                style = MaterialTheme.typography.bodySmall,
                color = SpotifyMuted,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatTrackDuration(item.runtimeTicks),
                style = MaterialTheme.typography.bodySmall,
                color = SpotifyMuted,
                modifier = Modifier.width(MUSIC_TIME_WIDTH_DP.dp),
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun DiscPlaceholderThumbnail(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier.fillMaxSize(0.72f)
                    .clip(CircleShape)
                    .background(SpotifyDiscOuter),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier.fillMaxSize(0.72f)
                        .clip(CircleShape)
                        .background(SpotifyDiscInner),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxSize(0.25f)
                            .clip(CircleShape)
                            .background(SpotifyDiscHole)
                )
            }
        }
    }
}

private data class TrackMetadata(
    val subtitle: String,
    val album: String,
)

private fun trackMetadata(item: FindroidItem): TrackMetadata {
    if (item !is FindroidMovie) {
        return TrackMetadata(subtitle = "", album = "Single")
    }

    val artist = item.albumArtist ?: item.artists.firstOrNull()
    val album = item.album ?: "Single"
    val subtitle =
        when {
            !artist.isNullOrBlank() && !item.album.isNullOrBlank() -> "$artist • ${item.album}"
            !artist.isNullOrBlank() -> artist
            !item.album.isNullOrBlank() -> item.album.orEmpty()
            else -> ""
        }
    return TrackMetadata(subtitle = subtitle, album = album)
}

private fun formatTrackDuration(runtimeTicks: Long): String {
    if (runtimeTicks <= 0) return "--:--"
    val totalSeconds = runtimeTicks / 10_000_000L
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

private val SpotifyGreen = Color(0xFF1DB954)
private val SpotifyMuted = Color(0xFFB3B3B3)
private val SpotifyFocusContainer = Color(0x33228B55)
private val SpotifyDiscOuter = Color(0xFF2A2A2A)
private val SpotifyDiscInner = Color(0xFF1E1E1E)
private val SpotifyDiscHole = Color(0xFF9A9A9A)

private const val MUSIC_ROW_MIN_HEIGHT_DP = 72
private const val MUSIC_ROW_HORIZONTAL_PADDING_DP = 10
private const val MUSIC_ROW_VERTICAL_PADDING_DP = 8
private const val MUSIC_HEADER_VERTICAL_PADDING_DP = 4
private const val MUSIC_ROW_GAP_DP = 10
private const val MUSIC_TRACK_NUMBER_WIDTH_DP = 26
private const val MUSIC_THUMBNAIL_SIZE_DP = 48
private const val MUSIC_TIME_WIDTH_DP = 56

@Preview
@Composable
private fun MusicListItemPreview() {
    FindroidTheme {
        MusicListItem(index = 0, item = dummyMovie, onClick = {}, modifier = Modifier.padding(24.dp))
    }
}
