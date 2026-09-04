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
  await kv.lpush(key, JSON.stringify(signal));
  await kv.ltrim(key, 0, MAX_BUFFER - 1);
  await kv.expire(key, SIGNAL_TTL_SECONDS);
}

export async function drainSignals(roomId: string, forPeer: string): Promise<SignalEnvelope[]> {
  if (!hasKv()) return memDrain(roomId, forPeer);

  const key = `signals:${roomId}`;
  // Read all currently-buffered envelopes, oldest first. @vercel/kv may
  // either return raw strings or auto-parsed JSON objects.
  const raw = (await kv.lrange(key, 0, -1)) as Array<string | SignalEnvelope> | null;
  if (!raw || raw.length === 0) return [];

  const all: SignalEnvelope[] = raw.map(s =>
    typeof s === "string" ? (JSON.parse(s) as SignalEnvelope) : s
  );
  const delivered: SignalEnvelope[] = [];
  const keepStrings: string[] = [];

  for (let i = 0; i < all.length; i++) {
    const env = all[i];
    const original = raw[i];
    if (env.from === forPeer) continue; // never echo back
    if (env.to === undefined || env.to === null || env.to === forPeer) {
      delivered.push(env);
    } else {
      // Re-serialize so we can lpush the survivors back. This is a small
      // CPU cost but keeps the wire format consistent for V0.1.
      keepStrings.push(typeof original === "string" ? original : JSON.stringify(original));
    }
  }

  if (keepStrings.length === 0) {
    await kv.del(key);
  } else {
    await kv.del(key);
    await kv.lpush(key, ...keepStrings);
    await kv.expire(key, SIGNAL_TTL_SECONDS);
  }

  return delivered;
}
