package com.syncwave.core.webrtc

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.webrtc.DataChannel
import java.nio.ByteBuffer

import android.annotation.SuppressLint

/**
 * Captures system playback audio via [android.media.projection.AudioPlaybackCapture]
 * (Android 10+), encodes it to Opus using [MediaCodec], and publishes each
 * encoded frame as a binary message on a [DataChannel].
 *
 * The receiving side is [SystemAudioDecoder], which decodes the same Opus
 * stream and plays through an [android.media.AudioTrack].
 *
 * Why a DataChannel and not a WebRTC AudioTrack? The libwebrtc M104
 * JavaAudioDeviceModule only feeds from the microphone; reusing it for
 * system audio would require JNI. The DataChannel path is fully Kotlin and
 * keeps the host in sync with the receiver over the same PeerConnection.
 */
@RequiresApi(Build.VERSION_CODES.Q)
class SystemAudioEncoder(
    private val scope: CoroutineScope
) {
    sealed interface State { data object Running : State; data object Stopped : State }

    private val _state = MutableSharedFlow<State>(replay = 1, extraBufferCapacity = 4)
    val state: SharedFlow<State> = _state.asSharedFlow()

    private var record: AudioRecord? = null
    private var encoder: MediaCodec? = null
    private var pumpJob: Job? = null
    private val sampleRate = 48_000
    private val channels = 2

    fun start(projection: MediaProjection, channel: DataChannel) {
        check(channel.state() == DataChannel.State.OPEN) {
            "DataChannel must be OPEN (was ${channel.state()})"
        }

        val cfg = AudioPlaybackCaptureConfiguration.Builder(projection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .build()

        val channelMask = AudioFormat.CHANNEL_IN_STEREO
        val pcmFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelMask, pcmFormat)
            .also { if (it <= 0) error("AudioRecord minBuffer invalid: $it") }

        val recorder = AudioRecord.Builder()
            .setAudioPlaybackCaptureConfig(cfg)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(pcmFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelMask)
                    .build()
            )
            .setBufferSizeInBytes(minBuf * 4)
            .build()
        check(recorder.state == AudioRecord.STATE_INITIALIZED) {
            "AudioRecord init failed (state=${recorder.state})"
        }
        record = recorder

        val opusName = findOpusEncoder() ?: error("No Opus encoder on this device")
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, sampleRate, channels)
            .apply {
                setInteger(MediaFormat.KEY_BIT_RATE, 64_000)
                setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_STEREO)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, minBuf * 2)
            }
        val enc = MediaCodec.createByCodecName(opusName)
        enc.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        enc.start()
        encoder = enc

        recorder.startRecording()
        _state.tryEmit(State.Running)

        pumpJob = scope.launch(Dispatchers.IO) {
            val pcm = ShortArray(minBuf)
            val info = MediaCodec.BufferInfo()
            val startNs = System.nanoTime()
            while (true) {
                val r = record ?: break
                val read = r.read(pcm, 0, pcm.size, AudioRecord.READ_BLOCKING)
                if (read <= 0) {
                    Log.w(TAG, "AudioRecord.read=$read, ending pump")
                    break
                }
                pumpOnce(enc, pcm, read, startNs, info, channel)
            }
        }
    }

    fun stop() {
        pumpJob?.cancel()
        pumpJob = null
        runCatching { record?.stop() }
        runCatching { record?.release() }
        record = null
        runCatching { encoder?.stop() }
        runCatching { encoder?.release() }
        encoder = null
        _state.tryEmit(State.Stopped)
    }

    private fun pumpOnce(
        enc: MediaCodec,
        pcm: ShortArray,
        frames: Int,
        startNs: Long,
        info: MediaCodec.BufferInfo,
        channel: DataChannel
    ) {
        val inIdx = enc.dequeueInputBuffer(10_000)
        if (inIdx >= 0) {
            val inBuf = enc.getInputBuffer(inIdx)
            if (inBuf == null) {
                Log.w(TAG, "Failed to get input buffer at index $inIdx")
                return
            }
            inBuf.clear()
            val bytes = ByteArray(frames * 2)
            for (i in 0 until frames) {
                val s = pcm[i].toInt()
                bytes[i * 2] = (s and 0xFF).toByte()
                bytes[i * 2 + 1] = ((s ushr 8) and 0xFF).toByte()
            }
            inBuf.put(bytes)
            val ptsUs = (System.nanoTime() - startNs) / 1_000L
            enc.queueInputBuffer(inIdx, 0, bytes.size, ptsUs, 0)
        }

        // Drain encoded packets and push them down the DataChannel.
        while (true) {
            val outIdx = enc.dequeueOutputBuffer(info, 0)
            when {
                outIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> return
                outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> continue
                outIdx >= 0 -> {
                    val outBuf: ByteBuffer? = enc.getOutputBuffer(outIdx)
                    if (outBuf == null) {
                        Log.w(TAG, "Failed to get output buffer at index $outIdx")
                        enc.releaseOutputBuffer(outIdx, false)
                        continue
                    }
                    if (info.size > 0 && channel.state() == DataChannel.State.OPEN) {
                        val payload = ByteArray(info.size)
                        outBuf.position(info.offset)
                        outBuf.get(payload, 0, info.size)
                        channel.send(
                            DataChannel.Buffer(ByteBuffer.wrap(payload), /* binary = */ true)
                        )
                    }
                    enc.releaseOutputBuffer(outIdx, false)
                }
                else -> return
            }
        }
    }

    private fun findOpusEncoder(): String? {
        val list = android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS)
        for (codec in list.codecInfos) {
            if (codec.isEncoder && codec.name.contains("opus", ignoreCase = true)) {
                return codec.name
            }
        }
        return null
    }

    companion object {
        private const val TAG = "SyncWave/SysAudioEnc"
    }
}
