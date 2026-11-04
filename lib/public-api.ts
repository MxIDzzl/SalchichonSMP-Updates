export type Overview = {
  players: number;
  officialDeaths: number;
  playtimeTicks: number;
  playerValue: number;
  seasonName: string;
  difficulty: string;
  seasonStartDate: string;
  generatedAt: string;
};

export type PlayerSummary = {
  uuid: string;
  username: string;
  lastDeathDate: string | null;
  totemUses: number;
  officialDeaths: number;
  playtimeTicks: number;
  playerValue: number;
};

export type Announcement = {
  id: number;
  title: string;
  body: string;
  category: string;
  publishedAt: string;
  expiresAt: string | null;
};

async function apiGet<T>(path: string): Promise<T | null> {
  const baseUrl = process.env.SALCHICHON_API_URL?.replace(/\/+$/, "");
  if (!baseUrl) return null;

  try {
    const response = await fetch(`${baseUrl}/api/v1${path}`, {
      cache: "no-store",
      signal: AbortSignal.timeout(2500),
      headers: { Accept: "application/json" },
    });
    if (!response.ok) return null;
    return (await response.json()) as T;
  } catch {
    return null;
  }
}

export function getOverview() {
  return apiGet<Overview>("/public/overview");
}

export function getPlayers(limit = 50, offset = 0, sort = "value") {
  const query = new URLSearchParams({ limit: String(limit), offset: String(offset), sort });
  return apiGet<PlayerSummary[]>(`/public/players?${query.toString()}`);
}

export function getPlayer(uuid: string) {
  return apiGet<PlayerSummary>(`/public/players/${encodeURIComponent(uuid)}`);
}

export function getAnnouncements(limit = 5) {
  return apiGet<Announcement[]>(`/public/announcements?limit=${limit}`);
}

export function formatPlaytime(ticks: number) {
  const totalHours = Math.floor(Math.max(0, ticks) / 72000);
  const days = Math.floor(totalHours / 24);
  const hours = totalHours % 24;
  if (days > 0) return `${days}d ${hours}h`;
  return `${totalHours}h`;
}

export function formatValue(value: number) {
  return new Intl.NumberFormat("es-MX", {
    notation: "compact",
    maximumFractionDigits: 1,
  }).format(value);
}
