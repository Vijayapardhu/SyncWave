export const metadata = {
  title: "SyncWave",
  description: "Watch and listen together, in real time.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body style={{ fontFamily: "ui-sans-serif, system-ui, -apple-system, sans-serif" }}>
        {children}
      </body>
    </html>
  );
}
