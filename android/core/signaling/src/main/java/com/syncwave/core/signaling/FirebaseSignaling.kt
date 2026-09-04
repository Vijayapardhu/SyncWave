package com.syncwave.core.signaling

import com.syncwave.core.network.dto.SignalEnvelopeDto
import com.syncwave.core.network.dto.SignalType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject

/**
 * Placeholder for the Firebase Realtime Database implementation we expect to
 * wire up in V0.3. Today it just keeps the contract honest: the interface
 * compiles, the wiring in [com.syncwave.core.webrtc.PeerSession] does not
 * need to change when we add a real transport here.
 */
class FirebaseSignaling(
    private val roomId: String,
    override val selfPeerId: String
) : SignalingClient {
    private val _incoming = MutableSharedFlow<SignalEnvelopeDto>(extraBufferCapacity = 64)
    override val incoming: SharedFlow<SignalEnvelopeDto> = _incoming.asSharedFlow()

    override fun start(scope: CoroutineScope) {
        // TODO(V0.3): attach a ValueEventListener under /rooms/{roomId}/signals
        //             and emit parsed envelopes into _incoming.
    }

    override fun stop() {
        // TODO(V0.3): detach the listener.
    }

    override suspend fun send(type: SignalType, to: String?, payload: JSONObject?) {
        // TODO(V0.3): push {type, from, to, payload, ts} under
        //             /rooms/{roomId}/signals/{pushId}.
    }
}
