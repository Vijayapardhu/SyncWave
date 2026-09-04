package com.syncwave.feature.receiver

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.syncwave.core.network.BuildConfigCompat
import com.syncwave.core.network.SyncWaveApi
import com.syncwave.core.signaling.VercelLongPollSignaling
import com.syncwave.core.webrtc.PeerEvent
import com.syncwave.core.webrtc.PeerRole
import com.syncwave.core.webrtc.PeerSession
import com.syncwave.core.webrtc.SystemAudioDecoder
import com.syncwave.core.webrtc.WebRtcGlobals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.webrtc.AudioTrack
import org.webrtc.VideoTrack

sealed interface ReceiverState {
    data object Joining : ReceiverState
    data class Connected(val video: VideoTrack?, val audio: AudioTrack?) : ReceiverState
    data class Error(val message: String) : ReceiverState
}

class ReceiverViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<ReceiverState>(ReceiverState.Joining)
    val state: StateFlow<ReceiverState> = _state.asStateFlow()

    private val api = SyncWaveApi(BuildConfigCompat.baseUrl())
    private var session: PeerSession? = null
    private var systemDecoder: SystemAudioDecoder? = null

    fun join(code: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { api.joinRoom(code, "Guest") }
                .onFailure { _state.value = ReceiverState.Error(it.message ?: "join_failed"); return@launch }
                .onSuccess { resp ->
                    WebRtcGlobals.init(getApplication())
                    val signaling = VercelLongPollSignaling(api, resp.roomId, resp.guestId)
                    val peer = PeerSession(getApplication(), signaling, PeerRole.GUEST).also { session = it }
                    peer.start(viewModelScope)
                    observe(peer, resp.hostId)
                }
        }
    }

    private fun observe(peer: PeerSession, hostId: String) {
        // Observe events and error state
        viewModelScope.launch {
            peer.events.collect { ev ->
                if (ev is PeerEvent.Failure) {
                    _state.value = ReceiverState.Error(ev.cause)
                }
            }
        }
        
        // Combine video and audio track updates to avoid race conditions
        viewModelScope.launch {
            combine(peer.remoteVideoTrack, peer.remoteAudioTrack) { video, audio ->
                if (video != null || audio != null) {
                    ReceiverState.Connected(video, audio)
                } else {
                    _state.value
                }
            }.collect { newState ->
                if (newState is ReceiverState.Connected) {
                    _state.value = newState
                }
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            viewModelScope.launch {
                peer.incomingData.collect { bytes ->
                    val dec = systemDecoder ?: run {
                        val d = SystemAudioDecoder(viewModelScope).also { it.start() }
                        systemDecoder = d
                        d
                    }
                    dec.submit(bytes)
                }
            }
        }
    }

    fun leave() {
        systemDecoder?.stop()
        systemDecoder = null
        session?.close()
        session = null
    }

    override fun onCleared() {
        leave()
        super.onCleared()
    }
}
