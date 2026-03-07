package dev.jdtech.jellyfin.ui

import android.media.audiofx.LoudnessEnhancer
import android.net.TrafficStats
import android.os.Process
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import dev.jdtech.jellyfin.core.R
import dev.jdtech.jellyfin.player.core.domain.models.Track
import dev.jdtech.jellyfin.player.local.presentation.PlayerViewModel
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.ui.components.player.VideoPlayerMediaButton
import dev.jdtech.jellyfin.ui.components.player.VideoPlayerOverlay
import dev.jdtech.jellyfin.ui.components.player.VideoPlayerSeekBar
import dev.jdtech.jellyfin.ui.components.player.VideoPlayerState
import dev.jdtech.jellyfin.ui.components.player.rememberVideoPlayerState
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun PlayerScreen(
    itemId: UUID,
    itemKind: String,
    startFromBeginning: Boolean,
    queueParentId: UUID? = null,
    onNavigateBack: () -> Unit = {},
    // resultRecipient: ResultRecipient<VideoPlayerTrackSelectorDialogDestination,
    // VideoPlayerTrackSelectorDialogResult>,
) {
    val viewModel = hiltViewModel<PlayerViewModel>()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                    mediaSession?.release()
                }

                Lifecycle.Event.ON_START -> {
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
    var bufferedPosition by remember { mutableLongStateOf(0L) }
    var playbackState by remember { mutableStateOf(Player.STATE_IDLE) }
    var networkSpeedBps by remember { mutableLongStateOf(0L) }
    var audioDetails by remember { mutableStateOf("Audio: --") }
    var videoDetails by remember { mutableStateOf("Video: --") }
    var audioGainMb by remember { mutableIntStateOf(AUDIO_GAIN_MIN_MB) }
    var autoGainEnabled by remember { mutableStateOf(true) }
    var autoGainTargetMb by remember { mutableIntStateOf(AUDIO_GAIN_MIN_MB) }
    var lastAutoGainTrackKey by remember { mutableStateOf<String?>(null) }
    var audioSessionId by remember { mutableIntStateOf(C.AUDIO_SESSION_ID_UNSET) }
    var audioGainUnavailableToastShown by remember { mutableStateOf(false) }
    val loudnessEnhancerState = remember { mutableStateOf<LoudnessEnhancer?>(null) }
    var isPlaying by remember { mutableStateOf(viewModel.player.isPlaying) }

    fun applyAudioGain(targetGainMb: Int, showUnavailableToast: Boolean) {
        val clampedGain = snapAudioGain(targetGainMb)
        audioGainMb = clampedGain
        viewModel.setAudioGain(clampedGain)
        val hardwareGainMb = toHardwareGain(clampedGain)
        val enhancer = loudnessEnhancerState.value
        if (enhancer != null) {
            runCatching {
                enhancer.setTargetGain(hardwareGainMb)
                enhancer.enabled = hardwareGainMb > 0
                audioGainUnavailableToastShown = false
            }.onFailure {
                if (showUnavailableToast && !audioGainUnavailableToastShown) {
                    Toast.makeText(
                        context,
                        "Hardware gain unavailable, using software boost",
                        Toast.LENGTH_SHORT,
                    ).show()
                    audioGainUnavailableToastShown = true
                }
            }
        } else if (showUnavailableToast && !audioGainUnavailableToastShown) {
            Toast.makeText(
                context,
                "Hardware gain unavailable, using software boost",
                Toast.LENGTH_SHORT,
            ).show()
            audioGainUnavailableToastShown = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            loudnessEnhancerState.value?.release()
            loudnessEnhancerState.value = null
        }
    }

    LaunchedEffect(Unit) {
        var lastRxBytes = TrafficStats.getUidRxBytes(Process.myUid())
        var lastTimestampMs = System.currentTimeMillis()
        var trackDetailsTick = 0
        while (isActive) {
            val pollDelayMs =
                if (videoPlayerState.controlsVisible || playbackState == Player.STATE_BUFFERING) {
                    500L
                } else {
                    1_000L
                }
            delay(pollDelayMs)
            currentPosition = viewModel.player.currentPosition
            bufferedPosition = viewModel.player.bufferedPosition
            playbackState = viewModel.player.playbackState
            isPlaying = viewModel.player.isPlaying
            val nowRxBytes = TrafficStats.getUidRxBytes(Process.myUid())
            val nowTimestampMs = System.currentTimeMillis()
            val deltaBytes = nowRxBytes - lastRxBytes
            val deltaMs = nowTimestampMs - lastTimestampMs
            networkSpeedBps =
                if (nowRxBytes < 0 || lastRxBytes < 0) {
                    0L
                } else if (deltaBytes > 0 && deltaMs > 0) {
                    (deltaBytes * 8_000L) / deltaMs
                } else {
                    0L
                }
            lastRxBytes = nowRxBytes
            lastTimestampMs = nowTimestampMs

            val shouldRefreshTrackDetails =
                videoPlayerState.controlsVisible &&
                    (trackDetailsTick++ % TRACK_DETAILS_REFRESH_INTERVAL_TICKS == 0)
            if (shouldRefreshTrackDetails || audioDetails == "Audio: --") {
                val (audioInfo, videoInfo) = getSelectedTrackDetails(viewModel.player)
                audioDetails = audioInfo
                videoDetails = videoInfo
            }

            val autoGainRecommendation = getSelectedAudioGainRecommendation(viewModel.player)
            if (autoGainRecommendation != null) {
                autoGainTargetMb = autoGainRecommendation.targetGainMb
                if (autoGainRecommendation.key != lastAutoGainTrackKey) {
                    lastAutoGainTrackKey = autoGainRecommendation.key
                    autoGainEnabled = true
                }
                if (autoGainEnabled && audioGainMb != autoGainRecommendation.targetGainMb) {
                    applyAudioGain(
                        targetGainMb = autoGainRecommendation.targetGainMb,
                        showUnavailableToast = false,
                    )
                }
            }

            val currentSessionId = viewModel.player.audioSessionId
            if (
                currentSessionId != C.AUDIO_SESSION_ID_UNSET &&
                    currentSessionId > 0 &&
                    currentSessionId != audioSessionId
            ) {
                runCatching {
                    loudnessEnhancerState.value?.release()
                    loudnessEnhancerState.value =
                        LoudnessEnhancer(currentSessionId).apply {
                            val hardwareGainMb = toHardwareGain(audioGainMb)
                            setTargetGain(hardwareGainMb)
                            enabled = hardwareGainMb > 0
                        }
                    audioSessionId = currentSessionId
                    audioGainUnavailableToastShown = false
                }.onFailure {
                    loudnessEnhancerState.value = null
                    audioSessionId = C.AUDIO_SESSION_ID_UNSET
                }
            }
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
            // If controls are hidden, stop playback and navigate back
            viewModel.player.stop()
            onNavigateBack()
        }
    }

    Box(
        modifier =
            Modifier.dPadEvents(videoPlayerState = videoPlayerState)
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
                        queueParentId = queueParentId,
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
        Text(
            text = formatBitrate(networkSpeedBps),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            modifier =
                Modifier.align(Alignment.TopEnd)
                    .padding(
                        top = MaterialTheme.spacings.default,
                        end = MaterialTheme.spacings.default,
                    )
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
        )
        val isAudioOnlyContent = !hasSelectedMotionVideoTrack(viewModel.player)
        if (isAudioOnlyContent && !videoPlayerState.controlsVisible) {
            AudioNowPlayingOverlay(
                title = uiState.currentItemTitle,
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                durationMs = viewModel.player.duration,
                modifier =
                    Modifier.align(Alignment.BottomCenter)
                        .padding(horizontal = MaterialTheme.spacings.large, vertical = 110.dp),
            )
        }
        val focusRequester = remember { FocusRequester() }
        VideoPlayerOverlay(
            modifier = Modifier.align(Alignment.BottomCenter),
            focusRequester = focusRequester,
            state = videoPlayerState,
            isPlaying = isPlaying,
            contentPadding =
                if (isAudioOnlyContent) MaterialTheme.spacings.medium else MaterialTheme.spacings.large,
            controls = {
                VideoPlayerControls(
                    title = uiState.currentItemTitle,
                    isPlaying = isPlaying,
                    contentCurrentPosition = currentPosition,
                    contentBufferedPosition = bufferedPosition,
                    playbackState = playbackState,
                    audioDetails = audioDetails,
                    audioGainMb = audioGainMb,
                    autoGainEnabled = autoGainEnabled,
                    autoGainTargetMb = autoGainTargetMb,
                    videoDetails = videoDetails,
                    player = viewModel.player,
                    state = videoPlayerState,
                    focusRequester = focusRequester,
                    onAudioGainChange = { targetGainMb ->
                        autoGainEnabled = false
                        applyAudioGain(
                            targetGainMb = targetGainMb,
                            showUnavailableToast = true,
                        )
                    },
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
    contentBufferedPosition: Long,
    playbackState: Int,
    audioDetails: String,
    audioGainMb: Int,
    autoGainEnabled: Boolean,
    autoGainTargetMb: Int,
    videoDetails: String,
    player: Player,
    state: VideoPlayerState,
    focusRequester: FocusRequester,
    onAudioGainChange: (Int) -> Unit,
    // navigator: DestinationsNavigator,
) {
    val context = LocalContext.current
    val isAudioOnlyContent = !hasSelectedMotionVideoTrack(player)
    var showAdvancedMenu by remember { mutableStateOf(false) }
    val panelHorizontalPadding =
        if (isAudioOnlyContent) MaterialTheme.spacings.medium else MaterialTheme.spacings.large
    val panelVerticalPadding =
        if (isAudioOnlyContent) MaterialTheme.spacings.small else MaterialTheme.spacings.default
    val headerToActionsSpacing =
        if (isAudioOnlyContent) MaterialTheme.spacings.small else MaterialTheme.spacings.default
    val actionsToSeekbarSpacing =
        if (isAudioOnlyContent) MaterialTheme.spacings.small else MaterialTheme.spacings.large
    val mediaButtonsSpacing =
        if (isAudioOnlyContent) MaterialTheme.spacings.extraSmall else MaterialTheme.spacings.medium
    val infoChipSpacing =
        if (isAudioOnlyContent) MaterialTheme.spacings.extraSmall else MaterialTheme.spacings.small
    val mediaButtonSize = if (isAudioOnlyContent) 52.dp else null
    val mediaButtonIconSize = if (isAudioOnlyContent) 22.dp else null
    val onPlayPauseToggle = { shouldPlay: Boolean ->
        if (shouldPlay) {
            player.play()
        } else {
            player.pause()
        }
    }

    Column(
        modifier =
            Modifier.fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                )
                .padding(
                    horizontal = panelHorizontalPadding,
                    vertical = panelVerticalPadding,
                )
    ) {
        if (title.isNotBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacings.small))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(infoChipSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!isAudioOnlyContent && videoDetails != "Video: --") {
                PlayerInfoChip(text = videoDetails, compact = isAudioOnlyContent)
            }
            if (audioDetails != "Audio: --") {
                PlayerInfoChip(text = audioDetails, compact = isAudioOnlyContent)
            }
        }
        Spacer(modifier = Modifier.height(headerToActionsSpacing))

        // Buttons at top: Play/Pause, Audio, Subtitle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(
                    space = mediaButtonsSpacing,
                    alignment = Alignment.CenterHorizontally,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isAudioOnlyContent) {
                VideoPlayerMediaButton(
                    icon = painterResource(id = R.drawable.ic_skip_back),
                    state = state,
                    isPlaying = isPlaying,
                    buttonSize = mediaButtonSize,
                    iconSize = mediaButtonIconSize,
                    onClick = {
                        if (player.hasPreviousMediaItem()) {
                            player.seekToPreviousMediaItem()
                        } else {
                            Toast.makeText(context, "No previous track", Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            }

            // Play/Pause button
            VideoPlayerMediaButton(
                icon = if (isPlaying) painterResource(id = R.drawable.ic_pause) else painterResource(id = R.drawable.ic_play),
                state = state,
                isPlaying = isPlaying,
                modifier = Modifier.focusRequester(focusRequester),
                buttonSize = mediaButtonSize,
                iconSize = mediaButtonIconSize,
                onClick = { onPlayPauseToggle(!isPlaying) },
            )
            
            // Audio button
            VideoPlayerMediaButton(
                icon = painterResource(id = R.drawable.ic_speaker),
                state = state,
                isPlaying = isPlaying,
                buttonSize = mediaButtonSize,
                iconSize = mediaButtonIconSize,
                onClick = {
                    // Cycle through audio tracks
                    val tracks = getTracks(player, C.TRACK_TYPE_AUDIO)
                    if (tracks.isEmpty()) {
                        Toast.makeText(context, "No audio tracks", Toast.LENGTH_SHORT).show()
                        return@VideoPlayerMediaButton
                    }

                    if (tracks.size == 1) {
                        val onlyTrack = tracks.first()
                        val trackInfo = onlyTrack.language ?: onlyTrack.label ?: "Default"
                        Toast.makeText(context, "Audio: $trackInfo", Toast.LENGTH_SHORT).show()
                        return@VideoPlayerMediaButton
                    }

                    val currentIndex = tracks.indexOfFirst { it.selected }.takeIf { it >= 0 } ?: 0
                    val nextIndex = (currentIndex + 1) % tracks.size
                    val nextTrack = tracks[nextIndex]

                    switchToTrack(player, C.TRACK_TYPE_AUDIO, nextTrack.id)
                    val trackInfo =
                        nextTrack.language ?: nextTrack.label ?: "Track ${nextIndex + 1}"
                    Toast.makeText(context, "Audio: $trackInfo", Toast.LENGTH_SHORT).show()
                },
            )

            if (!isAudioOnlyContent) {
                // Subtitle button
                VideoPlayerMediaButton(
                    icon = painterResource(id = R.drawable.ic_closed_caption),
                    state = state,
                    isPlaying = isPlaying,
                    buttonSize = mediaButtonSize,
                    iconSize = mediaButtonIconSize,
                    onClick = {
                        // Cycle through subtitle tracks
                        val tracks = getTracks(player, C.TRACK_TYPE_TEXT)
                        if (tracks.size <= 1) {
                            Toast.makeText(context, "No subtitles available", Toast.LENGTH_SHORT)
                                .show()
                            return@VideoPlayerMediaButton
                        }

                        val currentIndex =
                            tracks.indexOfFirst { it.selected }.takeIf { it >= 0 } ?: 0
                        val nextIndex = (currentIndex + 1) % tracks.size
                        val nextTrack = tracks[nextIndex]

                        switchToTrack(player, C.TRACK_TYPE_TEXT, nextTrack.id)
                        val trackInfo =
                            if (nextTrack.id == -1) {
                                "Off"
                            } else {
                                nextTrack.language ?: nextTrack.label ?: "Track ${nextIndex + 1}"
                            }
                        Toast.makeText(context, "Subtitles: $trackInfo", Toast.LENGTH_SHORT).show()
                    },
                )
            }

            if (isAudioOnlyContent) {
                VideoPlayerMediaButton(
                    icon = painterResource(id = R.drawable.ic_skip_forward),
                    state = state,
                    isPlaying = isPlaying,
                    buttonSize = mediaButtonSize,
                    iconSize = mediaButtonIconSize,
                    onClick = {
                        if (player.hasNextMediaItem()) {
                            player.seekToNextMediaItem()
                        } else {
                            Toast.makeText(context, "No next track", Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            }

            VideoPlayerMediaButton(
                icon = painterResource(id = R.drawable.ic_settings),
                state = state,
                isPlaying = isPlaying,
                buttonSize = mediaButtonSize,
                iconSize = mediaButtonIconSize,
                onClick = { showAdvancedMenu = !showAdvancedMenu },
            )
        }
        
        Spacer(modifier = Modifier.height(actionsToSeekbarSpacing))
        
        // Seekbar at bottom with time display
        Column(modifier = Modifier.fillMaxWidth()) {
            val duration = player.duration
            val hasValidDuration = duration > 0 && duration != C.TIME_UNSET
            val bufferedProgress =
                if (hasValidDuration) {
                    (contentBufferedPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
            val gainRangeMb = AUDIO_GAIN_MAX_MB - AUDIO_GAIN_MIN_MB
            val gainProgress =
                (audioGainMb - AUDIO_GAIN_MIN_MB).toFloat() / gainRangeMb.toFloat()
            val audioGainText = "Audio Gain: ${formatAudioGain(audioGainMb)}"
            val autoGainText =
                if (autoGainEnabled) {
                    "Auto Gain: ON (${formatAudioGain(autoGainTargetMb)})"
                } else {
                    "Auto Gain: OFF (manual)"
                }

            if (showAdvancedMenu) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
                    ) {
                        Text(
                            text = audioGainText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        )
                        Text(
                            text = autoGainText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        )
                    }
                    AudioGainVerticalControl(
                        progress = gainProgress.coerceIn(0f, 1f),
                        onIncrease = {
                            onAudioGainChange(
                                snapAudioGain(audioGainMb + AUDIO_GAIN_STEP_MB)
                            )
                        },
                        onDecrease = {
                            onAudioGainChange(
                                snapAudioGain(audioGainMb - AUDIO_GAIN_STEP_MB)
                            )
                        },
                    )
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacings.small))
            }

            // Time display - only show when duration is valid
            if (hasValidDuration) {
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
                        text = formatTime(duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                
                Spacer(modifier = Modifier.height(MaterialTheme.spacings.small))
            }
            
            // Seekbar
            VideoPlayerSeekBar(
                progress = if (hasValidDuration) contentCurrentPosition.toFloat() / duration.toFloat() else 0f,
                bufferedProgress = bufferedProgress,
                durationMs = duration,
                onSeek = { seekProgress -> 
                    if (hasValidDuration) {
                        player.seekTo((duration * seekProgress).toLong())
                    }
                },
                state = state,
            )
        }
    }
}

@Composable
private fun PlayerInfoChip(
    text: String,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(999.dp),
                )
                .padding(
                    horizontal = if (compact) 8.dp else 10.dp,
                    vertical = if (compact) 3.dp else 4.dp,
                ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
            maxLines = 1,
        )
    }
}

@Composable
private fun AudioGainVerticalControl(
    progress: Float,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val clampedProgress = progress.coerceIn(0f, 1f)

    Column(
        modifier =
            modifier
                .focusable(interactionSource = interactionSource)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.nativeKeyEvent.action != android.view.KeyEvent.ACTION_DOWN) {
                        return@onPreviewKeyEvent false
                    }
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_UP,
                        android.view.KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP -> {
                            onIncrease()
                            true
                        }
                        android.view.KeyEvent.KEYCODE_DPAD_DOWN,
                        android.view.KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN -> {
                            onDecrease()
                            true
                        }
                        else -> false
                    }
                },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "+",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
        )
        Box(
            modifier =
                Modifier.width(14.dp)
                    .height(120.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(999.dp),
                    )
                    .border(
                        width = if (isFocused) 2.dp else 1.dp,
                        color =
                            if (isFocused) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            },
                        shape = RoundedCornerShape(999.dp),
                    ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .fillMaxHeight(clampedProgress)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(999.dp),
                        ),
            )
        }
        Text(
            text = "-",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun AudioNowPlayingOverlay(
    title: String,
    isPlaying: Boolean,
    currentPosition: Long,
    durationMs: Long,
    modifier: Modifier = Modifier,
) {
    val durationValid = durationMs > 0 && durationMs != C.TIME_UNSET
    val progress =
        if (durationValid) {
            (currentPosition.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(18.dp),
                )
                .padding(
                    horizontal = MaterialTheme.spacings.large,
                    vertical = MaterialTheme.spacings.default,
                ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.default),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RotatingDiscIndicator(isPlaying = isPlaying)
            Text(
                text = if (title.isNotBlank()) title else "Now Playing",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${formatTime(currentPosition)} / ${if (durationValid) formatTime(durationMs) else "--:--"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacings.small))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun RotatingDiscIndicator(isPlaying: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "disc_rotation")
    val rotation by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 2500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "disc_rotation_angle",
        )

    Box(
        modifier =
            modifier
                .size(48.dp)
                .rotate(if (isPlaying) rotation else 0f)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                    shape = CircleShape,
                )
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                    shape = CircleShape,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier.size(28.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        shape = CircleShape,
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        shape = CircleShape,
                    ),
        )
        Box(
            modifier =
                Modifier.size(8.dp)
                    .alpha(if (isPlaying) 1f else 0.7f)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    ),
        )
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

