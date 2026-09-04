package com.syncwave.feature.audio

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.syncwave.core.network.BuildConfigCompat
import com.syncwave.core.network.SyncWaveApi
import com.syncwave.core.signaling.VercelLongPollSignaling
import com.syncwave.core.webrtc.MicAudioTrackFactory
import com.syncwave.core.webrtc.PeerEvent
import com.syncwave.core.webrtc.PeerRole
import com.syncwave.core.webrtc.PeerSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AudioSourceMode { MIC }

sealed interface AudioHostState {
    data object Creating : AudioHostState
    data class Ready(val code: String, val mode: AudioSourceMode = AudioSourceMode.MIC) : AudioHostState
    data class Sharing(val code: String, val mode: AudioSourceMode = AudioSourceMode.MIC) : AudioHostState
    data class Error(val message: String) : AudioHostState
}

class AudioHostViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<AudioHostState>(AudioHostState.Creating)
    val state: StateFlow<AudioHostState> = _state.asStateFlow()

    private val micFactory = MicAudioTrackFactory(app.applicationContext)

    private val api = SyncWaveApi(BuildConfigCompat.baseUrl())
    private var roomCode: String? = null
    private var hostId: String? = null
    private var session: PeerSession? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { api.createRoom("HostAudio") }
                .onSuccess { r ->
                    roomCode = r.code
                    hostId = r.hostId
                    Log.w(TAG, "room created code=${r.code} hostId=${r.hostId}")
                    _state.value = AudioHostState.Ready(r.code)
                }
                .onFailure { e ->
                    Log.w(TAG, "createRoom failed", e)
                    _state.value = AudioHostState.Error(e.message ?: "create_failed")
                }
        }
    }

    fun startSharing(resultCode: Int, data: android.content.Intent?, mode: AudioSourceMode = AudioSourceMode.MIC) {
        if (session != null) return
        val code = roomCode ?: return
        val hid = hostId ?: return
        if (resultCode != android.app.Activity.RESULT_OK) {
            _state.value = AudioHostState.Error("permission_denied")
            return
        }
        Log.w(TAG, "startSharing mode=$mode")
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val signaling = VercelLongPollSignaling(api, code, hid)
                val peer = PeerSession(getApplication(), signaling, PeerRole.HOST).also { session = it }
                peer.setPublishCapabilities(video = false, audio = true)
                peer.start(viewModelScope)
                observe(peer)

                val track = micFactory.create()
                peer.attachLocalAudioTrack(track)

                peer.createOffer()
                Log.w(TAG, "offer created")
                _state.value = AudioHostState.Sharing(code)
            }.onFailure { e ->
                Log.w(TAG, "startSharing failed", e)
                _state.value = AudioHostState.Error(e.message ?: "start_failed")
            }
        }
    }

    fun stopSharing() {
        session?.close()
        session = null
        val code = roomCode
        if (code != null) _state.value = AudioHostState.Ready(code)
    }

    private fun observe(peer: PeerSession) {
        viewModelScope.launch {
            peer.events.collect { ev ->
                Log.w(TAG, "host peer event=$ev")
                if (ev is PeerEvent.Failure) {
                    _state.value = AudioHostState.Error(ev.cause)
                }
            }
        }
    }

    override fun onCleared() {
        stopSharing()
        super.onCleared()
    }

    companion object {
        const val TAG = "SyncWave/AudioHostVM"
    }
}
