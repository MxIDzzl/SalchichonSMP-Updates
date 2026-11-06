import Link from "next/link";
import { formatPlaytime, formatValue, getPlayers } from "@/lib/public-api";

export const dynamic = "force-dynamic";
export const metadata = { title: "Jugadores · Escuadrón Salchichón" };

const PAGE_SIZE = 25;
const MAX_PAGE = 40_001;

export default async function PlayersPage({
  searchParams,
}: {
  searchParams: Promise<{ page?: string }>;
}) {
  const params = await searchParams;
  const requestedPage = Number.parseInt(params.page ?? "1", 10);
  const currentPage = Number.isSafeInteger(requestedPage) && requestedPage > 0
    ? Math.min(requestedPage, MAX_PAGE)
    : 1;
  const players = await getPlayers(PAGE_SIZE, (currentPage - 1) * PAGE_SIZE, "value");
  const hasPlayers = Boolean(players && players.length > 0);
  const hasNextPage = Boolean(players && players.length === PAGE_SIZE);

  return <main className="subpage">
    <Link href="/">← Portal oficial</Link>
    <header><small>COMUNIDAD / DIRECTORIO</small><h1>Los jugadores.</h1><p>Perfiles, actividad y progreso de quienes forman parte de la temporada.</p></header>
    {hasPlayers ? <>
      <section className="player-table">
        <div className="player-table-head"><span>JUGADOR</span><span>PLAYER VALUE</span><span>MUERTES</span><span>TIEMPO</span><span>ESTADO</span></div>
        {players!.map(player => <Link className="player-row" href={`/players/${player.uuid}`} key={player.uuid}>
          <span className="player-name-cell">
            <strong>{player.username}</strong>
          </span><span>{formatValue(player.playerValue)}</span><span>{player.officialDeaths}</span><span>{formatPlaytime(player.playtimeTicks)}</span><span className={player.hasLife ? "life-available" : "life-unavailable"}>{player.hasLife ? "VIDA" : "SIN VIDA"}</span>
        </Link>)}
      </section>
      <nav className="directory-pagination" aria-label="Paginación del directorio">
        {currentPage > 1 ? <Link href={`/players?page=${currentPage - 1}`}>← Anterior</Link> : <span />}
        <span>Página {currentPage}</span>
        {hasNextPage && currentPage < MAX_PAGE ? <Link href={`/players?page=${currentPage + 1}`}>Siguiente →</Link> : <span />}
      </nav>
    </> : <section className="empty"><b>{players === null ? "DIRECTORIO NO DISPONIBLE" : "FIN DEL DIRECTORIO"}</b><h2>{players === null ? "No se pudo consultar la API." : "No hay más jugadores en esta página."}</h2><p>{players === null ? "Inténtalo de nuevo más tarde." : "Vuelve a la página anterior para consultar los perfiles disponibles."}</p>{currentPage > 1 ? <Link href={`/players?page=${currentPage - 1}`}>← Página anterior</Link> : null}</section>}
  </main>;
}