private fun Modifier.dPadEvents(videoPlayerState: VideoPlayerState): Modifier =
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
            for (trackIndex in 0 until group.mediaTrackGroup.length) {
                val isSelected = group.isTrackSelected(trackIndex)
                val isSupported = group.isTrackSupported(trackIndex)
                if (!isSelected && !isSupported) continue

                val format = group.mediaTrackGroup.getFormat(trackIndex)
                val channelInfo =
                    if (format.channelCount > 0 && format.channelCount != Format.NO_VALUE) {
                        "${format.channelCount}ch"
                    } else {
                        null
                    }
                val codecInfo = format.codecs?.substringBefore('.')?.uppercase()
                val fallbackLabel = listOfNotNull(codecInfo, channelInfo).joinToString(" ")

                tracks.add(
                    Track(
                        id = encodeTrackId(groupIndex, trackIndex),
                        label = format.label ?: fallbackLabel.ifBlank { null },
                        language =
                            format.language?.let { languageTag ->
                                Locale.forLanguageTag(languageTag).displayLanguage.takeIf { it.isNotBlank() }
                            },
                        codec = format.codecs,
                        selected = isSelected,
                        supported = isSupported,
                    )
                )
            }
        }
    }

    if (type != C.TRACK_TYPE_TEXT) return tracks.toTypedArray()

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
        // Disable only for subtitle tracks ("Off" option).
        if (trackType != C.TRACK_TYPE_TEXT) return
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(trackType, true)
            .build()
    } else {
        val (groupIndexTarget, trackIndexTarget) = decodeTrackId(trackId)
        // Enable and select specific track
        for (groupIndex in 0 until trackGroups.size) {
            val group = trackGroups[groupIndex]
            if (group.type == trackType && groupIndex == groupIndexTarget) {
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(trackType, false)
                    .setOverrideForType(
                        androidx.media3.common.TrackSelectionOverride(
                            group.mediaTrackGroup,
                            trackIndexTarget
                        )
                    )
                    .build()
                break
            }
        }
    }
}

