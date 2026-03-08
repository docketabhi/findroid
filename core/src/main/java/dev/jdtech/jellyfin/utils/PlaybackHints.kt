package dev.jdtech.jellyfin.utils

import androidx.annotation.StringRes
import dev.jdtech.jellyfin.core.R
import java.util.Locale

enum class PlaybackKind(@StringRes val labelRes: Int) {
    LOCAL(R.string.playback_local),
    DIRECT_PLAY(R.string.playback_direct_play),
    DIRECT_STREAM(R.string.playback_direct_stream),
    TRANSCODE(R.string.playback_transcode),
    STREAMING(R.string.playback_streaming),
}

fun inferPlaybackKind(path: String?, isLocalSource: Boolean = false): PlaybackKind? {
    if (isLocalSource) return PlaybackKind.LOCAL
    if (path.isNullOrBlank()) return null

    val normalizedPath = path.lowercase(Locale.US)

    return when {
        normalizedPath.startsWith("file:") || normalizedPath.startsWith("/") -> PlaybackKind.LOCAL
        "transcod" in normalizedPath ||
            "universalvideos" in normalizedPath ||
            normalizedPath.endsWith(".m3u8") ||
            "master.m3u8" in normalizedPath -> PlaybackKind.TRANSCODE
        "static=true" in normalizedPath -> PlaybackKind.DIRECT_PLAY
        "/videos/" in normalizedPath && "/stream" in normalizedPath -> PlaybackKind.DIRECT_PLAY
        "/audio/" in normalizedPath || "/stream" in normalizedPath -> PlaybackKind.DIRECT_STREAM
        normalizedPath.startsWith("http://") || normalizedPath.startsWith("https://") ->
            PlaybackKind.STREAMING
        else -> null
    }
}
