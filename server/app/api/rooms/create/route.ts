import { NextRequest, NextResponse } from "next/server";
import { createRoom, generateRoomCode } from "@/lib/rooms";
import { nanoid } from "nanoid";
import type { CreateRoomResponse } from "@/types/wire";

export const runtime = "edge";

export async function POST(req: NextRequest) {
  const body = (await req.json().catch(() => ({}))) as { deviceName?: string };
  const hostId = nanoid(10);
  const code = generateRoomCode();
  await createRoom(code, hostId);

  const res: CreateRoomResponse = {
    code,
    roomId: code,
    hostId,
    deviceName: body.deviceName ?? "Host",
    createdAt: Date.now(),
  };
  return NextResponse.json(res);
}
