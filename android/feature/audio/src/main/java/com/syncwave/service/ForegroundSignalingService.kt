package com.syncwave.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.syncwave.core.network.BuildConfigCompat
import com.syncwave.core.network.SyncWaveApi
import com.syncwave.core.signaling.VercelLongPollSignaling
import com.syncwave.core.webrtc.MicAudioTrackFactory
import com.syncwave.core.webrtc.PeerEvent
import com.syncwave.core.webrtc.PeerRole
import com.syncwave.core.webrtc.PeerSession
import com.syncwave.core.webrtc.SystemAudioDecoder
import com.syncwave.core.webrtc.SystemAudioEncoder
import com.syncwave.feature.audio.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ForegroundSignalingService : Service() {
    private val binder = LocalBinder()
    private var session: PeerSession? = null
    private var signaling: VercelLongPollSignaling? = null
    private var api: SyncWaveApi? = null
    private var scope: CoroutineScope? = null
    private var job: Job? = null
    private var micFactory: MicAudioTrackFactory? = null
    private var systemDecoder: SystemAudioDecoder? = null

    inner class LocalBinder : Binder() {
        fun getService(): ForegroundSignalingService = this@ForegroundSignalingService
    }

    override fun onCreate() {
        super.onCreate()
        api = SyncWaveApi(BuildConfigCompat.baseUrl())
        micFactory = MicAudioTrackFactory(applicationContext)
    }

    override fun onBind(intent: Intent): IBinder = binder

    fun startHost(
        roomCode: String,
        hostId: String,
        appScope: CoroutineScope,
        mode: com.syncwave.feature.audio.AudioSourceMode,
        projectionData: Intent?
    ) {
        stopSession()
        scope = appScope
        val sig = VercelLongPollSignaling(requireApi(), roomCode, hostId)
        signaling = sig
        val peer = PeerSession(applicationContext, sig, PeerRole.HOST).also { session = it }
        peer.setPublishCapabilities(video = false, audio = true)
        peer.start(appScope)
        observe(peer)

        if (mode == com.syncwave.feature.audio.AudioSourceMode.SYSTEM && projectionData != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            Log.w(TAG, "host system audio path starting")
            try {
                val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as android.media.projection.MediaProjectionManager
                val projection = mgr.getMediaProjection(android.app.Activity.RESULT_OK, projectionData)
                Log.w(TAG, "media projection obtained")
                val dc = peer.createDataChannel("sysaudio")
                dc.registerObserver(object : org.webrtc.DataChannel.Observer {
                    override fun onBufferedAmountChange(previousAmount: Long) {}
                    override fun onStateChange() {
                        Log.w(TAG, "sysaudio datachannel state=${dc.state()}")
                        if (dc.state() == org.webrtc.DataChannel.State.OPEN) {
                            val encoder = SystemAudioEncoder(appScope, applicationContext)
                            encoder.start(projection, dc)
                        }
                    }
                    override fun onMessage(buffer: org.webrtc.DataChannel.Buffer?) {}
                })
                Log.w(TAG, "creating offer for system audio host")
                peer.createOffer()
                Log.w(TAG, "offer creation requested")
            } catch (t: Throwable) {
                Log.w(TAG, "host system audio path failed", t)
            }
        } else {
            Log.w(TAG, "host mic audio path starting")
            try {
                val track = micFactory!!.create()
                peer.attachLocalAudioTrack(track)
                peer.createOffer()
                Log.w(TAG, "offer creation requested")
            } catch (t: Throwable) {
                Log.w(TAG, "host mic audio path failed", t)
            }
        }
        Log.w(TAG, "host service started room=$roomCode mode=$mode")
    }

    fun startGuest(roomCode: String, guestId: String, appScope: CoroutineScope) {
        stopSession()
        scope = appScope
        val sig = VercelLongPollSignaling(requireApi(), roomCode, guestId)
        signaling = sig
        val peer = PeerSession(applicationContext, sig, PeerRole.GUEST).also { session = it }
        peer.setPublishCapabilities(video = false, audio = true)
        peer.start(appScope)
        observe(peer)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val decoder = SystemAudioDecoder(appScope)
            systemDecoder = decoder
            decoder.start()
            appScope.launch(Dispatchers.IO) {
                peer.incomingData.collect { bytes ->
                    decoder.submit(bytes)
                }
            }
        }
        Log.w(TAG, "guest service started room=$roomCode")
    }

    fun stopSession() {
        session?.close()
        signaling = null
        session = null
        systemDecoder?.stop()
        systemDecoder = null
        job?.cancel()
        job = null
    }

    override fun onDestroy() {
        stopSession()
        super.onDestroy()
    }

    private fun observe(peer: PeerSession) {
        job = (scope ?: CoroutineScope(Dispatchers.IO)).launch(Dispatchers.IO) {
            peer.events.collect { ev ->
                Log.w(TAG, "service peer event=$ev")
            }
        }
    }

    private fun requireApi(): SyncWaveApi {
        return api ?: error("SyncWaveApi not initialized")
    }

    companion object {
        const val TAG = "SyncWave/FgService"
    }
}
