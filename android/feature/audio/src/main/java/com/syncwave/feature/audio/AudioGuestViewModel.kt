package com.syncwave.feature.audio

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
import com.syncwave.core.webrtc.WebRtcGlobals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AudioGuestState {
    data object Idle : AudioGuestState
    data object Joining : AudioGuestState
    data object Listening : AudioGuestState
    data class Error(val message: String) : AudioGuestState
}

class AudioGuestViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<AudioGuestState>(AudioGuestState.Idle)
    val state: StateFlow<AudioGuestState> = _state.asStateFlow()

    private val api = SyncWaveApi(BuildConfigCompat.baseUrl())
    private var session: PeerSession? = null

    fun join(code: String) {
        _state.value = AudioGuestState.Joining
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { api.joinRoom(code, "GuestAudio") }
                .onFailure {
                    _state.value = AudioGuestState.Error(it.message ?: "join_failed"); return@launch
                }
                .onSuccess { resp ->
                    WebRtcGlobals.init(getApplication())
                    val signaling = VercelLongPollSignaling(api, resp.roomId, resp.guestId)
                    val peer = PeerSession(getApplication(), signaling, PeerRole.GUEST).also { session = it }
                    peer.setPublishCapabilities(video = false, audio = true)
                    peer.start(viewModelScope)
                    observe(peer)
                }
        }
    }

    private fun observe(peer: PeerSession) {
        viewModelScope.launch {
            peer.events.collect { ev ->
                when (ev) {
                    is PeerEvent.Connected -> _state.value = AudioGuestState.Listening
                    is PeerEvent.Disconnected -> _state.value = AudioGuestState.Error("disconnected")
                    is PeerEvent.Failure -> _state.value = AudioGuestState.Error(ev.cause)
                    null -> Unit
                }
            }
        }
    }

    fun leave() {
        session?.close()
        session = null
        _state.value = AudioGuestState.Idle
    }

    override fun onCleared() {
        leave()
        super.onCleared()
    }
}
