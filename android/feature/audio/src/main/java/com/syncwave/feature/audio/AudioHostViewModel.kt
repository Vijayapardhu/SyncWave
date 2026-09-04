package com.syncwave.feature.audio

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.syncwave.core.media.MediaProjectionRequester
import com.syncwave.core.network.BuildConfigCompat
import com.syncwave.core.network.SyncWaveApi
import com.syncwave.core.signaling.VercelLongPollSignaling
import com.syncwave.core.webrtc.MicAudioTrackFactory
import com.syncwave.core.webrtc.PeerEvent
import com.syncwave.core.webrtc.PeerRole
import com.syncwave.core.webrtc.PeerSession
import com.syncwave.core.webrtc.SystemAudioEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AudioSourceMode { MIC, SYSTEM }

sealed interface AudioHostState {
    data object Creating : AudioHostState
    data class Ready(val code: String, val mode: AudioSourceMode) : AudioHostState
    data class Sharing(val code: String, val mode: AudioSourceMode) : AudioHostState
    data class Error(val message: String) : AudioHostState
}

class AudioHostViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<AudioHostState>(AudioHostState.Creating)
    val state: StateFlow<AudioHostState> = _state.asStateFlow()

    val projectionRequester = MediaProjectionRequester(app)
    private val micFactory = MicAudioTrackFactory(app.applicationContext)

    private val api = SyncWaveApi(BuildConfigCompat.baseUrl())
    private var roomCode: String? = null
    private var hostId: String? = null
    private var session: PeerSession? = null
    private var systemEncoder: SystemAudioEncoder? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { api.createRoom("HostAudio") }
                .onSuccess { r ->
                    roomCode = r.code
                    hostId = r.hostId
                    _state.value = AudioHostState.Ready(r.code, AudioSourceMode.MIC)
                }
                .onFailure { _state.value = AudioHostState.Error(it.message ?: "create_failed") }
        }
    }

    fun setMode(mode: AudioSourceMode) {
        val s = _state.value
        if (s is AudioHostState.Ready) {
            _state.value = s.copy(mode = mode)
        } else if (s is AudioHostState.Sharing) {
            _state.value = s.copy(mode = mode)
        }
    }

    fun startSharing(resultCode: Int, data: Intent?, mode: AudioSourceMode) {
        val code = roomCode ?: return
        val hid = hostId ?: return
        if (resultCode != android.app.Activity.RESULT_OK || data == null) {
            _state.value = AudioHostState.Error("permission_denied")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val signaling = VercelLongPollSignaling(api, code, hid)
                val peer = PeerSession(getApplication(), signaling, PeerRole.HOST).also { session = it }
                peer.setPublishCapabilities(video = false, audio = true)
                peer.start(viewModelScope)
                observe(peer)

                when (mode) {
                    AudioSourceMode.MIC -> {
                        val track = micFactory.create()
                        peer.attachLocalAudioTrack(track)
                    }
                    AudioSourceMode.SYSTEM -> {
                        require(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            "System audio capture requires Android 10 (API 29) or later"
                        }
                        val projection = projectionRequester.tryObtainProjection(resultCode, data)
                            ?: error("MediaProjection unavailable")
                        val dc = peer.createDataChannel("syncwave-system-audio")
                        val encoder = SystemAudioEncoder(viewModelScope)
                        systemEncoder = encoder
                        // DataChannel may be CONNECTING at this point; the encoder
                        // waits for OPEN internally before pushing frames.
                        viewModelScope.launch {
                            dc.registerObserver(object : org.webrtc.DataChannel.Observer {
                                override fun onBufferedAmountChange(previousAmount: Long) {}
                                override fun onStateChange() {
                                    if (dc.state() == org.webrtc.DataChannel.State.OPEN) {
                                        encoder.start(projection, dc)
                                    }
                                }
                                override fun onMessage(buffer: org.webrtc.DataChannel.Buffer?) {}
                            })
                        }
                    }
                }

                peer.createOffer()
                _state.value = AudioHostState.Sharing(code, mode)
            }.onFailure { _state.value = AudioHostState.Error(it.message ?: "start_failed") }
        }
    }

    fun stopSharing() {
        systemEncoder?.stop()
        systemEncoder = null
        session?.close()
        session = null
        val code = roomCode
        val s = _state.value
        val mode = when (s) {
            is AudioHostState.Sharing -> s.mode
            is AudioHostState.Ready -> s.mode
            else -> AudioSourceMode.MIC
        }
        if (code != null) _state.value = AudioHostState.Ready(code, mode)
    }

    private fun observe(peer: PeerSession) {
        viewModelScope.launch {
            peer.events.collect { ev ->
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
}
