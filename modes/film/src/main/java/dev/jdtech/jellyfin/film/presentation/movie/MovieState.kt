package dev.jdtech.jellyfin.film.presentation.movie

import dev.jdtech.jellyfin.models.FindroidItemPerson
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.VideoMetadata
import dev.jdtech.jellyfin.utils.PlaybackKind

data class MovieState(
    val movie: FindroidMovie? = null,
    val videoMetadata: VideoMetadata? = null,
    val playbackKind: PlaybackKind? = null,
    val actors: List<FindroidItemPerson> = emptyList(),
    val director: FindroidItemPerson? = null,
    val writers: List<FindroidItemPerson> = emptyList(),
    val displayExtraInfo: Boolean = false,
    val error: Exception? = null,
)
