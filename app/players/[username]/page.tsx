import Link from "next/link";
export const dynamicParams=true;
export default async function PlayerProfile({params}:{params:Promise<{username:string}>}){const {username}=await params;return <main className="subpage"><Link href="/players">← Todos los jugadores</Link><header><small>PERFIL DE JUGADOR</small><h1>{decodeURIComponent(username)}</h1><p>El perfil detallado estará disponible cuando la API esté conectada.</p></header><section className="empty"><b>DATOS NO DISPONIBLES</b><p>No se han cargado estadísticas ni skin para este perfil. No se mostrarán datos inventados.</p></section></main>}