private fun encodeTrackId(groupIndex: Int, trackIndex: Int): Int = (groupIndex shl 16) or trackIndex

private fun decodeTrackId(trackId: Int): Pair<Int, Int> = (trackId ushr 16) to (trackId and 0xFFFF)

private const val TRACK_DETAILS_REFRESH_INTERVAL_TICKS = 4

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

@androidx.annotation.OptIn(UnstableApi::class)
private fun getSelectedTrackDetails(player: Player): Pair<String, String> {
    var audioText = "Audio: --"
    var videoText = "Video: --"

    for (group in player.currentTracks.groups) {
        if (!group.isSelected) continue
        val selectedTrackIndex =
            (0 until group.mediaTrackGroup.length).firstOrNull { group.isTrackSelected(it) } ?: 0
        val format = group.mediaTrackGroup.getFormat(selectedTrackIndex)
        when (group.type) {
            C.TRACK_TYPE_AUDIO -> {
                val codec = format.codecs?.substringBefore('.')?.uppercase().orEmpty()
                val channels =
                    if (format.channelCount != Format.NO_VALUE && format.channelCount > 0) {
                        "${format.channelCount}ch"
                    } else {
                        null
                    }
                val language =
                    format.language?.let { languageTag ->
                        Locale.forLanguageTag(languageTag).displayLanguage.takeIf { it.isNotBlank() }
                    }
                audioText =
                    buildDetailLine("Audio", codec.ifBlank { null }, channels, language)
            }
            C.TRACK_TYPE_VIDEO -> {
                val codec = format.codecs?.substringBefore('.')?.uppercase().orEmpty()
                val resolution =
                    if (format.width > 0 && format.height > 0) {
                        "${format.width}x${format.height}"
                    } else {
                        null
                    }
                val fps =
                    if (format.frameRate > 0f) {
                        "${format.frameRate.roundToInt()}fps"
                    } else {
                        null
                    }
                videoText =
                    buildDetailLine("Video", codec.ifBlank { null }, resolution, fps)
            }
        }
    }

    return audioText to videoText
}

