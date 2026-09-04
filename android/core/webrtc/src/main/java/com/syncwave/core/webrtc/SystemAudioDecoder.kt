package com.syncwave.core.webrtc

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Receives Opus frames produced by [SystemAudioEncoder] and decodes them
 * with [MediaCodec] into PCM, played back through an [AudioTrack].
 *
 * Frames arrive on a binary [org.webrtc.DataChannel] — push them in via
 * [submit] from the [org.webrtc.DataChannel.Observer] in your PeerConnection.
 */
@RequiresApi(Build.VERSION_CODES.Q)
class SystemAudioDecoder(
    private val scope: CoroutineScope
) {
    sealed interface State { data object Running : State; data object Stopped : State }

    private val _state = MutableSharedFlow<State>(replay = 1, extraBufferCapacity = 4)
    val state: SharedFlow<State> = _state.asSharedFlow()

    private val sampleRate = 48_000
    private val channels = 2
    private var codec: MediaCodec? = null
    private var track: AudioTrack? = null
    private var pumpJob: Job? = null

    private val pending = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    private val pendingFlow: SharedFlow<ByteArray> = pending.asSharedFlow()

    fun start() {
        val opusName = findOpusDecoder() ?: error("No Opus decoder on this device")
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, sampleRate, channels)
            .apply {
                setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
            }
        codec = MediaCodec.createByCodecName(opusName).also {
            it.configure(format, null, null, 0)
            it.start()
        }

        val pcmFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .build()
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT
        )
        track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(pcmFormat)
            .setBufferSizeInBytes(minBuf * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track?.play()

        pumpJob = scope.launch(Dispatchers.IO) {
            val info = MediaCodec.BufferInfo()
            pendingFlow.collect { opusPacket ->
                val dec = codec ?: return@collect
                feedAndDrain(dec, opusPacket, info)
            }
        }
        _state.tryEmit(State.Running)
    }

    fun submit(opusPacket: ByteArray) {
        pending.tryEmit(opusPacket)
    }

    fun stop() {
        pumpJob?.cancel()
        pumpJob = null
        runCatching { track?.stop() }
        runCatching { track?.release() }
        track = null
        runCatching { codec?.stop() }
        runCatching { codec?.release() }
        codec = null
        _state.tryEmit(State.Stopped)
    }

    private fun feedAndDrain(dec: MediaCodec, packet: ByteArray, info: MediaCodec.BufferInfo) {
        val inIdx = dec.dequeueInputBuffer(10_000)
        if (inIdx >= 0) {
            val inBuf = dec.getInputBuffer(inIdx)
            if (inBuf == null) {
                Log.w(TAG, "Failed to get input buffer at index $inIdx")
                return
            }
            inBuf.clear()
            inBuf.put(packet)
            dec.queueInputBuffer(inIdx, 0, packet.size, 0, 0)
        }
        while (true) {
            val outIdx = dec.dequeueOutputBuffer(info, 0)
            when {
                outIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> return
                outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> continue
                outIdx >= 0 -> {
                    val outBuf = dec.getOutputBuffer(outIdx)
                    if (outBuf == null) {
                        Log.w(TAG, "Failed to get output buffer at index $outIdx")
                        dec.releaseOutputBuffer(outIdx, false)
                        continue
                    }
                    if (info.size > 0) {
                        val pcm = ByteArray(info.size)
                        outBuf.position(info.offset)
                        outBuf.get(pcm, 0, info.size)
                        track?.write(pcm, 0, pcm.size)
                    }
                    dec.releaseOutputBuffer(outIdx, false)
                }
                else -> return
            }
        }
    }

    private fun findOpusDecoder(): String? {
        val list = android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS)
        for (codec in list.codecInfos) {
            if (!codec.isEncoder && codec.name.contains("opus", ignoreCase = true)) {
                return codec.name
            }
        }
        return null
    }

    companion object {
        private const val TAG = "SyncWave/SysAudioDec"
    }
}
