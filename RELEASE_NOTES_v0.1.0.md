# SyncWave v0.1.0

First public build of the SyncWave Android app.

## What's in this build

- **Monochrome black theme** — strict two-tone (ink / paper) palette across all screens. No color accents, no gradients.
- **No emoji** — all status, action, and label text uses uppercase labels.
- **Screen sharing** over WebRTC peer-to-peer (host / guest).
- **Audio sharing** (microphone + Android 10+ system audio) over a data channel.
- **Room code + QR** join flow.
- **MediaProjection foreground service** is now started before `getMediaProjection()`, fixing the "MediaProjection required a foreground service" error on Android 14+.

## Install

Sideload the signed APK on a device with API 26+:

```
adb install -r app-release.apk
```

The APK is signed with a debug keystore for sideloading. A production release keystore is not configured in this build.

## Known limitations

- The host's `PeerSession` lifetime is not yet moved into the foreground service. Stopping the share from the notification shade is not supported in this build; stop it from the in-app controls.
- The release APK is signed with the local debug keystore. Treat it as a sideload test build, not a Play Store artifact.
