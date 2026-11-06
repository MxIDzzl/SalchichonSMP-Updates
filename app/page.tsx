import Link from "next/link";
import { formatDate, formatPlaytime, formatValue, getAnnouncements, getOverview, getUpcomingUpdate } from "@/lib/public-api";

export const dynamic = "force-dynamic";

const features = [
  { number: "01", title: "Supervivencia", text: "Cada decisión cuenta. Las vidas, las muertes y el tiempo de juego forman parte de la historia de la temporada." },
  { number: "02", title: "Progreso real", text: "Estadísticas oficiales y registros persistentes. Sin rankings inventados ni datos de demostración." },
  { number: "03", title: "Una comunidad", text: "Un mundo compartido, retos colectivos y nuevas actualizaciones para el Escuadrón." },
];

const nav = [
  ["Temporada", "#season"],
  ["Jugadores", "/players"],
  ["Clanes", "/clans"],
  ["Rankings", "/rankings"],
  ["Sistema", "#system"],
  ["Novedades", "#updates"],
];

export default async function Home() {
  const [overview, announcements, upcomingUpdate] = await Promise.all([getOverview(), getAnnouncements(1), getUpcomingUpdate()]);
  const apiConfigured = Boolean(process.env.SALCHICHON_API_URL);
  const latestAnnouncement = announcements?.[0];

  return (
    <main>
      <div className="topline"><span className="status-dot" /> PORTAL OFICIAL <span className="topline-right">MINECRAFT · HARDCORE SMP</span></div>
      <header className="nav wrap">
        <a className="brand" href="#"><span className="brand-mark">S</span><span>ESCUADRÓN<span className="brand-muted"> SALCHICHÓN</span></span></a>
        <nav>{nav.map(([label, href]) => href.startsWith("/") ? <Link key={href} href={href}>{label}</Link> : <a key={href} href={href}>{label}</a>)}</nav>
        <a className="nav-cta" href="#updates">Explorar <span>↗</span></a>
      </header>

      <section className="hero wrap">
        <div className="hero-copy">
          <div className="eyebrow"><span /> TEMPORADA / ESTADO OFICIAL</div>
          <h1>NO ES SOLO<br /><span>SUPERVIVIR.</span></h1>
          <p className="hero-lead">Es dejar huella en un mundo donde cada día importa y cada jugador escribe su propia historia.</p>
          <div className="hero-actions">
            <a className="button button-primary" href="#season">Conocer la temporada <span>↗</span></a>
            <a className="text-link" href="#system">Cómo funciona <span>→</span></a>
          </div>
          <div className="hero-foot"><span className="mini-line" /> UN MUNDO. UNA COMUNIDAD. HISTORIAS REALES.</div>
        </div>
        <div className="hero-art" aria-label="Ilustración abstracta del mundo de Minecraft">
          <div className="orbit orbit-one" /><div className="orbit orbit-two" />
          <div className="cube cube-back" /><div className="cube cube-main"><div className="cube-top" /><div className="cube-front" /><div className="cube-side" /></div>
          <div className="art-label art-label-top">SMP / 01</div><div className="art-label art-label-bottom">SURVIVE TOGETHER</div>
          <div className="crosshair">+</div>
        </div>
      </section>

      <section className="season-band" id="season">
        <div className="wrap season-inner">
          <div><div className="section-kicker">01 / TEMPORADA</div><h2>{overview?.seasonName ?? "TEMPORADA SIN SINCRONIZAR"}</h2><p>{overview?.seasonDescription ?? "La temporada actual del Escuadrón Salchichón."}</p></div>
          <div className="season-meta"><div><span>DIFICULTAD</span><strong>{overview?.difficulty ?? "—"}</strong></div><div><span>INICIO</span><strong>{overview?.seasonStartDate ?? "—"}</strong></div><div><span>ESTADO</span><strong>—</strong></div></div>
        </div>
      </section>

      <section className="stats wrap" aria-label="Estadísticas del servidor">
        <div className="stats-heading"><div className="section-kicker">02 / EL SERVIDOR EN DATOS</div><span className="data-state"><i /> {overview ? "API CONECTADA" : apiConfigured ? "API SIN RESPUESTA" : "DATOS EN CONEXIÓN"}</span></div>
        <div className="stats-grid">
          <div className="stat"><span>JUGADORES</span><strong>{overview ? overview.players.toLocaleString("es-MX") : "—"}</strong><small>{overview ? "Datos oficiales" : "Próximamente"}</small></div>
          <div className="stat"><span>MUERTES OFICIALES</span><strong>{overview ? overview.officialDeaths.toLocaleString("es-MX") : "—"}</strong><small>{overview ? "Datos oficiales" : "Próximamente"}</small></div>
          <div className="stat"><span>HORAS JUGADAS</span><strong>{overview ? formatPlaytime(overview.playtimeTicks) : "—"}</strong><small>{overview ? "Tiempo acumulado" : "Próximamente"}</small></div>
          <div className="stat"><span>PLAYER VALUE</span><strong>{overview ? formatValue(overview.playerValue) : "—"}</strong><small>{overview ? "Prestigio acumulado" : "Próximamente"}</small></div>
        </div>
        <p className="data-note">{overview ? "Información consultada desde la API oficial del servidor." : "Las estadísticas aparecerán cuando la API esté disponible. No mostramos cifras ficticias."}</p>
      </section>

      <section className="system-section" id="system">
        <div className="wrap">
          <div className="section-kicker">03 / FILOSOFÍA DEL SERVIDOR</div>
          <div className="system-heading"><h2>EL MUNDO NO<br />SE DETIENE.</h2><p>Un sistema pensado para que el progreso, los riesgos y las decisiones tengan consecuencias.</p></div>
          <div className="feature-grid">{features.map((feature) => <article className="feature" key={feature.number}><span>{feature.number}</span><h3>{feature.title}</h3><p>{feature.text}</p><div className="feature-line" /></article>)}</div>
        </div>
      </section>

      <section className="updates-section wrap" id="updates">
        <div><div className="section-kicker">04 / TRANSMISIONES</div><h2>LO QUE SIGUE.</h2><p>Próximas actualizaciones, cambios de temporada y anuncios oficiales.</p></div>
        <div className="updates-cards"><article className="update-card"><div className="update-tag">{latestAnnouncement?.category ?? "ÚLTIMO ANUNCIO"}</div><h3>{latestAnnouncement?.title ?? "Preparando la siguiente transmisión."}</h3><p>{latestAnnouncement?.body ?? "Las notas y anuncios oficiales aparecerán aquí cuando se publiquen."}</p><span className="update-arrow">↗</span></article>{upcomingUpdate && <article className="update-card upcoming-card"><div className="update-tag">{upcomingUpdate.teaser ? "ADELANTO · " : "PRÓXIMA ACTUALIZACIÓN · "}{upcomingUpdate.category}</div><h3>{upcomingUpdate.title}</h3><p>{upcomingUpdate.body}</p><small>PROGRAMADA PARA {formatDate(upcomingUpdate.scheduledAt).toUpperCase()}</small><span className="update-arrow">↗</span></article>}</div><Link className="text-link updates-archive-link" href="/updates">Ver todas las novedades →</Link>
      </section>

      <footer className="footer"><div className="wrap footer-inner"><a className="brand" href="#"><span className="brand-mark">S</span><span>ESCUADRÓN<span className="brand-muted"> SALCHICHÓN</span></span></a><span>HECHO PARA SOBREVIVIR JUNTOS.</span><span>© 2026 · PORTAL OFICIAL</span></div></footer>
    </main>
  );
}
