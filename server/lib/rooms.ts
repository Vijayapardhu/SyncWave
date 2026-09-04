// Storage abstraction for rooms.
//
// On Vercel (production): uses Vercel KV (Redis). Reads/writes survive
// edge-instance cold starts and round-trips between edge regions.
//
// In local dev: if KV env vars are missing, falls back to an in-process
// Map so `npm run dev` works without setup. The fallback is also useful
// for unit tests.

import { kv } from "@vercel/kv";

export type Role = "host" | "guest";

export interface Participant {
  id: string;
  role: Role;
  joinedAt: number;
}

export interface Room {
  id: string;
  createdAt: number;
  hostId: string | null;
  participants: Participant[];
}

const ROOM_TTL_SECONDS = 6 * 60 * 60; // 6 hours

function hasKv(): boolean {
  // The @vercel/kv client throws on construction if env is missing.
  // We probe by checking the env directly — much cheaper than catching.
  return Boolean(process.env.KV_URL || process.env.KV_REST_API_URL);
}

// --- In-memory fallback (local dev only) ---------------------------------

const memRooms = new Map<string, Room>();

function memGet(code: string): Room | undefined {
  const r = memRooms.get(code);
  if (!r) return undefined;
  if (Date.now() - r.createdAt > ROOM_TTL_SECONDS * 1000) {
    memRooms.delete(code);
    return undefined;
  }
  return r;
}

function memPut(room: Room) {
  memRooms.set(room.id, room);
}

function memAddParticipant(code: string, p: Participant): Room | undefined {
  const r = memGet(code);
  if (!r) return undefined;
  if (!r.participants.some(x => x.id === p.id)) r.participants.push(p);
  memPut(r);
  return r;
}

function memRemoveParticipant(code: string, peerId: string): Room | undefined {
  const r = memGet(code);
  if (!r) return undefined;
  r.participants = r.participants.filter(p => p.id !== peerId);
  if (r.hostId === peerId) r.hostId = null;
  if (r.participants.length === 0) memRooms.delete(code);
  else memPut(r);
  return r;
}

// --- Public API ----------------------------------------------------------

export async function createRoom(code: string, hostId: string): Promise<Room> {
  const room: Room = {
    id: code,
    createdAt: Date.now(),
    hostId,
    participants: [{ id: hostId, role: "host", joinedAt: Date.now() }],
  };

  if (!hasKv()) {
    memPut(room);
    return room;
  }

  // Hash stores scalar fields; participants are kept inline. @vercel/kv
  // auto-serializes object values as JSON.
  await kv.hset(`room:${code}`, {
    id: room.id,
    createdAt: room.createdAt,
    hostId: room.hostId,
    participants: room.participants as unknown as Record<string, unknown>,
  });
  await kv.expire(`room:${code}`, ROOM_TTL_SECONDS);
  return room;
}

export async function getRoom(code: string): Promise<Room | undefined> {
  if (!hasKv()) return memGet(code);

  const data = await kv.hgetall<{
    id?: string;
    createdAt?: number | string;
    hostId?: string | null;
    participants?: string | Participant[];
  }>(`room:${code}`);
  if (!data || !data.id) return undefined;

  // Stale-guard: if the TTL window has somehow drifted, treat as missing.
  const created = Number(data.createdAt);
  if (Date.now() - created > ROOM_TTL_SECONDS * 1000) return undefined;

  // @vercel/kv auto-parses JSON-shaped values returned by hgetall, so
  // `participants` may already be an array. Handle both shapes.
  let participants: Participant[] = [];
  const raw = data.participants;
  if (Array.isArray(raw)) {
    participants = raw;
  } else if (typeof raw === "string" && raw.length > 0) {
    try { participants = JSON.parse(raw); } catch { participants = []; }
  }

  return {
    id: data.id,
    createdAt: created,
    hostId: data.hostId ?? null,
    participants,
  };
}

export async function addParticipant(code: string, p: Participant): Promise<Room | undefined> {
  if (!hasKv()) return memAddParticipant(code, p);

  const room = await getRoom(code);
  if (!room) return undefined;
  if (!room.participants.some(x => x.id === p.id)) room.participants.push(p);
  // @vercel/kv serializes objects passed to hset as JSON, so we can pass
  // the array directly. If a future client returns it pre-parsed, our
  // reader already handles both shapes.
  await kv.hset(`room:${code}`, { participants: room.participants as unknown as Record<string, unknown> });
  return room;
}

export async function removeParticipant(code: string, peerId: string): Promise<Room | undefined> {
  if (!hasKv()) return memRemoveParticipant(code, peerId);

  const room = await getRoom(code);
  if (!room) return undefined;
  room.participants = room.participants.filter(p => p.id !== peerId);
  if (room.hostId === peerId) room.hostId = null;
  if (room.participants.length === 0) {
    await kv.del(`room:${code}`);
    return room;
  }
  await kv.hset(`room:${code}`, { participants: room.participants as unknown as Record<string, unknown>, hostId: room.hostId });
  return room;
}

export function generateRoomCode(): string {
  const alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  let out = "";
  for (let i = 0; i < 6; i++) {
    out += alphabet[Math.floor(Math.random() * alphabet.length)];
  }
  return out;
}

// Re-export the KV availability so route handlers can decide whether to
// include a "running with in-memory store" warning in dev.
export const usingInMemoryStore = (): boolean => !hasKv();
