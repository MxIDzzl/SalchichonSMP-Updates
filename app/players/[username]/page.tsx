import Link from "next/link";
import { formatPlaytime, formatValue, getPlayer } from "@/lib/public-api";

export const dynamic = "force-dynamic";

export default async function PlayerProfile({ params }: { params: Promise<{ username: string }> }) {
  const { username } = await params;
  const player = await getPlayer(username);
  return <main className="subpage">
    <Link href="/players">← Todos los jugadores</Link>
    {player ? <>
      <header className="profile-heading">
        {player.skinTextureHash ? <img className="skin-avatar" src={`https://mc-heads.net/avatar/${player.skinTextureHash}/64`} alt={`Skin de ${player.username}`} /> : <span className="skin-avatar-empty" aria-label="Skin no disponible" />}
        <div><small>PERFIL DE JUGADOR</small><h1>{player.username}</h1><p>Estadísticas oficiales de la temporada.</p></div>
      </header>
      <section className="profile-grid">
        <article><small>PLAYER VALUE</small><strong>{formatValue(player.playerValue)}</strong></article>
        <article><small>MUERTES OFICIALES</small><strong>{player.officialDeaths.toLocaleString("es-MX")}</strong></article>
        <article><small>TIEMPO JUGADO</small><strong>{formatPlaytime(player.playtimeTicks)}</strong></article>
        <article><small>TÓTEMS UTILIZADOS</small><strong>{player.totemUses.toLocaleString("es-MX")}</strong></article>
        <article><small>ESTADO DE VIDA</small><strong className={player.hasLife ? "life-available" : "life-unavailable"}>{player.hasLife ? "DISPONIBLE" : "SIN VIDA"}</strong></article>
      </section>
      <section className="empty profile-extra"><b>ÚLTIMA MUERTE</b><h2>{player.lastDeathDate ?? "Sin registro"}</h2><p>La skin se obtiene del perfil sincronizado por SkinsRestorer cuando el jugador entra al servidor.</p></section>
    </> : <><header><small>PERFIL DE JUGADOR</small><h1>Perfil no disponible.</h1><p>No se encontró el UUID solicitado o la API todavía no está conectada.</p></header><section className="empty"><b>DATOS NO DISPONIBLES</b><p>No se han cargado estadísticas ni skin para este perfil. No se mostrarán datos inventados.</p></section></>}
  </main>;
}
