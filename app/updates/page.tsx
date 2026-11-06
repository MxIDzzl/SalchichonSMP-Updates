import Link from "next/link";
import { formatDate, getAnnouncements } from "@/lib/public-api";

export const dynamic = "force-dynamic";
export const metadata = { title: "Novedades · Escuadrón Salchichón" };

export default async function UpdatesPage() {
  const announcements = await getAnnouncements(50);
  return <main className="subpage">
    <Link href="/">← Portal oficial</Link>
    <header><small>COMUNICADOS / HISTORIAL</small><h1>Las novedades.</h1><p>Anuncios y actualizaciones publicados oficialmente por el Escuadrón.</p></header>
    {announcements && announcements.length > 0 ? <section className="announcement-list">
      {announcements.map(item => <article className="announcement-card" key={item.id}>
        <div className="update-tag">{item.category}</div>
        <h2>{item.title}</h2>
        <p>{item.body}</p>
        <time dateTime={item.publishedAt}>{formatDate(item.publishedAt)}</time>
      </article>)}
    </section> : <section className="empty"><b>ARCHIVO EN PREPARACIÓN</b><h2>Aún no hay comunicados publicados.</h2><p>Los anuncios aparecerán aquí cuando el equipo los publique.</p></section>}
  </main>;
}
