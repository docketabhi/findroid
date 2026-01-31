package dev.jdtech.jellyfin.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerView
import android.widget.Toast
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import dev.jdtech.jellyfin.core.R
import dev.jdtech.jellyfin.player.core.domain.models.Track
import dev.jdtech.jellyfin.player.local.presentation.PlayerViewModel
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.ui.components.player.VideoPlayerControlsLayout
import dev.jdtech.jellyfin.ui.components.player.VideoPlayerMediaButton
import dev.jdtech.jellyfin.ui.components.player.VideoPlayerMediaTitle
import dev.jdtech.jellyfin.ui.components.player.VideoPlayerOverlay
import dev.jdtech.jellyfin.ui.components.player.VideoPlayerSeekBar
import dev.jdtech.jellyfin.ui.components.player.VideoPlayerSeeker
import dev.jdtech.jellyfin.ui.components.player.VideoPlayerState
import dev.jdtech.jellyfin.ui.components.player.rememberVideoPlayerState
import dev.jdtech.jellyfin.utils.handleDPadKeyEvents
import java.util.Locale
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    itemId: UUID,
    itemKind: String,
    startFromBeginning: Boolean,
    // resultRecipient: ResultRecipient<VideoPlayerTrackSelectorDialogDestination,
    // VideoPlayerTrackSelectorDialogResult>,
) {
    val viewModel = hiltViewModel<PlayerViewModel>()

    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val currentView = LocalView.current

    // Keep the screen on while player is show
    DisposableEffect(Unit) {
        currentView.keepScreenOn = true
        onDispose { currentView.keepScreenOn = false }
    }

    var lifecycle by remember { mutableStateOf(Lifecycle.Event.ON_CREATE) }
    var mediaSession by remember { mutableStateOf<MediaSession?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            lifecycle = event

            // Handle creation and release of media session
            when (lifecycle) {
                Lifecycle.Event.ON_STOP -> {
                    println("ON_STOP")
                    mediaSession?.release()
                }

                Lifecycle.Event.ON_START -> {
                    println("ON_START")
                    mediaSession = MediaSession.Builder(context, viewModel.player).build()
                }

                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val videoPlayerState = rememberVideoPlayerState()

    var currentPosition by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(viewModel.player.isPlaying) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(300)
            currentPosition = viewModel.player.currentPosition
            isPlaying = viewModel.player.isPlaying
        }
    }

    // TODO: implement the track selection dialogs
    /*
    resultRecipient.onNavResult { result ->
        when (result) {
            is NavResult.Canceled -> Unit
            is NavResult.Value -> {
                val trackType = result.value.trackType
                val index = result.value.index

                if (index == -1) {
                    viewModel.player.trackSelectionParameters = viewModel.player.trackSelectionParameters
                        .buildUpon()
                        .clearOverridesOfType(trackType)
                        .setTrackTypeDisabled(trackType, true)
                        .build()
                } else {
                    viewModel.player.trackSelectionParameters = viewModel.player.trackSelectionParameters
                        .buildUpon()
                        .setOverrideForType(
                            TrackSelectionOverride(viewModel.player.currentTracks.groups[index].mediaTrackGroup, 0),
                        )
                        .setTrackTypeDisabled(trackType, false)
                        .build()
                }
            }
        }
    }
     */

    // Media Segments
    val segment = uiState.currentSegment
    if (segment != null && !videoPlayerState.controlsVisible) {
        val skipButtonFocusRequester = remember { FocusRequester() }

        SkipButton(
            stringRes = uiState.currentSkipButtonStringRes,
            onClick = { viewModel.skipSegment(segment) },
            skipButtonFocusRequester = skipButtonFocusRequester,
        )

        LaunchedEffect(videoPlayerState.controlsVisible) {
            if (!videoPlayerState.controlsVisible) {
                skipButtonFocusRequester.requestFocus()
            }
        }
    }

    // Handle back button
    androidx.activity.compose.BackHandler(enabled = true) {
        if (videoPlayerState.controlsVisible) {
            // If controls are visible, just hide them
            videoPlayerState.hideControls()
        } else {
            // If controls are hidden, stop playback and exit
            viewModel.player.stop()
            (context as? android.app.Activity)?.finish()
        }
    }

    Box(
        modifier =
            Modifier.dPadEvents(exoPlayer = viewModel.player, videoPlayerState = videoPlayerState)
                .focusable(!videoPlayerState.controlsVisible)  // Only focusable when controls are hidden
    ) {
        AndroidView(
            factory = { context ->
                PlayerView(context).also { playerView ->
                    playerView.player = viewModel.player
                    playerView.useController = false
                    viewModel.initializePlayer(
                        itemId = itemId,
                        itemKind = itemKind,
                        startFromBeginning = startFromBeginning,
                    )
                    playerView.setBackgroundColor(
                        context.resources.getColor(android.R.color.black, context.theme)
                    )
                }
            },
            update = {
                when (lifecycle) {
                    Lifecycle.Event.ON_PAUSE -> {
                        it.onPause()
                        it.player?.pause()
                    }

                    Lifecycle.Event.ON_RESUME -> {
                        it.onResume()
                    }

                    else -> Unit
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        val focusRequester = remember { FocusRequester() }
        VideoPlayerOverlay(
            modifier = Modifier.align(Alignment.BottomCenter),
            focusRequester = focusRequester,
            state = videoPlayerState,
            isPlaying = isPlaying,
            controls = {
                VideoPlayerControls(
                    title = uiState.currentItemTitle,
                    isPlaying = isPlaying,
                    contentCurrentPosition = currentPosition,
                    player = viewModel.player,
                    state = videoPlayerState,
                    focusRequester = focusRequester,
                    // navigator = navigator,
                )
            },
        )
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerControls(
    title: String,
    isPlaying: Boolean,
    contentCurrentPosition: Long,
    player: Player,
    state: VideoPlayerState,
    focusRequester: FocusRequester,
    // navigator: DestinationsNavigator,
) {
    val context = LocalContext.current
    val onPlayPauseToggle = { shouldPlay: Boolean ->
        if (shouldPlay) {
            player.play()
        } else {
            player.pause()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Buttons at top: Play/Pause, Audio, Subtitle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Play/Pause button
            VideoPlayerMediaButton(
                icon = if (isPlaying) painterResource(id = R.drawable.ic_pause) else painterResource(id = R.drawable.ic_play),
                state = state,
                isPlaying = isPlaying,
                onClick = { onPlayPauseToggle(!isPlaying) },
            )
            
            Spacer(modifier = Modifier.width(MaterialTheme.spacings.large))
            
            // Audio button
            VideoPlayerMediaButton(
                icon = painterResource(id = R.drawable.ic_speaker),
                state = state,
                isPlaying = isPlaying,
                onClick = {
                    // Cycle through audio tracks
                    val tracks = getTracks(player, C.TRACK_TYPE_AUDIO)
                    val currentIndex = tracks.indexOfFirst { it.selected }
                    val nextIndex = if (currentIndex >= tracks.size - 1) 0 else currentIndex + 1
                    val nextTrack = tracks[nextIndex]
                    
                    if (nextTrack.id >= 0) {
                        switchToTrack(player, C.TRACK_TYPE_AUDIO, nextTrack.id)
                        val trackInfo = nextTrack.language ?: nextTrack.label ?: "Audio Track ${nextTrack.id + 1}"
                        Toast.makeText(context, "Audio: $trackInfo", Toast.LENGTH_SHORT).show()
                    }
                },
            )
            
            Spacer(modifier = Modifier.width(MaterialTheme.spacings.medium))
            
            // Subtitle button
            VideoPlayerMediaButton(
                icon = painterResource(id = R.drawable.ic_closed_caption),
                state = state,
                isPlaying = isPlaying,
                onClick = {
                    // Cycle through subtitle tracks
                    val tracks = getTracks(player, C.TRACK_TYPE_TEXT)
                    val currentIndex = tracks.indexOfFirst { it.selected }
                    val nextIndex = if (currentIndex >= tracks.size - 1) 0 else currentIndex + 1
                    val nextTrack = tracks[nextIndex]
                    
                    switchToTrack(player, C.TRACK_TYPE_TEXT, nextTrack.id)
                    val trackInfo = if (nextTrack.id == -1) {
                        "Subtitles: Off"
                    } else {
                        nextTrack.language ?: nextTrack.label ?: "Subtitle Track ${nextTrack.id + 1}"
                    }
                    Toast.makeText(context, "Subtitles: $trackInfo", Toast.LENGTH_SHORT).show()
                },
            )
        }
        
        Spacer(modifier = Modifier.height(MaterialTheme.spacings.large))
        
        // Seekbar at bottom with time display
        Column(modifier = Modifier.fillMaxWidth()) {
            // Time display - only show when duration is valid
            if (player.duration > 0 && player.duration != androidx.media3.common.C.TIME_UNSET) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacings.small),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatTime(contentCurrentPosition),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = formatTime(player.duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                
                Spacer(modifier = Modifier.height(MaterialTheme.spacings.small))
            }
            
            // Seekbar
            VideoPlayerSeekBar(
                progress = if (player.duration > 0) contentCurrentPosition.toFloat() / player.duration.toFloat() else 0f,
                onSeek = { seekProgress -> 
                    player.seekTo((player.duration * seekProgress).toLong())
                },
                state = state,
                focusRequester = focusRequester,
            )
        }
    }
}

@Composable
private fun SkipButton(
    stringRes: Int,
    onClick: () -> Unit,
    skipButtonFocusRequester: FocusRequester,
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(MaterialTheme.spacings.large).zIndex(1f),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.focusRequester(skipButtonFocusRequester),
            glow =
                ButtonDefaults.glow(
                    focusedGlow = Glow(elevationColor = Color.Gray, elevation = 20.dp)
                ),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_skip_forward),
                contentDescription = null,
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = stringResource(stringRes), color = Color.Black)
        }
    }
}

