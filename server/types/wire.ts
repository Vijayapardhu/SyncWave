// Shared wire contract between the Android app and the Vercel signaling
// layer. Keep this file in sync with android/core/network/dto/Dto.kt.
//
// "code" is the human-shareable room id (e.g. "7K4P2A").
// "roomId" is the same value today; we keep both names so future migrations
// (e.g. multi-tenant) are easy.
//
// Signaling payload is always an opaque JSON object — the server does not
// interpret it.

export type CreateRoomResponse = {
  code: string;
  roomId: string;
  hostId: string;
  deviceName: string;
  createdAt: number;
};

export type JoinRoomResponse = {
  success: true;
  code: string;
  roomId: string;
  guestId: string;
  hostId: string;
  deviceName: string;
};

export type JoinRoomError =
  | { success: false; error: "room_not_found" | "host_gone" | "bad_request" };

export type SignalType = "OFFER" | "ANSWER" | "ICE" | "PRESENCE";

export type SignalEnvelope = {
  type: SignalType;
  from: string;
  to?: string;
  roomId: string; // same as code
  payload?: unknown;
  ts: number;
};

export type PollResponse = {
  signals: SignalEnvelope[];
};
