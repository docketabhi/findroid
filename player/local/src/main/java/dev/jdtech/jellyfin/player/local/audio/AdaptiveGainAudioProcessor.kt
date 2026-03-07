package dev.jdtech.jellyfin.player.local.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow
import kotlin.math.roundToInt

internal class AdaptiveGainAudioProcessor(
    private val gainMilliBelProvider: () -> Int,
) : BaseAudioProcessor() {
    override fun onConfigure(
        inputAudioFormat: AudioProcessor.AudioFormat
    ): AudioProcessor.AudioFormat {
        return when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT,
            C.ENCODING_PCM_FLOAT,
            -> inputAudioFormat
            else -> AudioProcessor.AudioFormat.NOT_SET
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) {
            return
        }

        val outputBuffer = replaceOutputBuffer(inputBuffer.remaining())
        val effectiveGainDb =
            resolveEffectiveGainDb(
                requestedGainMilliBel = gainMilliBelProvider().coerceIn(0, MAX_GAIN_MILLIBEL),
                channelCount = inputAudioFormat.channelCount,
            )

        if (effectiveGainDb <= 0f) {
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        val linearGain = 10f.pow(effectiveGainDb / 20f)
        when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT -> processPcm16(inputBuffer, outputBuffer, linearGain)
            C.ENCODING_PCM_FLOAT -> processPcmFloat(inputBuffer, outputBuffer, linearGain)
            else -> outputBuffer.put(inputBuffer)
        }
        outputBuffer.flip()
    }

    private fun processPcm16(inputBuffer: ByteBuffer, outputBuffer: ByteBuffer, gain: Float) {
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        outputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        while (inputBuffer.remaining() >= PCM_16_BYTES_PER_SAMPLE) {
            val sample = inputBuffer.short.toInt()
            val boosted = (sample * gain).roundToInt()
            outputBuffer.putShort(
                boosted
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    .toShort()
            )
        }
    }

    private fun processPcmFloat(inputBuffer: ByteBuffer, outputBuffer: ByteBuffer, gain: Float) {
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        outputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        while (inputBuffer.remaining() >= PCM_FLOAT_BYTES_PER_SAMPLE) {
            val sample = inputBuffer.float
            val boosted = (sample * gain).coerceIn(-1f, 1f)
            outputBuffer.putFloat(boosted)
        }
    }

    private fun resolveEffectiveGainDb(requestedGainMilliBel: Int, channelCount: Int): Float {
        val requestedGainDb = requestedGainMilliBel / 100f
        if (requestedGainDb <= 0f) {
            return 0f
        }

        // Preserve punch for stereo content while reducing clipping risk for surround tracks.
        val effectiveGainDb =
            if (channelCount <= STEREO_CHANNEL_COUNT) {
                requestedGainDb
            } else {
                requestedGainDb * SURROUND_GAIN_RATIO
            }

        return effectiveGainDb.coerceIn(0f, MAX_GAIN_DB)
    }

    private companion object {
        private const val STEREO_CHANNEL_COUNT = 2
        private const val SURROUND_GAIN_RATIO = 0.65f
        private const val MAX_GAIN_MILLIBEL = 3000
        private const val MAX_GAIN_DB = 30f
        private const val PCM_16_BYTES_PER_SAMPLE = 2
        private const val PCM_FLOAT_BYTES_PER_SAMPLE = 4
    }
}
