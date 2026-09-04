import { NextRequest, NextResponse } from "next/server";
import { addParticipant, getRoom } from "@/lib/rooms";
import { nanoid } from "nanoid";
import type { JoinRoomResponse } from "@/types/wire";

export const runtime = "edge";

export async function POST(req: NextRequest, { params }: { params: { code: string } }) {
  const code = params.code.toUpperCase();
  const body = (await req.json().catch(() => ({}))) as { deviceName?: string };
  const room = await getRoom(code);
  if (!room) return NextResponse.json({ error: "room_not_found" }, { status: 404 });
  if (!room.hostId) return NextResponse.json({ error: "host_gone" }, { status: 410 });

  const guestId = nanoid(10);
  await addParticipant(code, { id: guestId, role: "guest", joinedAt: Date.now() });

  const res: JoinRoomResponse = {
    success: true,
    code,
    roomId: code,
    guestId,
    hostId: room.hostId,
    deviceName: body.deviceName ?? "Guest",
  };
  return NextResponse.json(res);
}
