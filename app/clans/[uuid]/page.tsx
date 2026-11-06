import Link from "next/link";
import { getClan } from "@/lib/public-api";

export const dynamic = "force-dynamic";

export default async function ClanProfile({ params }: { params: Promise<{ uuid: string }> }) {
  const { uuid } = await params;
  const detail = await getClan(uuid);
  if (!detail) return <main className="subpage">
    <Link href="/clans">← Todos los clanes</Link>
    <header><small>PERFIL DE CLAN</small><h1>Clan no disponible.</h1><p>No se encontró el clan o la API todavía no está conectada.</p></header>
    <section className="empty"><b>DATOS NO DISPONIBLES</b><p>El directorio muestra únicamente los datos sincronizados desde zMClans.</p></section>
  </main>;

  const clan = detail.clan;
  return <main className="subpage">
    <Link href="/clans">← Todos los clanes</Link>
    <header className="clan-profile-heading">
      <span className="clan-emblem">{clan.tag}</span>
      <div><small>PERFIL DE CLAN / NIVEL {clan.level}</small><h1>{clan.name}</h1><p>{clan.description || "Sin descripción pública."}</p></div>
    </header>
    <section className="clan-profile-stats">
      <article><small>PUNTOS</small><strong>{clan.points.toLocaleString("es-MX")}</strong></article>
      <article><small>MIEMBROS</small><strong>{clan.memberCount}/{clan.maxMembers}</strong></article>
      <article><small>ASESINATOS</small><strong>{clan.kills.toLocaleString("es-MX")}</strong></article>
      <article><small>KDR</small><strong>{clan.kdr.toFixed(2)}</strong></article>
    </section>
    <section className="clan-members-section">
      <div className="section-kicker">MIEMBROS / {detail.members.length}</div>
      <div className="clan-members">
        {detail.members.map(member => <Link href={`/players/${member.playerUuid}`} className="clan-member-row" key={member.playerUuid}>
          <span><strong>{member.username}</strong><small>{member.rankId.toUpperCase()}</small></span>
          <span><small>KILLS</small><b>{member.kills}</b></span>
          <span><small>MUERTES</small><b>{member.deaths}</b></span>
          <span><small>KDR</small><b>{member.kdr.toFixed(2)}</b></span>
        </Link>)}
      </div>
    </section>
  </main>;
}