private fun Modifier.dPadEvents(exoPlayer: Player, videoPlayerState: VideoPlayerState): Modifier =
    this.onPreviewKeyEvent { keyEvent ->
        // Only intercept when controls are HIDDEN
        if (!videoPlayerState.controlsVisible && keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_UP) {
            when (keyEvent.nativeKeyEvent.keyCode) {
                android.view.KeyEvent.KEYCODE_DPAD_UP,
                android.view.KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP -> {
                    // Show controls on UP
                    videoPlayerState.showControls()
                    true
                }
                android.view.KeyEvent.KEYCODE_DPAD_DOWN,
                android.view.KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN -> {
                    // Show controls on DOWN
                    videoPlayerState.showControls()
                    true
                }
                android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                android.view.KeyEvent.KEYCODE_ENTER,
                android.view.KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                    // Show controls on OK
                    videoPlayerState.showControls()
                    true
                }
                else -> false
            }
        } else {
            false // Let child elements handle the event when controls are visible
        }
    }

@androidx.annotation.OptIn(UnstableApi::class)
private fun getTracks(player: Player, type: Int): Array<Track> {
    val tracks = arrayListOf<Track>()
    for (groupIndex in 0 until player.currentTracks.groups.count()) {
        val group = player.currentTracks.groups[groupIndex]
        if (group.type == type) {
            val format = group.mediaTrackGroup.getFormat(0)

            val track =
                Track(
                    id = groupIndex,
                    label = format.label,
                    language = Locale(format.language.toString()).displayLanguage,
                    codec = format.codecs,
                    selected = group.isSelected,
                    supported = group.isSupported,
                )

            tracks.add(track)
        }
    }

    val noneTrack =
        Track(
            id = -1,
            label = null,
            language = null,
            codec = null,
            selected = !tracks.any { it.selected },
            supported = true,
        )
    return arrayOf(noneTrack) + tracks
}

@androidx.annotation.OptIn(UnstableApi::class)
private fun switchToTrack(player: Player, trackType: Int, trackId: Int) {
    val trackGroups = player.currentTracks.groups
    
    if (trackId == -1) {
        // Disable track (for subtitles)
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(trackType, true)
            .build()
    } else {
        // Enable and select specific track
        for (groupIndex in 0 until trackGroups.size) {
            val group = trackGroups[groupIndex]
            if (group.type == trackType && groupIndex == trackId) {
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(trackType, false)
                    .setOverrideForType(
                        androidx.media3.common.TrackSelectionOverride(
                            group.mediaTrackGroup,
                            0
                        )
                    )
                    .build()
                break
            }
        }
    }
}

// Helper function to format time in HH:MM:SS format
private fun formatTime(millis: Long): String {
    // Handle invalid/unknown duration
    if (millis <= 0 || millis == Long.MAX_VALUE || millis == androidx.media3.common.C.TIME_UNSET) {
        return "--:--:--"
    }
    
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    // Show HH:MM:SS format, or MM:SS if less than 1 hour
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
