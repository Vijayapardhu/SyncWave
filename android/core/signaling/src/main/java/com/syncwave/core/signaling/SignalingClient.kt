package com.syncwave.core.signaling

import com.syncwave.core.network.dto.SignalEnvelopeDto
import com.syncwave.core.network.dto.SignalType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import org.json.JSONObject

/**
 * Transport-agnostic signaling contract. The concrete implementation only
 * needs to (a) deliver outgoing envelopes and (b) surface incoming envelopes
 * as a hot flow. PeerConnection wiring in [com.syncwave.core.webrtc.PeerSession]
 * depends only on this interface — Vercel, Firebase, Ably, or a self-hosted
 * WebSocket can all slot in here.
 */
interface SignalingClient {
    val incoming: SharedFlow<SignalEnvelopeDto>

    /** The peer's own id, used by the session to ignore its own echoed signals. */
    val selfPeerId: String

    /** Start the receive pump. Must be idempotent. */
    fun start(scope: CoroutineScope)

    /** Stop the receive pump. Safe to call when not started. */
    fun stop()

    /** Push an envelope. */
    suspend fun send(type: SignalType, to: String?, payload: JSONObject?)
}