@androidx.annotation.OptIn(UnstableApi::class)
private fun getSelectedAudioGainRecommendation(player: Player): AutoGainRecommendation? {
    val hasVideoTrack = hasSelectedMotionVideoTrack(player)
    val isAudioOnly = !hasVideoTrack

    for (groupIndex in 0 until player.currentTracks.groups.size) {
        val group = player.currentTracks.groups[groupIndex]
        if (group.type != C.TRACK_TYPE_AUDIO) continue

        val selectedTrackIndex =
            (0 until group.mediaTrackGroup.length).firstOrNull { group.isTrackSelected(it) }
                ?: continue

        val format = group.mediaTrackGroup.getFormat(selectedTrackIndex)
        val channelCount =
            if (format.channelCount != Format.NO_VALUE && format.channelCount > 0) {
                format.channelCount
            } else {
                2
            }
        val codec = format.codecs?.substringBefore('.')?.uppercase(Locale.US).orEmpty()
        val targetGainMb =
            recommendAutoGainMb(
                channelCount = channelCount,
                codec = codec,
                isAudioOnly = isAudioOnly,
            )
        val key =
            buildString {
                append(groupIndex)
                append(':')
                append(selectedTrackIndex)
                append(':')
                append(format.id ?: "")
                append(':')
                append(channelCount)
                append(':')
                append(codec)
            }

        return AutoGainRecommendation(key = key, targetGainMb = targetGainMb)
    }

    return null
}

