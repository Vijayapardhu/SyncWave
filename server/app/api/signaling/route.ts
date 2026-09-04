import { NextRequest, NextResponse } from "next/server";
import { getRoom } from "@/lib/rooms";
import { bufferSignal, drainSignals } from "@/lib/signaling";
import type { SignalEnvelope, SignalType } from "@/types/wire";

export const runtime = "edge";

function isSignalType(v: unknown): v is SignalType {
  return v === "OFFER" || v === "ANSWER" || v === "ICE" || v === "PRESENCE";
}

export async function POST(req: NextRequest) {
  const body = (await req.json().catch(() => null)) as Partial<SignalEnvelope> | null;
  if (!body || !isSignalType(body.type) || !body.from || !body.roomId) {
    return NextResponse.json({ error: "bad_request" }, { status: 400 });
  }
  const room = await getRoom(body.roomId);
  if (!room) return NextResponse.json({ error: "room_not_found" }, { status: 404 });

  const env: SignalEnvelope = {
    type: body.type,
    from: body.from,
    to: body.to,
    roomId: body.roomId,
    payload: body.payload,
    ts: Date.now(),
  };
  console.log(`[signaling] POST room=${body.roomId} from=${body.from} type=${body.type} to=${body.to}`);
  await bufferSignal(body.roomId, env);
  return NextResponse.json({ ok: true, ts: env.ts });
}

export async function GET(req: NextRequest) {
  const url = new URL(req.url);
  const roomId = url.searchParams.get("roomId");
  const peer = url.searchParams.get("peer");
  if (!roomId || !peer) return NextResponse.json({ error: "bad_request" }, { status: 400 });

  const room = await getRoom(roomId);
  if (!room) return NextResponse.json({ error: "room_not_found" }, { status: 404 });

  const signals = await drainSignals(roomId, peer);
  return NextResponse.json({ signals });
}

export async function DEBUG(req: NextRequest) {
  const url = new URL(req.url);
  const roomId = url.searchParams.get("roomId");
  if (!roomId) return NextResponse.json({ error: "bad_request" }, { status: 400 });

  const room = await getRoom(roomId);
  if (!room) return NextResponse.json({ error: "room_not_found" }, { status: 404 });

  const key = `signals:${roomId}`;
  const raw = await kv.lrange(key, 0, -1);
  const parsed = raw.map((s: unknown) => {
    try {
      const str = typeof s === "string" ? s : JSON.stringify(s);
      return JSON.parse(str);
    } catch {
      return s;
    }
  });
  return NextResponse.json({ key, rawCount: raw.length, signals: parsed });
}
