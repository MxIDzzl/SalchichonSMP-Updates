import Link from "next/link";
import { formatPlaytime, formatValue, getPlayers, type PlayerSummary } from "@/lib/public-api";

export const dynamic = "force-dynamic";
export const metadata = { title: "Rankings · Escuadrón Salchichón" };

const boards = [
  { key: "value", title: "Player Value", format: (p: PlayerSummary) => formatValue(p.playerValue) },
  { key: "playtime", title: "Tiempo jugado", format: (p: PlayerSummary) => formatPlaytime(p.playtimeTicks) },
  { key: "deaths", title: "Muertes oficiales", format: (p: PlayerSummary) => p.officialDeaths.toLocaleString("es-MX") },
  { key: "totems", title: "Tótems utilizados", format: (p: PlayerSummary) => p.totemUses.toLocaleString("es-MX") },
] as const;

export default async function RankingsPage() {
  const results = await Promise.all(boards.map(board => getPlayers(10, 0, board.key)));
  return <main className="subpage">
    <Link href="/">← Portal oficial</Link>
    <header><small>COMPETENCIA / CLASIFICACIONES</small><h1>Los rankings.</h1><p>El progreso de la temporada, medido con estadísticas oficiales.</p></header>
    <section className="ranking-list">
      {boards.map((board, index) => <article key={board.key}>
        <small>0{index + 1}</small><h2>{board.title}</h2>
        {results[index] && results[index]!.length > 0 ? <ol>{results[index]!.map(player => <li key={player.uuid}><Link href={`/players/${player.uuid}`}>{player.username}</Link><b>{board.format(player)}</b></li>)}</ol> : <span>Esperando datos de la API</span>}
      </article>)}
    </section>
  </main>;
}
