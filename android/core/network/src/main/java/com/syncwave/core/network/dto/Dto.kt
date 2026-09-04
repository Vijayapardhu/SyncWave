package com.syncwave.core.network.dto

data class CreateRoomResponse(
    val code: String,
    val roomId: String,
    val hostId: String,
    val deviceName: String,
    val createdAt: Long
)

data class JoinRoomResponse(
    val code: String,
    val roomId: String,
    val guestId: String,
    val hostId: String,
    val deviceName: String
)

data class JoinRoomError(val error: String)

data class RoomStateResponse(
    val code: String,
    val roomId: String,
    val hostId: String?,
    val participantCount: Int
)

enum class SignalType { OFFER, ANSWER, ICE, PRESENCE }

data class SignalRequest(
    val type: SignalType,
    val from: String,
    val to: String?,
    val roomId: String,
    val payloadJson: String?
)

data class SignalEnvelopeDto(
    val type: SignalType,
    val from: String,
    val to: String?,
    val payloadJson: String?,
    val ts: Long
)
