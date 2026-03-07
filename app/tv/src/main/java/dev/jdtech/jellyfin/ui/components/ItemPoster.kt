package dev.jdtech.jellyfin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.CircleShape
import androidx.tv.material3.MaterialTheme
import coil3.compose.AsyncImage
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import org.jellyfin.sdk.model.api.MediaStreamType

enum class Direction {
    HORIZONTAL,
    VERTICAL,
}

@Composable
fun ItemPoster(item: FindroidItem, direction: Direction, modifier: Modifier = Modifier) {
    val imageUri = resolvePosterUri(item, direction)
    val posterModifier =
        modifier
            .fillMaxWidth()
            .aspectRatio(if (direction == Direction.HORIZONTAL) 1.77f else 0.66f)
            .background(MaterialTheme.colorScheme.surface)

    if (imageUri != null) {
        AsyncImage(
            model = imageUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = posterModifier,
        )
    } else if (isAudioLikeItem(item)) {
        Box(modifier = posterModifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Box(
                modifier =
                    Modifier.fillMaxSize(0.64f)
                        .clip(CircleShape)
                        .background(DiscOuter),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxSize(0.68f)
                            .clip(CircleShape)
                            .background(DiscInner),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize(0.22f)
                                .clip(CircleShape)
                                .background(DiscHole)
                    )
                }
            }
        }
    } else {
        Box(modifier = posterModifier)
    }
}

fun resolvePosterUri(item: FindroidItem, direction: Direction): Any? {
    var imageUri = item.images.primary
    when (direction) {
        Direction.HORIZONTAL -> {
            if (item is FindroidMovie) imageUri = item.images.backdrop ?: item.images.primary
        }
        Direction.VERTICAL -> {
            if (item is FindroidEpisode) imageUri = item.images.showPrimary ?: item.images.primary
        }
    }
    return imageUri
}

private fun isAudioLikeItem(item: FindroidItem): Boolean {
    if (item !is FindroidMovie) return false

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

private val DiscOuter = Color(0xFF2A2A2A)
private val DiscInner = Color(0xFF1E1E1E)
private val DiscHole = Color(0xFF9A9A9A)
