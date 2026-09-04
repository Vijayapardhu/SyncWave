import Link from "next/link";

export default function Page() {
  return (
    <main style={{ fontFamily: "ui-sans-serif, system-ui", padding: 48, maxWidth: 720 }}>
      <h1 style={{ fontSize: 40, marginBottom: 8 }}>SyncWave</h1>
      <p style={{ color: "#666" }}>Watch and listen together, in real time.</p>
      <ul style={{ marginTop: 32, lineHeight: 2 }}>
        <li>
          <code>POST /api/rooms/create</code> — host creates a room.
        </li>
        <li>
          <code>POST /api/rooms/join/[code]</code> — guest joins a room.
        </li>
        <li>
          <code>GET /api/rooms/[code]</code> — fetch room state.
        </li>
        <li>
          <code>POST /api/signaling</code> — push SDP / ICE / presence.
        </li>
        <li>
          <code>GET /api/signaling?roomId=…&peer=…</code> — long-poll pending signals.
        </li>
      </ul>
      <p style={{ marginTop: 32, color: "#888" }}>
        See <Link href="/docs/architecture.md">docs/architecture.md</Link>.
      </p>
    </main>
  );
}