@androidx.annotation.OptIn(UnstableApi::class)
private fun hasSelectedMotionVideoTrack(player: Player): Boolean {
    for (group in player.currentTracks.groups) {
        if (group.type != C.TRACK_TYPE_VIDEO || !group.isSelected) continue
        val selectedTrackIndex =
            (0 until group.mediaTrackGroup.length).firstOrNull { group.isTrackSelected(it) }
                ?: continue
        val format = group.mediaTrackGroup.getFormat(selectedTrackIndex)
        if (isMotionVideoFormat(format)) return true
    }
    return false
}

private fun isMotionVideoFormat(format: Format): Boolean {
    val sampleMime = format.sampleMimeType?.lowercase(Locale.US).orEmpty()
    val codec = format.codecs?.substringBefore('.')?.lowercase(Locale.US).orEmpty()
    val isImageLikeCodec =
        codec == "mjpeg" ||
            codec == "jpeg" ||
            codec == "jpg" ||
            codec == "png" ||
            codec == "webp" ||
            codec == "bmp" ||
            codec == "gif"
    val isImageLikeMime = sampleMime.startsWith("image/")
    if (isImageLikeCodec || isImageLikeMime) return false

    // Treat cover-art or tiny attached-picture tracks as audio-only.
    val isTinyVisual = format.width in 1..160 && format.height in 1..160
    val hasMotionFrameRate = format.frameRate > 1f
    val hasVideoMime = sampleMime.startsWith("video/")
    return !isTinyVisual && (hasMotionFrameRate || hasVideoMime)
}

