package dev.jdtech.jellyfin.ui.components

import android.text.format.Formatter
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.dummy.dummyVideoMetadata
import dev.jdtech.jellyfin.models.AudioCodec
import dev.jdtech.jellyfin.models.DisplayProfile
import dev.jdtech.jellyfin.models.VideoMetadata
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.utils.PlaybackKind

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun VideoMetadataBar(
    videoMetadata: VideoMetadata,
    playbackKind: PlaybackKind? = null,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        playbackKind?.let {
            VideoMetadataBarItem(text = stringResource(id = it.labelRes))
        }
        videoMetadata.resolution.firstOrNull()?.apply { VideoMetadataBarItem(text = this.raw) }
        videoMetadata.videoCodecs.firstOrNull()?.apply { VideoMetadataBarItem(text = this.raw) }
        videoMetadata.displayProfiles.firstOrNull()?.apply {
            val icon =
                when (this) {
                    DisplayProfile.DOLBY_VISION -> CoreR.drawable.ic_dolby
                    else -> null
                }
            VideoMetadataBarItem(text = this.raw, icon = icon)
        }
        videoMetadata.audioCodecs.firstOrNull()?.apply {
            val icon =
                when (this) {
                    AudioCodec.AC3,
                    AudioCodec.EAC3,
                    AudioCodec.TRUEHD -> CoreR.drawable.ic_dolby
                    else -> null
                }
            VideoMetadataBarItem(text = this.raw, icon = icon)
        }
        videoMetadata.audioChannels.firstOrNull()?.apply { VideoMetadataBarItem(text = this.raw) }
    }
}

@Composable
fun VideoMetadataBarItem(
    text: String,
    @DrawableRes icon: Int? = null,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Row(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                .padding(
                    horizontal = MaterialTheme.spacings.small,
                    vertical = MaterialTheme.spacings.extraSmall,
                ),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(painter = painterResource(icon), contentDescription = null)
        }
        Text(text = text, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
fun ExtraInfoText(videoMetadata: VideoMetadata, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
    ) {
        TechnicalLine(
            label = stringResource(CoreR.string.size),
            value = Formatter.formatFileSize(context, videoMetadata.size),
        )
        if (videoMetadata.videoTracks.isNotEmpty()) {
            TechnicalLine(
                label = stringResource(CoreR.string.video),
                value = videoMetadata.videoTracks.joinToString(),
            )
        }
        if (videoMetadata.audioTracks.isNotEmpty()) {
            TechnicalLine(
                label = stringResource(CoreR.string.audio),
                value = videoMetadata.audioTracks.joinToString(),
            )
        }
        if (videoMetadata.subtitleTracks.isNotEmpty()) {
            TechnicalLine(
                label = stringResource(CoreR.string.subtitle),
                value = videoMetadata.subtitleTracks.joinToString(),
            )
        }
    }
}

@Composable
private fun TechnicalLine(label: String, value: String) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.84f),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@androidx.compose.ui.tooling.preview.Preview(device = "id:tv_1080p")
@Composable
private fun TechnicalDetailsPreview() {
    FindroidTheme {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
            VideoMetadataBar(videoMetadata = dummyVideoMetadata, playbackKind = PlaybackKind.DIRECT_PLAY)
            ExtraInfoText(videoMetadata = dummyVideoMetadata)
        }
    }
}
