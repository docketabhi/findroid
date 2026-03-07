package dev.jdtech.jellyfin.ui.components.player

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton

@Composable
fun VideoPlayerMediaButton(
    icon: Painter,
    state: VideoPlayerState,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    buttonSize: Dp? = null,
    iconSize: Dp? = null,
    onClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(isFocused && isPlaying) {
        if (isFocused && isPlaying) {
            state.showControls()
        }
    }

    IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = if (buttonSize != null) modifier.size(buttonSize) else modifier,
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = if (iconSize != null) Modifier.size(iconSize) else Modifier,
        )
    }
}
