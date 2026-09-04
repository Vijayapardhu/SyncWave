import { kv } from "@vercel/kv";

export type SignalType = "OFFER" | "ANSWER" | "ICE" | "PRESENCE";

export interface SignalEnvelope {
  type: SignalType;
  from: string;
  to?: string;
  roomId: string;
  payload?: unknown;
  ts: number;
}

const MAX_BUFFER = 50;
const SIGNAL_TTL_SECONDS = 6 * 60 * 60; // match room TTL

function hasKv(): boolean {
  return Boolean(process.env.KV_URL || process.env.KV_REST_API_URL);
}

// --- In-memory fallback ---------------------------------------------------

const memSignals = new Map<string, SignalEnvelope[]>();

function memBuffer(roomId: string, signal: SignalEnvelope) {
  const list = memSignals.get(roomId) ?? [];
  list.push(signal);
  if (list.length > MAX_BUFFER) list.shift();
  memSignals.set(roomId, list);
}

function memDrain(roomId: string, forPeer: string): SignalEnvelope[] {
  const list = memSignals.get(roomId) ?? [];
  const remaining: SignalEnvelope[] = [];
  const delivered: SignalEnvelope[] = [];
  for (const s of list) {
    if (s.from === forPeer) continue;
    if (s.to === undefined || s.to === null || s.to === forPeer) {
      delivered.push(s);
    } else {
      remaining.push(s);
    }
  }
  memSignals.set(roomId, remaining);
  return delivered;
}

// --- Public API -----------------------------------------------------------

export async function bufferSignal(roomId: string, signal: SignalEnvelope): Promise<void> {
  if (!hasKv()) {
    memBuffer(roomId, signal);
    return;
  }

  const key = `signals:${roomId}`;
  const raw = JSON.stringify(signal);
  await kv.lpush(key, raw);
  await kv.ltrim(key, 0, MAX_BUFFER - 1);
  await kv.expire(key, SIGNAL_TTL_SECONDS);
}

export async function drainSignals(roomId: string, forPeer: string): Promise<SignalEnvelope[]> {
  if (!hasKv()) {
    const mem = memDrain(roomId, forPeer);
    console.log(`[signaling] memDrain room=${roomId} peer=${forPeer} count=${mem.length}`);
    return mem;
  }

  const key = `signals:${roomId}`;
  const raw = (await kv.lrange(key, 0, -1)) as Array<string | SignalEnvelope> | null;
  console.log(`[signaling] drain room=${roomId} peer=${forPeer} rawCount=${raw?.length ?? 0}`);
  if (!raw || raw.length === 0) return [];

  const all: SignalEnvelope[] = raw.map(s =>
    typeof s === "string" ? (JSON.parse(s) as SignalEnvelope) : s
  );
  const delivered: SignalEnvelope[] = [];
  const keepStrings: string[] = [];

  for (let i = 0; i < all.length; i++) {
    const env = all[i];
    const original = raw[i];
    if (env.from === forPeer) continue;
    if (env.to === undefined || env.to === null || env.to === forPeer) {
      delivered.push(env);
    } else {
      keepStrings.push(typeof original === "string" ? original : JSON.stringify(original));
    }
  }

  console.log(`[signaling] drain room=${roomId} peer=${forPeer} delivered=${delivered.length} kept=${keepStrings.length}`);

  if (keepStrings.length === 0) {
    await kv.del(key);
  } else {
    await kv.del(key);
    await kv.lpush(key, ...keepStrings);
    await kv.expire(key, SIGNAL_TTL_SECONDS);
  }

  return delivered;
}
