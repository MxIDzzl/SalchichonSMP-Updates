export const metadata = { title: "Administración · Escuadrón Salchichón" };

export default function AdminPage() {
  const configured = Boolean(process.env.NEXT_PUBLIC_SALCHICHON_API_URL);

  return (
    <main className="admin-page">
      <a className="back-link" href="/">← Volver al portal</a>
      <div className="admin-panel">
        <div className="section-kicker">ÁREA PRIVADA / ADMINISTRACIÓN</div>
        <div className="admin-symbol">S</div>
        <h1>Centro de control.</h1>
        <p>La administración del servidor se conectará al servicio API independiente. Las acciones administrativas requerirán autenticación, permisos y registro de auditoría.</p>
        <div className={configured ? "connection-state configured" : "connection-state"}>
          <span />
          {configured ? "URL de API configurada; autenticación pendiente de integración." : "API no conectada"}
        </div>
        <div className="admin-warning">El panel todavía no permite modificar datos. No se simula una sesión ni se guardan credenciales en el navegador.</div>
        <a className="text-link" href="/">Regresar al sitio público →</a>
      </div>
    </main>
  );
}
