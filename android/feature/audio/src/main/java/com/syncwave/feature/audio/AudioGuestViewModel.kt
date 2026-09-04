package com.syncwave.feature.audio

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.syncwave.core.network.BuildConfigCompat
import com.syncwave.core.network.SyncWaveApi
import com.syncwave.core.webrtc.PeerEvent
import com.syncwave.core.webrtc.WebRtcGlobals
import com.syncwave.service.ForegroundSignalingService
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
    private var service: ForegroundSignalingService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            @Suppress("UNCHECKED_CAST")
            val svc = (binder as ForegroundSignalingService.LocalBinder).getService()
            service = svc
            bound = true
            svc.startGuest(currentRoomCode, currentGuestId, viewModelScope)
            _state.value = AudioGuestState.Listening
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            bound = false
        }
    }

    private var currentRoomCode: String = ""
    private var currentGuestId: String = ""

    fun join(code: String) {
        Log.w(TAG, "join requested code=$code")
        _state.value = AudioGuestState.Joining
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { api.joinRoom(code, "GuestAudio") }
                .onFailure { e ->
                    Log.w(TAG, "joinRoom failed", e)
                    _state.value = AudioGuestState.Error(e.message ?: "join_failed"); return@launch
                }
                .onSuccess { resp ->
                    Log.w(TAG, "joinRoom ok roomId=${resp.roomId} guestId=${resp.guestId}")
                    currentRoomCode = resp.roomId
                    currentGuestId = resp.guestId
                    WebRtcGlobals.init(getApplication())
                    val intent = Intent(getApplication(), ForegroundSignalingService::class.java)
                    getApplication<Application>().bindService(intent, connection, Context.BIND_AUTO_CREATE)
                }
        }
    }

    fun leave() {
        if (bound) {
            getApplication<Application>().unbindService(connection)
            bound = false
        }
        service?.stopSession()
        service = null
        _state.value = AudioGuestState.Idle
    }

    override fun onCleared() {
        leave()
        super.onCleared()
    }

    companion object {
        const val TAG = "SyncWave/AudioGuestVM"
    }
}
