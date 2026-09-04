# Server — Vercel deployment

The server is a Next.js 14 app (App Router) deployed on Vercel's edge runtime. It only carries SDP/ICE signaling; the actual media flows peer-to-peer over WebRTC.

## One-time setup

```bash
cd server
npm install
```

## Local development

```bash
npm run dev
# → http://localhost:3000
```

Test the endpoints:

```bash
# Create a room
curl -X POST http://localhost:3000/api/rooms/create \
  -H "Content-Type: application/json" \
  -d '{"deviceName":"test"}'

# Join it (use the code from the previous response)
curl -X POST http://localhost:3000/api/rooms/join/ABC123 \
  -H "Content-Type: application/json" \
  -d '{"deviceName":"guest"}'
```

## Deploy to Vercel

```bash
npm install -g vercel
vercel login
vercel              # preview deploy
vercel --prod       # production deploy
```

You'll get a URL like `https://syncwave-xxxxx.vercel.app`.

## Point the Android app at the deployed server

In `android/gradle.properties`:

```properties
SYNCWAVE_BASE_URL=https://syncwave-xxxxx.vercel.app
```

The default in `app/build.gradle.kts` already uses this URL for `release` builds. For local development against a LAN dev server, the `debug` build type overrides it to `http://10.0.2.2:3000` (emulator localhost).

## Endpoints

| Method | Path                       | Purpose                          |
|--------|----------------------------|----------------------------------|
| POST   | `/api/rooms/create`        | Host creates a room.             |
| POST   | `/api/rooms/join/[code]`   | Guest joins by room code.        |
| GET    | `/api/rooms/[code]`        | Inspect room state.              |
| POST   | `/api/signaling`           | Push SDP/ICE/PRESENCE envelope.  |
| GET    | `/api/signaling?roomId=&peer=` | Long-poll for incoming envelopes. |

Wire contract is defined in `types/wire.ts` and mirrored in `android/core/network/dto/Dto.kt`.

## State storage

The in-memory `Map` in `lib/rooms.ts` is fine for V0.1. Each Vercel serverless invocation can land on a different instance, so for production scale you would back this with Vercel KV or Upstash Redis. The V0.1 path stays on in-memory because the long-poll client retries every 500 ms and is tolerant to short gaps.
