package com.syncwave.feature.audio

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
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
import kotlinx.coroutines.launch

sealed interface AudioGuestState {
    data object Idle : AudioGuestState
    data object Joining : AudioGuestState
    data object Listening : AudioGuestState
    data class Error(val message: String) : AudioGuestState
}

/**
 * Lightweight guest that joins a room and plays back system-audio Opus frames
 * pushed down the host's DataChannel. Lives in `:feature:audio` so the
 * audio-only flow is end-to-end testable without depending on the
 * (intentionally broken) `:feature:receiver` module.
 */
class AudioGuestViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<AudioGuestState>(AudioGuestState.Idle)
    val state: StateFlow<AudioGuestState> = _state.asStateFlow()

    private val api = SyncWaveApi(BuildConfigCompat.baseUrl())
    private var session: PeerSession? = null
    private var decoder: SystemAudioDecoder? = null

    fun join(code: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            _state.value = AudioGuestState.Error("requires_android_10")
            return
        }
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
                    peer.start(viewModelScope)
                    observe(peer)
                }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun observe(peer: PeerSession) {
        viewModelScope.launch {
            peer.events.collect { ev ->
                if (ev is PeerEvent.Failure) {
                    _state.value = AudioGuestState.Error(ev.cause)
                }
            }
        }
        viewModelScope.launch {
            peer.incomingData.collect { bytes ->
                val d = decoder ?: SystemAudioDecoder(viewModelScope).also {
                    decoder = it
                    it.start()
                    _state.value = AudioGuestState.Listening
                }
                d.submit(bytes)
            }
        }
    }

    fun leave() {
        decoder?.stop()
        decoder = null
        session?.close()
        session = null
        _state.value = AudioGuestState.Idle
    }

    override fun onCleared() {
        leave()
        super.onCleared()
    }
}
