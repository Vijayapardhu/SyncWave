package com.syncwave.core.webrtc

import android.content.Context
import android.content.Intent
import android.view.Surface
import com.syncwave.core.media.ScreenCaptureConfig
import org.webrtc.ScreenCapturerAndroid
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

/**
 * Turns a granted screen-capture [Intent] into a [VideoTrack] that can be added to
 * a [org.webrtc.PeerConnection] via [PeerSession.attachLocalVideoTrack].
 *
 * The track is screencast-flagged, which lets libwebrtc pick the right
 * encoder tuning (lower latency, lower CPU).
 */
class ScreenTrackFactory(private val appContext: Context) {

    fun create(mediaProjectionIntent: Intent, config: ScreenCaptureConfig): VideoTrack {
        // libwebrtc M104 expects (Intent, MediaProjection.Callback). We don't
        // need the projection callback here — libwebrtc creates its own
        // MediaProjection from the Intent on first startCapture().
        val capturer: VideoCapturer = ScreenCapturerAndroid(
            mediaProjectionIntent,
            /* mediaProjectionCallback = */ null
        )
        // Use the same EGL context as the encoder factory (WebRtcGlobals.eglBase)
        // so frames can flow from capturer → source without context mismatch.
        val helper = SurfaceTextureHelper.create("SyncWaveScreen", WebRtcGlobals.eglBase.eglBaseContext)
        val source: VideoSource = WebRtcGlobals.factory.createVideoSource(capturer.isScreencast)
        capturer.initialize(helper, appContext, source.capturerObserver)
        capturer.startCapture(config.width, config.height, config.fps)
        return WebRtcGlobals.factory.createVideoTrack("ARDAMv0", source).also { it.setEnabled(true) }
    }

    /**
     * Hook for V0.2: keep the underlying [Surface] in case the host app wants
     * to overlay its own UI on top of the captured surface.
     */
    @Suppress("unused")
    fun surfaceFor(track: VideoTrack): Surface? = null
}
