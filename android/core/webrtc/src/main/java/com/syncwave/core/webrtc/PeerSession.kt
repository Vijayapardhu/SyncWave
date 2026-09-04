package com.syncwave.core.webrtc

import android.content.Context
import android.util.Log
import com.syncwave.core.network.dto.SignalEnvelopeDto
import com.syncwave.core.network.dto.SignalType
import com.syncwave.core.signaling.SignalingClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.VideoTrack

enum class PeerRole { HOST, GUEST }

sealed interface PeerEvent {
    data object Connected : PeerEvent
    data object Disconnected : PeerEvent
    data class Failure(val cause: String) : PeerEvent
}

/**
 * Owns a [PeerConnection] and drives the SDP/ICE handshake through an
 * injected [SignalingClient]. Feature modules only see [PeerSession]; they
 * never import libwebrtc types.
 */
class PeerSession(
    private val context: Context,
    private val signaling: SignalingClient,
    val role: PeerRole
) {
    private val _events = MutableStateFlow<PeerEvent?>(null)
    val events: StateFlow<PeerEvent?> = _events.asStateFlow()

    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack: StateFlow<VideoTrack?> = _remoteVideoTrack.asStateFlow()

    private val _remoteAudioTrack = MutableStateFlow<AudioTrack?>(null)
    val remoteAudioTrack: StateFlow<AudioTrack?> = _remoteAudioTrack.asStateFlow()

    private val _incomingData = MutableSharedFlow<ByteArray>(extraBufferCapacity = 128)
    val incomingData: SharedFlow<ByteArray> = _incomingData.asSharedFlow()

    private var dataChannel: DataChannel? = null

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
    )

    // Scope owned by the caller (e.g. viewModelScope). All WebRTC callbacks
    // that need to do work — sending SDP/ICE, applying remote descriptions —
    // dispatch through this scope. Using GlobalScope here would leak the
    // signaling client past close().
    private var scope: CoroutineScope? = null

    private val pc: PeerConnection by lazy {
        WebRtcGlobals.factory.createPeerConnection(
            iceServers,
            mediaConstraints(),
            observer
        ) ?: error("Failed to create PeerConnection")
    }

    private var publishVideo: Boolean = true
    private var publishAudio: Boolean = false

    fun setPublishCapabilities(video: Boolean, audio: Boolean) {
        publishVideo = video
        publishAudio = audio
    }

    private fun mediaConstraints(): MediaConstraints {
        // The offerer (host in V0.1) only sends what the user opted into. The
        // guest side gets transceivers automatically when applying the offer.
        val offerToReceiveAudio = role == PeerRole.GUEST
        val offerToReceiveVideo = role == PeerRole.GUEST && publishVideo
        return MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", offerToReceiveAudio.toString()))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", offerToReceiveVideo.toString()))
        }
    }

    private val observer = object : PeerConnection.Observer {
        override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
            Log.d(TAG, "signalingState=$newState")
        }
        override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
            Log.d(TAG, "iceConnectionState=$newState")
            when (newState) {
                PeerConnection.IceConnectionState.CONNECTED,
                PeerConnection.IceConnectionState.COMPLETED -> _events.value = PeerEvent.Connected
                PeerConnection.IceConnectionState.DISCONNECTED -> _events.value = PeerEvent.Disconnected
                PeerConnection.IceConnectionState.FAILED -> _events.value = PeerEvent.Failure("ice_failed")
                else -> Unit
            }
        }
        override fun onIceConnectionReceivingChange(receiving: Boolean) {}
        override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {}
        override fun onIceCandidate(candidate: IceCandidate) {
            Log.d(TAG, "iceCandidate mid=${candidate.sdpMid} idx=${candidate.sdpMLineIndex}")
            scope?.launch {
                runCatching {
                    signaling.send(
                        type = SignalType.ICE,
                        to = null,
                        payload = JSONObject().apply {
                            put("sdpMid", candidate.sdpMid)
                            put("sdpMLineIndex", candidate.sdpMLineIndex)
                            put("candidate", candidate.sdp)
                        }
                    )
                }.onFailure { Log.w(TAG, "send ice failed", it) }
            }
        }
        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
        override fun onAddStream(stream: MediaStream?) {
            stream?.videoTracks?.firstOrNull()?.let { _remoteVideoTrack.value = it }
        }
        override fun onRemoveStream(stream: MediaStream?) {}
        override fun onDataChannel(dc: DataChannel?) {
            dc ?: return
            dataChannel = dc
            dc.registerObserver(object : DataChannel.Observer {
                override fun onBufferedAmountChange(previousAmount: Long) {}
                override fun onStateChange() {
                    Log.d(TAG, "datachannel state=${dc.state()}")
                }
                override fun onMessage(buffer: DataChannel.Buffer?) {
                    if (buffer == null) return
                    val bytes = ByteArray(buffer.data.remaining())
                    buffer.data.get(bytes)
                    scope?.launch { _incomingData.emit(bytes) }
                }
            })
        }
        override fun onRenegotiationNeeded() {}
        override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
            when (val t = receiver?.track()) {
                is VideoTrack -> _remoteVideoTrack.value = t
                is AudioTrack -> _remoteAudioTrack.value = t
            }
        }
        override fun onTrack(transceiver: RtpTransceiver?) {
            when (val t = transceiver?.receiver?.track()) {
                is VideoTrack -> _remoteVideoTrack.value = t
                is AudioTrack -> _remoteAudioTrack.value = t
            }
        }
    }

    private val sdpObserver = object : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription) {
            Log.d(TAG, "sdp created type=${sdp.type}")
            pc.setLocalDescription(this, sdp)
            scope?.launch {
                runCatching {
                    val wireType = if (sdp.type == SessionDescription.Type.OFFER)
                        SignalType.OFFER else SignalType.ANSWER
                    signaling.send(
                        type = wireType,
                        to = null,
                        payload = JSONObject()
                            .put("sdp", sdp.description)
                            .put("type", sdp.type.canonicalForm())
                    )
                }.onFailure { Log.w(TAG, "send sdp failed", it) }
            }
        }
        override fun onSetSuccess() { Log.d(TAG, "sdp set ok") }
        override fun onCreateFailure(error: String?) { _events.value = PeerEvent.Failure("sdp_create:$error") }
        override fun onSetFailure(error: String?) { _events.value = PeerEvent.Failure("sdp_set:$error") }
    }

    fun start(scope: CoroutineScope) {
        this.scope = scope
        WebRtcGlobals.init(context)
        signaling.start(scope)
        scope.launch {
            signaling.incoming.collect { env ->
                if (env.from == selfPeerId()) {
                    Log.d(TAG, "skipping own signal ${env.type}")
                    return@collect
                }
                handle(env)
            }
        }
    }

    fun createOffer() {
        pc.createOffer(sdpObserver, mediaConstraints())
    }

    fun attachLocalVideoTrack(track: VideoTrack) {
        pc.addTrack(track, listOf("ARDAMS"))
    }

    fun attachLocalAudioTrack(track: AudioTrack) {
        pc.addTrack(track, listOf("ARDAMS"))
    }

    /**
     * Create a reliable DataChannel for custom payloads (e.g. system audio
     * Opus frames). The label becomes the mDNS/SDP name.
     */
    fun createDataChannel(label: String): DataChannel {
        val dc = pc.createDataChannel(label, DataChannel.Init().apply {
            ordered = true
            maxRetransmits = -1
        })
        dataChannel = dc
        dc.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(previousAmount: Long) {}
            override fun onStateChange() {
                Log.d(TAG, "local datachannel state=${dc.state()}")
            }
            override fun onMessage(buffer: DataChannel.Buffer?) {
                if (buffer == null) return
                val bytes = ByteArray(buffer.data.remaining())
                buffer.data.get(bytes)
                scope?.launch { _incomingData.emit(bytes) }
            }
        })
        return dc
    }

    fun close() {
        signaling.stop()
        runCatching { pc.close() }
        scope = null
    }

    private fun selfPeerId(): String = signaling.selfPeerId

    private fun handle(env: SignalEnvelopeDto) {
        Log.d(TAG, "handle ${env.type} from=${env.from}")
        when (env.type) {
            SignalType.OFFER -> handleRemoteOffer(env.payloadJson)
            SignalType.ANSWER -> handleRemoteAnswer(env.payloadJson)
            SignalType.ICE -> handleRemoteIce(env.payloadJson)
            SignalType.PRESENCE -> Unit
        }
    }

    private fun handleRemoteOffer(payload: String?) {
        payload ?: return
        val obj = JSONObject(payload)
        val remote = SessionDescription(
            SessionDescription.Type.fromCanonicalForm(obj.getString("type")),
            obj.getString("sdp")
        )
        pc.setRemoteDescription(sdpObserver, remote)
        pc.createAnswer(sdpObserver, mediaConstraints())
    }

    private fun handleRemoteAnswer(payload: String?) {
        payload ?: return
        val obj = JSONObject(payload)
        val remote = SessionDescription(
            SessionDescription.Type.fromCanonicalForm(obj.getString("type")),
            obj.getString("sdp")
        )
        pc.setRemoteDescription(sdpObserver, remote)
    }

    private fun handleRemoteIce(payload: String?) {
        payload ?: return
        val obj = JSONObject(payload)
        val cand = IceCandidate(
            obj.getString("sdpMid"),
            obj.getInt("sdpMLineIndex"),
            obj.getString("candidate")
        )
        pc.addIceCandidate(cand)
    }

    private companion object {
        const val TAG = "SyncWave/Peer"
    }
}
