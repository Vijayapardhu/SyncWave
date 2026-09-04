import { NextRequest, NextResponse } from "next/server";
import { getRoom } from "@/lib/rooms";

export const runtime = "edge";

export async function GET(_req: NextRequest, { params }: { params: { code: string } }) {
  const code = params.code.toUpperCase();
  const room = await getRoom(code);
  if (!room) return NextResponse.json({ error: "room_not_found" }, { status: 404 });
  return NextResponse.json({
    code: room.id,
    roomId: room.id,
    hostId: room.hostId,
    participants: room.participants,
  });
}
