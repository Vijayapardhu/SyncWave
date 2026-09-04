package com.syncwave.core.webrtc

import android.content.Context
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.MediaConstraints

/**
 * Creates a WebRTC [AudioTrack] that captures from the device microphone
 * through the standard [org.webrtc.audio.JavaAudioDeviceModule] path.
 *
 * Echo cancellation / noise suppression / AGC are enabled by default; that's
 * the right choice for voice. The track is published via
 * [PeerSession.attachLocalAudioTrack].
 */
class MicAudioTrackFactory(private val appContext: Context) {

    fun create(): AudioTrack {
        WebRtcGlobals.init(appContext)
        val source: AudioSource = WebRtcGlobals.factory.createAudioSource(
            MediaConstraints().apply {
                optional.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
                optional.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
                optional.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            }
        )
        return WebRtcGlobals.factory.createAudioTrack("ARDAMSa0", source).also {
            it.setEnabled(true)
        }
    }
}
