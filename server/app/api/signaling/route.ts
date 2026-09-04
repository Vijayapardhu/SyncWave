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

  console.log(`[signaling] GET poll room=${roomId} peer=${peer}`);
  const signals = await drainSignals(roomId, peer);
  return NextResponse.json({ signals });
}
