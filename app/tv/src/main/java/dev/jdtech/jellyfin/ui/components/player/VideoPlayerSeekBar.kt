package dev.jdtech.jellyfin.ui.components.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun VideoPlayerSeekBar(
    progress: Float,
    bufferedProgress: Float = progress,
    durationMs: Long,
    onSeek: (seekProgress: Float) -> Unit,
    state: VideoPlayerState,
    focusRequester: androidx.compose.ui.focus.FocusRequester = androidx.compose.ui.focus.FocusRequester(),
    stepMs: Long = SEEK_STEP_MS,
    fastStepMs: Long = SEEK_FAST_STEP_MS,
    consumeDownKey: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val color by
        rememberUpdatedState(
            newValue =
                if (isFocused) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
        )
    val animatedHeight by animateDpAsState(targetValue = 8.dp.times(if (isFocused) 2f else 1f))
    var seekProgress by remember { mutableFloatStateOf(progress) }
    val focusManager = LocalFocusManager.current

    // Optimize: Use derivedStateOf to prevent unnecessary recompositions
    val displayProgress by remember {
        androidx.compose.runtime.derivedStateOf {
            if (isFocused) seekProgress else progress
        }
    }

    // Update seek progress when not focused
    LaunchedEffect(progress, isFocused) {
        if (!isFocused) {
            seekProgress = progress
        }
    }

    // Keep controls visible while focused
    LaunchedEffect(isFocused) {
        if (isFocused) {
            state.showControls(seconds = SEEKBAR_FOCUS_HIDE_SECONDS)
        } else {
            state.showControls()
        }
    }

    fun seekBy(stepMs: Long) {
        val safeDurationMs = durationMs.coerceAtLeast(1L)
        val deltaProgress = (stepMs.toFloat() / safeDurationMs.toFloat()).coerceIn(-1f, 1f)
        seekProgress = (seekProgress + deltaProgress).coerceIn(0f, 1f)
        onSeek(seekProgress)
    }

    Canvas(
        modifier =
            Modifier.fillMaxWidth()
                .height(animatedHeight)
                .padding(horizontal = 4.dp)
                .focusRequester(focusRequester)
                .focusable(interactionSource = interactionSource)
                .onPreviewKeyEvent { keyEvent ->
                    if (!isFocused) return@onPreviewKeyEvent false

                    when (keyEvent.nativeKeyEvent.action) {
                        android.view.KeyEvent.ACTION_DOWN -> {
                            when (keyEvent.nativeKeyEvent.keyCode) {
                                android.view.KeyEvent.KEYCODE_DPAD_LEFT,
                                android.view.KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT -> {
                                    val deltaMs =
                                        if (keyEvent.nativeKeyEvent.repeatCount > 4) {
                                            -fastStepMs
                                        } else {
                                            -stepMs
                                        }
                                    seekBy(deltaMs)
                                    state.showControls(seconds = SEEKBAR_FOCUS_HIDE_SECONDS)
                                    true
                                }
                                android.view.KeyEvent.KEYCODE_DPAD_RIGHT,
                                android.view.KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT -> {
                                    val deltaMs =
                                        if (keyEvent.nativeKeyEvent.repeatCount > 4) {
                                            fastStepMs
                                        } else {
                                            stepMs
                                        }
                                    seekBy(deltaMs)
                                    state.showControls(seconds = SEEKBAR_FOCUS_HIDE_SECONDS)
                                    true
                                }
                                android.view.KeyEvent.KEYCODE_DPAD_UP,
                                android.view.KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP -> {
                                    focusManager.moveFocus(FocusDirection.Up)
                                    state.showControls()
                                    true
                                }
                                android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                android.view.KeyEvent.KEYCODE_ENTER,
                                android.view.KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                    onSeek(seekProgress)
                                    focusManager.moveFocus(FocusDirection.Up)
                                    state.showControls()
                                    true
                                }
                                android.view.KeyEvent.KEYCODE_DPAD_DOWN ->
                                    if (consumeDownKey) true else false
                                else -> false
                            }
                        }
                        android.view.KeyEvent.ACTION_UP -> {
                            when (keyEvent.nativeKeyEvent.keyCode) {
                                android.view.KeyEvent.KEYCODE_DPAD_LEFT,
                                android.view.KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT,
                                android.view.KeyEvent.KEYCODE_DPAD_RIGHT,
                                android.view.KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT,
                                android.view.KeyEvent.KEYCODE_DPAD_UP,
                                android.view.KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP,
                                android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                android.view.KeyEvent.KEYCODE_ENTER,
                                android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,
                                android.view.KeyEvent.KEYCODE_DPAD_DOWN ->
                                    if (consumeDownKey) true else false
                                else -> false
                            }
                        }
                        else -> false
                    }
                }
    ) {
        val yOffset = size.height.div(2)
        drawLine(
            color = color.copy(alpha = 0.24f),
            start = Offset(x = 0f, y = yOffset),
            end = Offset(x = size.width, y = yOffset),
            strokeWidth = size.height.div(2),
            cap = StrokeCap.Round,
        )
        val clampedBufferedProgress = bufferedProgress.coerceIn(0f, 1f)
        if (clampedBufferedProgress > 0f) {
            drawLine(
                color = color.copy(alpha = 0.45f),
                start = Offset(x = 0f, y = yOffset),
                end = Offset(x = size.width.times(clampedBufferedProgress), y = yOffset),
                strokeWidth = size.height.div(2),
                cap = StrokeCap.Round,
            )
        }
        drawLine(
            color = color,
            start = Offset(x = 0f, y = yOffset),
            end =
                Offset(
                    x = size.width.times(displayProgress),
                    y = yOffset,
                ),
            strokeWidth = size.height.div(2),
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = Color.White,
            radius = size.height.div(2),
            center =
                Offset(
                    x = size.width.times(displayProgress),
                    y = yOffset,
                ),
        )
    }
}

@Preview
@Composable
fun VideoPlayerSeekBarPreview() {
    FindroidTheme {
        VideoPlayerSeekBar(
            progress = 0.4f,
            bufferedProgress = 0.72f,
            durationMs = 3_600_000L,
            onSeek = {},
            state = rememberVideoPlayerState(),
        )
    }
}

private const val SEEK_STEP_MS = 5_000L
private const val SEEK_FAST_STEP_MS = 15_000L
private const val SEEKBAR_FOCUS_HIDE_SECONDS = 12
