# SyncWave — Architecture

## Goal

One Android device ("host") shares its screen + system audio with one or more
other devices ("guests") in real time. The receiver does not need the original
media file.

## Why WebRTC for the media, Vercel for the signaling

WebRTC is purpose-built for low-latency, real-time media between two peers. It
gives us encryption, jitter buffering, congestion control, and codec
negotiation for free. We let it carry the actual video/audio bytes peer-to-peer
and only use Vercel for the boring, low-bandwidth part: **coordinating the
peers** (creating rooms, exchanging SDP offers/answers and ICE candidates,
tracking who is connected).

This separation is intentional:

- **Vercel** — REST/edge functions for rooms + a tiny long-poll signaling
  buffer. Serverless is fine because signaling traffic is small and bursty.
- **WebRTC** — handles the actual stream between devices, never touching the
  server.

## High-level diagram

```
                    ┌───────────────────┐
                    │      VERCEL       │
                    │  Next.js (edge)   │
                    │  rooms + signaling│
                    └─────────┬─────────┘
                              │ REST / long-poll
                    ┌─────────┴─────────┐
                    │                   │
              ┌─────▼─────┐       ┌────▼──────┐
              │  PHONE A  │       │  PHONE B  │
              │   HOST    │       │  GUEST    │
              │ MediaProj │       │  Renderer │
              │    ↓      │       │     ↑     │
              │  WebRTC ──┼───────┼─  WebRTC  │
              └───────────┘       └───────────┘
```

## Repository layout

```
android/
  app/                  # Application module (MainActivity, navigation)
  core/
    network/            # HTTP client, DTOs
    signaling/          # Coroutine-driven signaling pump
    media/              # MediaProjection request + config helpers
    webrtc/             # PeerConnection facade, screen track factory
  feature/
    home/               # Home screen (Create/Join)
    host/               # Host flow (capture permission + share)
    receiver/           # Guest flow (render remote video)
    room/               # Room code entry
server/
  app/
    api/
      rooms/            # create / join / state
      signaling/        # signal push + long-poll
    lib/
      rooms.ts          # in-memory room store (V0.1)
      signaling.ts      # in-memory signal buffer
docs/
  architecture.md       # this file
```

## Versioning strategy

| Version | What lands                                          | Status |
|---------|------------------------------------------------------|--------|
| 0.1     | Screen → WebRTC → render. Vercel rooms + signaling. | **in progress** |
| 0.2     | Add system audio (AudioPlaybackCapture).            |  |
| 0.3     | QR code + Firebase/Ably signaling option.           |  |
| 0.4     | UX: status, mute, reconnect, quality indicator.     |  |
| 0.5     | Sync Play mode: host commands, both devices play.   |  |

### V0.1 checklist

- [x] Vercel deployed (config in `server/vercel.json`)
- [x] `POST /api/rooms/create` returns `{code, roomId, hostId, ...}`
- [x] `POST /api/rooms/join/{code}` returns `{success, code, roomId, guestId, hostId, ...}`
- [x] `POST/GET /api/signaling` push + long-poll SDP/ICE
- [x] `SignalingClient` interface with `VercelLongPollSignaling` + `FirebaseSignaling` stub
- [x] `PeerSession` drives offer/answer/ICE through the interface
- [x] `ScreenTrackFactory` wraps `MediaProjection` → `VideoTrack`
- [x] `HostViewModel` + `HostScreen`: create room → permission → offer
- [x] `ReceiverViewModel` + `ReceiverScreen`: `SurfaceViewRenderer` renders remote track

## V0.1 manual test plan

1. **Start the server**
   ```bash
   cd server
   npm install
   npm run dev
   ```
   Confirm:
   - `POST /api/rooms/create` → `{code, roomId, hostId, deviceName, createdAt}`
   - `POST /api/rooms/join/<code>` → `{success, code, roomId, guestId, hostId, deviceName}`
   - `POST /api/signaling` → `{ok, ts}`
   - `GET /api/signaling?roomId=…&peer=…` → `{signals: [...]}`

2. **Set the Android base URL** in `android/gradle.properties`:
   ```properties
   SYNCWAVE_BASE_URL=http://<your-lan-ip>:3000
   ```
   - Emulator: `10.0.2.2:3000` already wired for `debug`.
   - Physical device: use the host machine's LAN IP. `localhost` is the phone itself.

3. **Phone A — Host**
   - Launch SyncWave → **Create Room**. Note the 6-character code.
   - Tap **Start sharing** → accept the MediaProjection prompt.
   - Watch Logcat for tag `SyncWave/Peer`:
     ```
     iceConnectionState=CHECKING
     sdp created type=OFFER
     iceCandidate mid=0 idx=0
     iceConnectionState=CONNECTED
     ```

4. **Phone B — Guest**
   - Launch SyncWave → **Join Room** → enter the 6-character code.
   - Logcat should show:
     ```
     handle OFFER from=<hostId>
     sdp created type=ANSWER
     iceConnectionState=CONNECTED
     ```
   - The screen contents of Phone A should appear full-screen.

5. **Negative tests**
   - Wrong code: `POST /api/rooms/join/<bogus>` returns 404.
   - Stop sharing on A: receiver should see `Disconnected` and the renderer freezes / clears.
   - Put A in the background: stream may stall. This is the V0.2 problem.

## Known V0.1 limitations (to be fixed in V0.2)

| Limitation | Fix in V0.2 |
|---|---|
| Stream stops when the host Activity is backgrounded | `ShareForegroundService` with `foregroundServiceType="mediaProjection"` |
| No audio captured | `AudioPlaybackCapture` → WebRTC `AudioTrack` |
| Long-poll signaling (≤500ms latency) | Optional Firebase/Ably transport; same `SignalingClient` interface |
| TURN not configured — fails on symmetric NAT | Add `turn:…` ICE server in `PeerSession` |
| No screen-resolution scaling (uses full display) | Track-scaling on the host based on bandwidth |
| No QR code | Add `com.google.zxing` + a `Room QR` button on the host screen |

## V0.2 task list

1. Implement `ShareForegroundService`; lift `PeerSession` + `MediaProjection` into it.
2. Add `AudioPlaybackCapture` + `AudioSource` wiring in `ScreenTrackFactory` (or a new `AudioTrackFactory`).
3. Add TURN server (e.g. self-hosted coturn) for cellular networks.
4. Optional: replace `VercelLongPollSignaling` with `FirebaseSignaling`.

## Why we don't yet run an SFU

P2P WebRTC is bounded by the host's upload bandwidth. For V0.1–0.3 (1–5 guests)
P2P is dramatically simpler and cheaper. A Selective Forwarding Unit (mediasoup,
ion-sfu, LiveKit) becomes worth its complexity once we target 10+ concurrent
viewers or want server-side recording.

## Open risks

- **Audio capture restrictions.** Many apps (Netflix, DRM video) intentionally
  block `AudioPlaybackCapture`. There is no software workaround — only
  cooperation from the source app.
- **Background restrictions.** Hosting while the app is backgrounded is
  unreliable; V0.4 will require a foreground service.
- **WebRTC battery cost.** The encoder on the host will be the dominant
  battery draw. We'll measure in V0.2.