private fun buildDetailLine(prefix: String, vararg parts: String?): String {
    val validParts = parts.filterNotNull().filter { it.isNotBlank() }
    return if (validParts.isEmpty()) {
        "$prefix: --"
    } else {
        "$prefix: ${validParts.joinToString(" • ")}"
    }
}

private fun formatDurationShort(millis: Long): String {
    val seconds = (millis / 1000).coerceAtLeast(0)
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return if (minutes > 0) "${minutes}m ${remainingSeconds}s" else "${remainingSeconds}s"
}

private fun formatBitrate(bitsPerSecond: Long): String {
    if (bitsPerSecond <= 0) return "0 Mbps"
    val mbps = bitsPerSecond.toDouble() / 1_000_000.0
    return String.format(Locale.US, "%.2f Mbps", mbps)
}

private const val AUDIO_GAIN_MIN_MB = 0
private const val AUDIO_GAIN_MAX_MB = 3000
private const val AUDIO_GAIN_STEP_MB = 50
private const val AUDIO_GAIN_FAST_STEP_MB = 200
private const val HARDWARE_GAIN_MAX_MB = 1200
private const val HARDWARE_GAIN_RATIO = 0.35f
private const val AUTO_GAIN_DEFAULT_MB = 0

private fun snapAudioGain(gainMilliBel: Int): Int {
    val clamped = gainMilliBel.coerceIn(AUDIO_GAIN_MIN_MB, AUDIO_GAIN_MAX_MB)
    val down = clamped - (clamped % AUDIO_GAIN_STEP_MB)
    val up = (down + AUDIO_GAIN_STEP_MB).coerceAtMost(AUDIO_GAIN_MAX_MB)
    return if (clamped - down < up - clamped) down else up
}

private fun toHardwareGain(gainMilliBel: Int): Int {
    return (gainMilliBel * HARDWARE_GAIN_RATIO)
        .roundToInt()
        .coerceIn(AUDIO_GAIN_MIN_MB, HARDWARE_GAIN_MAX_MB)
}

private fun recommendAutoGainMb(
    @Suppress("UNUSED_PARAMETER") channelCount: Int,
    @Suppress("UNUSED_PARAMETER") codec: String,
    @Suppress("UNUSED_PARAMETER") isAudioOnly: Boolean,
): Int {
    return AUTO_GAIN_DEFAULT_MB
}

private fun formatAudioGain(gainMilliBel: Int): String {
    if (gainMilliBel <= 0) return "0 dB"
    return String.format(Locale.US, "+%.1f dB", gainMilliBel / 100f)
}

private data class AutoGainRecommendation(
    val key: String,
    val targetGainMb: Int,
)
