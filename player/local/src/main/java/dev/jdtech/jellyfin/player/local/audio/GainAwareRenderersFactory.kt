package dev.jdtech.jellyfin.player.local.audio

import android.content.Context
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink

internal class GainAwareRenderersFactory(
    context: Context,
    private val gainMilliBelProvider: () -> Int,
) : DefaultRenderersFactory(context) {
    private val gainAudioProcessor = AdaptiveGainAudioProcessor(gainMilliBelProvider)

    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean,
    ): AudioSink {
        return DefaultAudioSink.Builder(context)
            .setAudioProcessors(arrayOf(gainAudioProcessor))
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioOutputPlaybackParameters(enableAudioTrackPlaybackParams)
            .build()
    }
}
