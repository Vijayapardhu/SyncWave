package com.syncwave.core.network

import android.util.Log
import com.syncwave.core.network.dto.CreateRoomResponse
import com.syncwave.core.network.dto.JoinRoomResponse
import com.syncwave.core.network.dto.RoomStateResponse
import com.syncwave.core.network.dto.SignalEnvelopeDto
import com.syncwave.core.network.dto.SignalRequest
import com.syncwave.core.network.dto.SignalType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Thin REST client for the SyncWave signaling server.
 * The server only carries SDP/ICE JSON blobs; no media bytes ever cross it.
 */
class SyncWaveApi(
    private val baseUrl: String
) {
    private val TAG = "SyncWave/Api"

    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json".toMediaType()

    private fun url(path: String): String {
        val url = baseUrl + path
        Log.w(TAG, "url=$url")
        return url
    }

    suspend fun createRoom(deviceName: String): CreateRoomResponse = withContext(Dispatchers.IO) {
        val body = JSONObject().put("deviceName", deviceName).toString()
        Log.w(TAG, "createRoom body=$body")
        val res = post("/api/rooms/create", body)
        Log.w(TAG, "createRoom res=$res")
        CreateRoomResponse(
            code = res.getString("code"),
            roomId = res.optString("roomId", res.getString("code")),
            hostId = res.getString("hostId"),
            deviceName = res.optString("deviceName", deviceName),
            createdAt = res.optLong("createdAt", System.currentTimeMillis())
        )
    }

    suspend fun joinRoom(code: String, deviceName: String): JoinRoomResponse = withContext(Dispatchers.IO) {
        val body = JSONObject().put("deviceName", deviceName).toString()
        Log.w(TAG, "joinRoom code=$code body=$body")
        val res = post("/api/rooms/join/$code", body)
        Log.w(TAG, "joinRoom res=$res")
        JoinRoomResponse(
            code = res.getString("code"),
            roomId = res.optString("roomId", res.getString("code")),
            guestId = res.getString("guestId"),
            hostId = res.getString("hostId"),
            deviceName = res.optString("deviceName", deviceName)
        )
    }

    suspend fun roomState(code: String): RoomStateResponse = withContext(Dispatchers.IO) {
        val res = get("/api/rooms/$code")
        val participants = res.optJSONArray("participants") ?: JSONArray()
        RoomStateResponse(
            code = res.getString("code"),
            roomId = res.optString("roomId", res.getString("code")),
            hostId = res.optString("hostId", null).ifEmpty { null },
            participantCount = participants.length()
        )
    }

    suspend fun sendSignal(
        roomId: String,
        from: String,
        to: String?,
        type: SignalType,
        payload: JSONObject?
    ): Long = withContext(Dispatchers.IO) {
        val req = SignalRequest(type, from, to, roomId, payload?.toString())
        val body = JSONObject().apply {
            put("type", req.type.name)
            put("from", req.from)
            req.to?.let { put("to", it) }
            put("roomId", req.roomId)
            req.payloadJson?.let { put("payload", JSONObject(it)) }
        }.toString()
        Log.w(TAG, "sendSignal roomId=$roomId from=$from to=$to type=$type")
        val ts = post("/api/signaling", body).getLong("ts")
        Log.w(TAG, "sendSignal ok ts=$ts")
        ts
    }

    suspend fun pollSignals(roomId: String, peer: String): List<SignalEnvelopeDto> = withContext(Dispatchers.IO) {
        Log.w(TAG, "pollSignals roomId=$roomId peer=$peer")
        try {
            val res = get("/api/signaling?roomId=$roomId&peer=$peer")
            Log.w(TAG, "pollSignals res=$res")
            val arr = res.optJSONArray("signals") ?: return@withContext emptyList()
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                SignalEnvelopeDto(
                    type = SignalType.valueOf(o.getString("type")),
                    from = o.getString("from"),
                    to = o.optString("to", null)?.ifEmpty { null },
                    payloadJson = o.optJSONObject("payload")?.toString(),
                    ts = o.optLong("ts", 0L)
                )
            }
        } catch (t: Throwable) {
            Log.w(TAG, "pollSignals failed baseUrl=$baseUrl", t)
            emptyList()
        }
    }

    private fun get(path: String): JSONObject {
        val req = Request.Builder().url(baseUrl + path).get().build()
        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            check(resp.isSuccessful) { "GET $path failed: ${resp.code} $text" }
            return JSONObject(text)
        }
    }

    private fun post(path: String, body: String): JSONObject {
        val req = Request.Builder()
            .url(baseUrl + path)
            .post(body.toRequestBody(jsonType))
            .build()
        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            check(resp.isSuccessful) { "POST $path failed: ${resp.code} $text" }
            return JSONObject(text)
        }
    }
}
