package com.syncwave.feature.host

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.syncwave.core.media.MediaProjectionRequester
import com.syncwave.core.network.BuildConfigCompat
import com.syncwave.core.network.SyncWaveApi
import com.syncwave.core.signaling.VercelLongPollSignaling
import com.syncwave.core.webrtc.PeerEvent
import com.syncwave.core.webrtc.PeerRole
import com.syncwave.core.webrtc.PeerSession
import com.syncwave.core.webrtc.ScreenTrackFactory
import com.syncwave.core.media.ScreenCaptureConfigFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.webrtc.VideoTrack

sealed interface HostState {
    data object Creating : HostState
    data class Ready(val code: String) : HostState
    data class Sharing(val code: String) : HostState
    data class Error(val message: String) : HostState
}

class HostViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<HostState>(HostState.Creating)
    val state: StateFlow<HostState> = _state.asStateFlow()

    val projectionRequester = MediaProjectionRequester(app)
    private val screenFactory = ScreenTrackFactory(app.applicationContext)

    private val api = SyncWaveApi(BuildConfigCompat.baseUrl())

    private var roomCode: String? = null
    private var hostId: String? = null
    private var session: PeerSession? = null
    private var localTrack: VideoTrack? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { api.createRoom("Host") }
                .onSuccess { r ->
                    roomCode = r.code
                    hostId = r.hostId
                    _state.value = HostState.Ready(r.code)
                }
                .onFailure { _state.value = HostState.Error(it.message ?: "create_failed") }
        }
    }

    fun startSharing(resultCode: Int, data: Intent?) {
        val code = roomCode ?: return
        val hid = hostId ?: return
        if (resultCode != android.app.Activity.RESULT_OK || data == null) {
            _state.value = HostState.Error("permission_denied")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val signaling = VercelLongPollSignaling(api, code, hid)
                val peer = PeerSession(getApplication(), signaling, PeerRole.HOST).also { session = it }
                peer.start(viewModelScope)
                observe(peer)

                val cfg = ScreenCaptureConfigFactory.fromContext(getApplication())
                val track = screenFactory.create(data, cfg).also { localTrack = it }
                peer.attachLocalVideoTrack(track)
                peer.createOffer()
                _state.value = HostState.Sharing(code)
            }.onFailure { _state.value = HostState.Error(it.message ?: "start_failed") }
        }
    }

    fun stopSharing() {
        localTrack?.dispose()
        localTrack = null
        session?.close()
        session = null
        roomCode?.let { code -> _state.value = HostState.Ready(code) }
    }

    private fun observe(peer: PeerSession) {
        viewModelScope.launch {
            peer.events.collect { ev ->
                when (ev) {
                    is PeerEvent.Failure -> _state.value = HostState.Error(ev.cause)
                    else -> Unit
                }
            }
        }
    }

    override fun onCleared() {
        stopSharing()
        super.onCleared()
    }
}
