package dev.jdtech.jellyfin.ui.components.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
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
import dev.jdtech.jellyfin.utils.handleDPadKeyEvents

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun VideoPlayerSeekBar(
    progress: Float,
    onSeek: (seekProgress: Float) -> Unit,
    state: VideoPlayerState,
    focusRequester: androidx.compose.ui.focus.FocusRequester = androidx.compose.ui.focus.FocusRequester(),
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

    // Update seek progress when not focused
    LaunchedEffect(progress, isFocused) {
        if (!isFocused) {
            seekProgress = progress
        }
    }

    // Keep controls visible while focused
    LaunchedEffect(isFocused) {
        if (isFocused) {
            state.showControls(seconds = Int.MAX_VALUE)
        }
    }

    Canvas(
        modifier =
            Modifier.fillMaxWidth()
                .height(animatedHeight)
                .padding(horizontal = 4.dp)
                .focusRequester(focusRequester)
                .focusable(interactionSource = interactionSource)
                .onPreviewKeyEvent { keyEvent ->
                    // Intercept ALL key events when seekbar is focused
                    if (isFocused && keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_UP) {
                        when (keyEvent.nativeKeyEvent.keyCode) {
                            android.view.KeyEvent.KEYCODE_DPAD_LEFT,
                            android.view.KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT -> {
                                // Seek backward by 1% and apply immediately
                                seekProgress = (seekProgress - 0.01f).coerceAtLeast(0f)
                                onSeek(seekProgress)
                                true // Consume the event
                            }
                            android.view.KeyEvent.KEYCODE_DPAD_RIGHT,
                            android.view.KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT -> {
                                // Seek forward by 1% and apply immediately
                                seekProgress = (seekProgress + 0.01f).coerceAtMost(1f)
                                onSeek(seekProgress)
                                true // Consume the event
                            }
                            android.view.KeyEvent.KEYCODE_DPAD_UP,
                            android.view.KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP -> {
                                // Exit seekbar, go back to buttons
                                focusManager.moveFocus(FocusDirection.Up)
                                true
                            }
                            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                            android.view.KeyEvent.KEYCODE_ENTER,
                            android.view.KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                // Apply final seek and exit
                                onSeek(seekProgress)
                                focusManager.moveFocus(FocusDirection.Up)
                                true
                            }
                            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                // Block down navigation
                                true
                            }
                            else -> false
                        }
                    } else {
                        false
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
        drawLine(
            color = color,
            start = Offset(x = 0f, y = yOffset),
            end =
                Offset(
                    x = size.width.times(if (isFocused) seekProgress else progress),
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
                    x = size.width.times(if (isFocused) seekProgress else progress),
                    y = yOffset,
                ),
        )
    }
}

@Preview
@Composable
fun VideoPlayerSeekBarPreview() {
    FindroidTheme {
        VideoPlayerSeekBar(progress = 0.4f, onSeek = {}, state = rememberVideoPlayerState())
    }
}
