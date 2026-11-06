import Link from "next/link";
import { getClans } from "@/lib/public-api";

export const dynamic = "force-dynamic";
export const metadata = { title: "Clanes · Escuadrón Salchichón" };

export default async function ClansPage() {
  const clans = await getClans(100, 0, "points");
  return <main className="subpage">
    <Link href="/">← Portal oficial</Link>
    <header><small>COMUNIDAD / CLANES</small><h1>Una comunidad.<br />Varios estandartes.</h1><p>Directorio y estadísticas sincronizados desde zMClans. Los clanes se administran dentro del servidor.</p></header>
    {clans && clans.length > 0 ? <section className="clan-grid">
      {clans.map((clan, index) => <Link className="clan-card" href={`/clans/${clan.uuid}`} key={clan.uuid}>
        <div className="clan-card-top"><span>#{String(index + 1).padStart(2, "0")}</span><b>{clan.tag}</b></div>
        <h2>{clan.name}</h2>
        <p>{clan.description || "Sin descripción pública."}</p>
        <div className="clan-card-stats">
          <span><small>NIVEL</small><strong>{clan.level}</strong></span>
          <span><small>PUNTOS</small><strong>{clan.points.toLocaleString("es-MX")}</strong></span>
          <span><small>MIEMBROS</small><strong>{clan.memberCount}/{clan.maxMembers}</strong></span>
        </div>
      </Link>)}
    </section> : <section className="empty"><b>{clans === null ? "CLANES NO DISPONIBLES" : "SIN CLANES ACTIVOS"}</b><h2>{clans === null ? "No se pudo consultar el directorio." : "Aún no hay clanes sincronizados."}</h2><p>El directorio se actualiza desde el plugin oficial de clanes del servidor. No se muestran datos de demostración.</p></section>}
  </main>;
}
