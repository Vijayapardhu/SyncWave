# SyncWave — branding

Vector sources for the SyncWave identity. All files are SVG, solid-ink (black/white), and rendered to PNG via `render.js` (Puppeteer + Sharp).

## Files

### Sources
- `syncwave-icon.svg` — 512×512 monogram for the Android launcher.
- `syncwave-lockup.svg` — wordmark + double-wave symbol (light background).
- `syncwave-lockup-inverse.svg` — same on a black plate.
- `syncwave-readme-header.svg` — 1280×320 banner for the GitHub README.

### Rendered PNGs (`out/`)
- `icon-foreground-432.png` — adaptive icon foreground at 432px (transparent).
- `icon-foreground-1024.png` — adaptive icon foreground at 1024px (transparent).
- `app-icon-512.png` — full app icon at 512px on white.
- `lockup-1440.png` — lockup on white.
- `lockup-inverse-1440.png` — lockup on black.
- `readme-header-1280.png` — README banner.

## Symbol

Two offset sinusoidal waves meeting at a filled dot. The solid wave represents the host's outgoing stream; the outlined wave represents the guest's return channel. The dot at the meeting point is the room — the place where they sync.

## Wordmark

"SyncWave" drawn entirely with `<path>` strokes (no font dependency). The geometric construction makes it render identically on every platform, including devices that lack Inter / Helvetica fonts.

## Regenerating the PNGs

```bash
cd branding
npm install
node render.js
```

The script needs Chrome at the standard install path
(`C:\Program Files\Google\Chrome\Application\chrome.exe`) and `sharp` for resizing.

## Using the icon in Android

Place `app-icon-512.png` (or the larger variants) into
`android/app/src/main/res/mipmap-*/` at the appropriate density. For the
adaptive icon API 26+ foreground, use `icon-foreground-432.png` over a
solid background drawable.
