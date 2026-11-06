export type Overview = {
  players: number;
  officialDeaths: number;
  playtimeTicks: number;
  playerValue: string;
  seasonName: string;
  seasonDescription: string;
  difficulty: string;
  seasonStartDate: string;
  generatedAt: string;
};

export type PlayerSummary = {
  uuid: string;
  username: string;
  skinTextureHash: string | null;
  lastDeathDate: string | null;
  hasLife: boolean;
  totemUses: number;
  officialDeaths: number;
  playtimeTicks: number;
  playerValue: string;
  clanUuid: string | null;
  clanName: string | null;
  clanTag: string | null;
  clanLevel: number | null;
  clanRank: string | null;
};

export type ClanSummary = {
  uuid: string;
  name: string;
  tag: string;
  description: string;
  leaderUuid: string;
  createdAt: string;
  memberCount: number;
  maxMembers: number;
  points: number;
  level: number;
  kills: number;
  deaths: number;
  kdr: number;
};

export type ClanMember = {
  playerUuid: string;
  username: string;
  rankId: string;
  joinedAt: string;
  kills: number;
  deaths: number;
  kdr: number;
};

export type ClanDetail = {
  clan: ClanSummary;
  members: ClanMember[];
};

export type Announcement = {
  id: number;
  title: string;
  body: string;
  category: string;
  publishedAt: string;
  expiresAt: string | null;
};


export type UpcomingUpdate = {
  id: number;
  title: string;
  body: string;
  category: string;
  scheduledAt: string;
  teaser: boolean;
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

export function getClans(limit = 50, offset = 0, sort = "points") {
  const query = new URLSearchParams({ limit: String(limit), offset: String(offset), sort });
  return apiGet<ClanSummary[]>(`/public/clans?${query.toString()}`);
}

export function getClan(uuid: string) {
  return apiGet<ClanDetail>(`/public/clans/${encodeURIComponent(uuid)}`);
}

export function getAnnouncements(limit = 5) {
  return apiGet<Announcement[]>(`/public/announcements?limit=${limit}`);
}

export function getUpcomingUpdate() {
  return apiGet<UpcomingUpdate>(`/public/upcoming-update`);
}

export function formatDate(value: string) {
  return new Intl.DateTimeFormat("es-MX", { dateStyle: "long", timeZone: "America/Mexico_City" }).format(new Date(value));
}

export function formatPlaytime(ticks: number) {
  const totalHours = Math.floor(Math.max(0, ticks) / 72000);
  const days = Math.floor(totalHours / 24);
  const hours = totalHours % 24;
  if (days > 0) return `${days}d ${hours}h`;
  return `${totalHours}h`;
}

export function formatValue(value: string) {
  const amount = BigInt(value);
  const units = [
    { threshold: 1_000_000_000n, suffix: "b" },
    { threshold: 1_000_000n, suffix: "m" },
    { threshold: 1_000n, suffix: "k" },
  ];
  for (const unit of units) {
    if (amount >= unit.threshold) {
      const whole = amount / unit.threshold;
      const decimal = (amount % unit.threshold) * 10n / unit.threshold;
      return `${whole}${decimal > 0n ? `.${decimal}` : ""}${unit.suffix}`;
    }
  }
  return amount.toString();
}
