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

enum class SignalQuality {
    NONE,
    WEAK,
    MEDIUM,
    STRONG
}

sealed interface ReceiverState {
    data object Idle : ReceiverState
    data object Joining : ReceiverState
    data object Signaling : ReceiverState
    data object IceChecking : ReceiverState
    data class Connected(val video: VideoTrack?, val audio: AudioTrack?, val signal: SignalQuality = SignalQuality.MEDIUM) : ReceiverState
    data class Error(val message: String) : ReceiverState
}

class ReceiverViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<ReceiverState>(ReceiverState.Idle)
    val state: StateFlow<ReceiverState> = _state.asStateFlow()

    private val _signalQuality = MutableStateFlow(SignalQuality.NONE)
    val signalQuality: StateFlow<SignalQuality> = _signalQuality.asStateFlow()

    private val api = SyncWaveApi(BuildConfigCompat.baseUrl())
    private var session: PeerSession? = null
    private var systemDecoder: SystemAudioDecoder? = null
    private var iceCheckStart: Long = 0

    fun join(code: String) {
        _state.value = ReceiverState.Joining
        _signalQuality.value = SignalQuality.NONE
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { api.joinRoom(code, "Guest") }
                .onFailure { _state.value = ReceiverState.Error(it.message ?: "join_failed"); return@launch }
                .onSuccess { resp ->
                    _state.value = ReceiverState.Signaling
                    WebRtcGlobals.init(getApplication())
                    val signaling = VercelLongPollSignaling(api, resp.roomId, resp.guestId)
                    val peer = PeerSession(getApplication(), signaling, PeerRole.GUEST).also { session = it }
                    peer.start(viewModelScope)
                    observe(peer)
                }
        }
    }

    private fun observe(peer: PeerSession) {
        viewModelScope.launch {
            peer.events.collect { ev ->
                when (ev) {
                    is PeerEvent.Connected -> {
                        _state.value = ReceiverState.Connected(
                            video = peer.remoteVideoTrack.value,
                            audio = peer.remoteAudioTrack.value,
                            signal = SignalQuality.STRONG
                        )
                        _signalQuality.value = SignalQuality.STRONG
                    }
                    is PeerEvent.Disconnected -> {
                        _signalQuality.value = SignalQuality.WEAK
                        if (_state.value is ReceiverState.Connected) {
                            _state.value = ReceiverState.IceChecking
                        }
                    }
                    is PeerEvent.Failure -> {
                        _state.value = ReceiverState.Error(ev.cause)
                        _signalQuality.value = SignalQuality.NONE
                    }
                    else -> Unit
                }
            }
        }

        viewModelScope.launch {
            combine(peer.remoteVideoTrack, peer.remoteAudioTrack) { video, audio ->
                if (video != null || audio != null) {
                    ReceiverState.Connected(video, audio, _signalQuality.value)
                } else {
                    _state.value
                }
            }.collect { newState ->
                if (newState is ReceiverState.Connected) {
                    _state.value = newState
                }
            }
        }

        viewModelScope.launch {
            peer.remoteAudioTrack.collect { audio ->
                if (audio != null && _state.value is ReceiverState.Connected) {
                    _state.value = (_state.value as ReceiverState.Connected).copy(audio = audio)
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
        _state.value = ReceiverState.Idle
        _signalQuality.value = SignalQuality.NONE
    }

    override fun onCleared() {
        leave()
        super.onCleared()
    }
}
