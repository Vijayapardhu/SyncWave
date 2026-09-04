# SyncWave v0.1.1

Audio sharing fix release.

## What's fixed

- **Audio guest now reachable.** Added a "JOIN AUDIO" button to the room-code screen so guests can join an audio room directly. The previous build only routed guests into the video receiver, which never received an audio track from an audio-only host.
- **System audio (Android 10+) encoder format key fix.** `SystemAudioEncoder` was setting `MediaFormat.KEY_PCM_ENCODING` to `MediaRecorder.AudioSource.REMOTE_SUBMIX`, which is an audio-source id, not a PCM encoding. The Opus encoder configuration was being rejected on most devices. Replaced with the correct `KEY_CHANNEL_MASK` and removed the bogus PCM encoding key.

## Install

```
adb install -r app-release-v0.1.1.apk
```

Same signing key as v0.1.0 (debug keystore, sideload only).

## Verification

- Host: enter the audio feature, pick MIC or SYSTEM (Android 10+), tap START SHARING.
- Guest: from JOIN, enter the same 6-character code and tap "JOIN AUDIO &lt;code&gt;".
- For MIC mode, audio plays through the device speaker once the WebRTC connection is established.
- For SYSTEM mode, the host captures system playback and ships Opus frames over a data channel; the guest decodes them with `MediaCodec` and plays them via `AudioTrack`.
