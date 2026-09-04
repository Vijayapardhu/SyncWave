package com.syncwave.core.signaling

import com.syncwave.core.network.SyncWaveApi
import com.syncwave.core.network.dto.SignalEnvelopeDto
import com.syncwave.core.network.dto.SignalType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Polls the Vercel signaling endpoint on a short interval. Suitable for V0.1
 * where we deliberately want zero infrastructure beyond the Next.js app.
 * Latency is bounded by [pollIntervalMs].
 */
class VercelLongPollSignaling(
    private val api: SyncWaveApi,
    private val roomId: String,
    override val selfPeerId: String,
    private val pollIntervalMs: Long = POLL_INTERVAL_MS
) : SignalingClient {

    private val _incoming = MutableSharedFlow<SignalEnvelopeDto>(extraBufferCapacity = 64)
    override val incoming: SharedFlow<SignalEnvelopeDto> = _incoming.asSharedFlow()

    private var pumpJob: Job? = null

    override fun start(scope: CoroutineScope) {
        if (pumpJob?.isActive == true) return
        pumpJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val signals = api.pollSignals(roomId, selfPeerId)
                    for (s in signals) _incoming.emit(s)
                } catch (_: Throwable) {
                    // transient — try again next tick. Real apps would
                    // back off on consecutive failures.
                }
                delay(pollIntervalMs)
            }
        }
    }

    override fun stop() {
        pumpJob?.cancel()
        pumpJob = null
    }

    override suspend fun send(type: SignalType, to: String?, payload: JSONObject?) {
        api.sendSignal(roomId, selfPeerId, to, type, payload)
    }

    companion object {
        const val POLL_INTERVAL_MS = 500L
    }
}
