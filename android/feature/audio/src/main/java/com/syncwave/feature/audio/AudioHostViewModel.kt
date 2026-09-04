package com.syncwave.feature.audio

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.syncwave.core.network.BuildConfigCompat
import com.syncwave.core.network.SyncWaveApi
import com.syncwave.core.webrtc.PeerEvent
import com.syncwave.service.ForegroundSignalingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AudioSourceMode { MIC, SYSTEM }

sealed interface AudioHostState {
    data object Creating : AudioHostState
    data class Ready(val code: String, val mode: AudioSourceMode = AudioSourceMode.MIC) : AudioHostState
    data class Sharing(val code: String, val mode: AudioSourceMode = AudioSourceMode.MIC) : AudioHostState
    data class Error(val message: String) : AudioHostState
}

class AudioHostViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<AudioHostState>(AudioHostState.Creating)
    val state: StateFlow<AudioHostState> = _state.asStateFlow()

    private val api = SyncWaveApi(BuildConfigCompat.baseUrl())
    private var roomCode: String? = null
    private var hostId: String? = null
    private var service: ForegroundSignalingService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val svc = (binder as ForegroundSignalingService.LocalBinder).getService()
            service = svc
            bound = true
            val code = roomCode
            val hid = hostId
            if (code != null && hid != null) {
                val mode = pendingMode ?: AudioSourceMode.MIC
                val projection = pendingProjection
                svc.startHost(code, hid, viewModelScope, mode, projection)
                _state.value = AudioHostState.Sharing(code, mode)
                pendingMode = null
                pendingProjection = null
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            bound = false
        }
    }

    private var pendingMode: AudioSourceMode? = null
    private var pendingProjection: Intent? = null

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
        if (bound) return
        val code = roomCode ?: return
        val hid = hostId ?: return
        if (mode == AudioSourceMode.SYSTEM) {
            if (resultCode != android.app.Activity.RESULT_OK || data == null) {
                _state.value = AudioHostState.Error("media_projection_denied")
                return
            }
            pendingMode = AudioSourceMode.SYSTEM
            pendingProjection = data
        } else {
            pendingMode = AudioSourceMode.MIC
            pendingProjection = null
        }
        Log.w(TAG, "startSharing mode=$mode")
        val intent = Intent(getApplication<Application>(), ForegroundSignalingService::class.java)
        getApplication<Application>().bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun stopSharing() {
        if (bound) {
            getApplication<Application>().unbindService(connection)
            bound = false
        }
        service?.stopSession()
        service = null
        val code = roomCode
        if (code != null) _state.value = AudioHostState.Ready(code)
    }

    override fun onCleared() {
        stopSharing()
        super.onCleared()
    }

    companion object {
        const val TAG = "SyncWave/AudioHostVM"
    }
}

