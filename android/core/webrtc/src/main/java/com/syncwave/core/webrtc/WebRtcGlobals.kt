package com.syncwave.core.webrtc

import android.content.Context
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.Loggable
import org.webrtc.Logging
import org.webrtc.PeerConnectionFactory
import org.webrtc.audio.JavaAudioDeviceModule

/**
 * Owns the process-wide WebRTC globals. Must be initialised before any
 * PeerConnection is created.
 */
object WebRtcGlobals {
    @Volatile private var initialised = false
    lateinit var eglBase: EglBase
        private set
    lateinit var factory: PeerConnectionFactory
        private set

    fun init(context: Context) {
        if (initialised) return
        synchronized(this) {
            if (initialised) return
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                    .setEnableInternalTracer(true)
                    .setInjectableLogger(AndroidLogger, Logging.Severity.LS_WARNING)
                    .createInitializationOptions()
            )
            eglBase = EglBase.create()
            val audio = JavaAudioDeviceModule.builder(context).createAudioDeviceModule()
            factory = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(
                    DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
                )
                .setVideoDecoderFactory(
                    DefaultVideoDecoderFactory(eglBase.eglBaseContext)
                )
                .setAudioDeviceModule(audio)
                .createPeerConnectionFactory()
            initialised = true
        }
    }
}

private object AndroidLogger : Loggable {
    override fun onLogMessage(message: String?, severity: Logging.Severity?, tag: String?) {
        if (message == null || severity == null) return
        if (severity.ordinal >= Logging.Severity.LS_WARNING.ordinal) {
            android.util.Log.w("SyncWaveWebRtc", message)
        }
    }
}
