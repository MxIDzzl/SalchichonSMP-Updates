import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Escuadrón Salchichón SMP",
  description: "Portal oficial del Escuadrón Salchichón: temporada, estadísticas, jugadores y novedades.",
  applicationName: "Escuadrón Salchichón",
  robots: { index: true, follow: true },
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="es">
      <body>{children}</body>
    </html>
  );
}
