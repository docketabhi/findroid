package dev.jdtech.jellyfin.film.presentation.episode

import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItemPerson
import dev.jdtech.jellyfin.models.VideoMetadata
import dev.jdtech.jellyfin.utils.PlaybackKind

data class EpisodeState(
    val episode: FindroidEpisode? = null,
    val videoMetadata: VideoMetadata? = null,
    val playbackKind: PlaybackKind? = null,
    val actors: List<FindroidItemPerson> = emptyList(),
    val displayExtraInfo: Boolean = false,
    val error: Exception? = null,
)
