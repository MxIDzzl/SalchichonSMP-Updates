import Link from "next/link";
import { formatPlaytime, formatValue, getPlayers } from "@/lib/public-api";

export const dynamic = "force-dynamic";
export const metadata = { title: "Jugadores · Escuadrón Salchichón" };

export default async function PlayersPage() {
  const players = await getPlayers(100, 0, "value");
  return <main className="subpage">
    <Link href="/">← Portal oficial</Link>
    <header><small>COMUNIDAD / DIRECTORIO</small><h1>Los jugadores.</h1><p>Perfiles, actividad y progreso de quienes forman parte de la temporada.</p></header>
    {players && players.length > 0 ? <section className="player-table">
      <div className="player-table-head"><span>JUGADOR</span><span>PLAYER VALUE</span><span>MUERTES</span><span>TIEMPO</span><span>ESTADO</span></div>
      {players.map(player => <Link className="player-row" href={`/players/${player.uuid}`} key={player.uuid}>
        <strong>{player.username}</strong><span>{formatValue(player.playerValue)}</span><span>{player.officialDeaths}</span><span>{formatPlaytime(player.playtimeTicks)}</span><span className={player.hasLife ? "life-available" : "life-unavailable"}>{player.hasLife ? "VIDA" : "SIN VIDA"}</span>
      </Link>)}
    </section> : <section className="empty"><b>DIRECTORIO EN PREPARACIÓN</b><h2>Los perfiles aparecerán aquí.</h2><p>Cuando la API esté conectada y haya datos disponibles, verás nombres, skins y estadísticas verificadas.</p></section>}
  </main>;
}
