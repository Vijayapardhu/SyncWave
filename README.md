# SyncWave

![SyncWave banner](branding/out/readme-header-1280.png)

Watch and listen together, in real time. One Android device becomes the host and shares its screen + system audio over WebRTC. Other devices join via a short room code (QR or typed) and receive the stream peer-to-peer.

## Repository layout

```
syncwave/
├── android/   # Kotlin + Jetpack Compose app
├── server/    # Next.js app deployed on Vercel (signaling + rooms)
└── docs/      # architecture & design notes
```

## V0.1 status

Code-complete. The pipeline is wired end-to-end:

1. Phone A → `POST /api/rooms/create` → 6-character room code.
2. Phone A taps **Start sharing** → `MediaProjection` → `VideoTrack` → `PeerConnection` offer.
3. Phone B → `POST /api/rooms/join/<code>` → starts polling signaling.
4. Both phones exchange SDP + ICE through the Vercel signaling endpoint.
5. WebRTC connects P2P. Media never touches the server.
6. Phone B renders the remote track via `SurfaceViewRenderer`.

See `docs/architecture.md` for the full V0.1 manual test plan and the V0.2 task list.

## Quick start

### 1. Run the signaling server

```bash
cd server
npm install
npm run dev
```

Test it:

```bash
curl -X POST http://localhost:3000/api/rooms/create \
  -H "Content-Type: application/json" \
  -d '{"deviceName":"test"}'
# → {"code":"ABC123","roomId":"ABC123","hostId":"...","deviceName":"test","createdAt":...}
```

For two-device testing, run the server on a machine reachable from both phones. Find its LAN IP (`ipconfig` / `ifconfig`) and use it from the Android side.

### 2. Build the Android app

The wrapper is **not committed** (it includes a binary `gradle-wrapper.jar`). Generate it once:

```powershell
# Windows
cd android
.\tools\bootstrap-wrapper.ps1
```

```bash
# macOS / Linux
cd android
./tools/bootstrap-wrapper.sh
```

Or just open the `android/` folder in Android Studio and let it sync — that generates the wrapper automatically.

Set the server URL in `android/gradle.properties`:

```properties
# Emulator: already correct as 10.0.2.2:3000
# Physical device: use the host machine's LAN IP
SYNCWAVE_BASE_URL=http://192.168.1.100:3000
```

Create your `local.properties` (Android Studio does this on first sync, or copy the template):

```powershell
Copy-Item android\local.properties.template android\local.properties
# Edit android\local.properties and set sdk.dir
```

Build & install:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```

### 3. Two-device acceptance test

1. Phone A → **Create Room** → note the 6-character code.
2. Phone A → **Start sharing** → accept the MediaProjection prompt.
3. Phone B → **Join Room** → enter the code.
4. Watch Logcat for tag `SyncWave/Peer` on both devices. You should see `iceConnectionState=CONNECTED` and Phone B should display Phone A's screen.

If anything fails, the Logcat trace tells you whether the failure is in signaling, SDP, ICE, MediaProjection, or the renderer.

## Build order (V0.1 → V0.5)

1. **V0.1** — Screen → WebRTC → render. Vercel rooms + signaling. ← *we are here*
2. **V0.2** — Add system audio (`AudioPlaybackCapture`) and a real `ShareForegroundService`.
3. **V0.3** — QR code + Firebase/Ably signaling option.
4. **V0.4** — UX: status, mute, reconnect, quality indicator.
5. **V0.5** — Sync Play mode: host commands, both devices play.
