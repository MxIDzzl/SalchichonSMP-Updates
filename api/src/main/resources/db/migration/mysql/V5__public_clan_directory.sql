-- Read-only projection of the authoritative zMClans snapshot.
CREATE TABLE IF NOT EXISTS public_clans (
    clan_uuid CHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    tag VARCHAR(32) NOT NULL,
    description TEXT NOT NULL,
    leader_uuid CHAR(36) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    member_count INT NOT NULL,
    max_members INT NOT NULL,
    points BIGINT NOT NULL,
    level INT NOT NULL,
    kills BIGINT NOT NULL,
    deaths BIGINT NOT NULL,
    kdr DOUBLE NOT NULL,
    synced_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    INDEX idx_public_clans_points (points DESC, clan_uuid),
    CONSTRAINT chk_public_clans_member_count CHECK (member_count >= 0),
    CONSTRAINT chk_public_clans_max_members CHECK (max_members >= 0),
    CONSTRAINT chk_public_clans_points CHECK (points >= 0),
    CONSTRAINT chk_public_clans_level CHECK (level >= 0),
    CONSTRAINT chk_public_clans_kills CHECK (kills >= 0),
    CONSTRAINT chk_public_clans_deaths CHECK (deaths >= 0),
    CONSTRAINT chk_public_clans_kdr CHECK (kdr >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS player_clan_memberships (
    player_uuid CHAR(36) NOT NULL PRIMARY KEY,
    clan_uuid CHAR(36) NOT NULL,
    rank_id VARCHAR(32) NOT NULL,
    joined_at DATETIME(6) NOT NULL,
    kills BIGINT NOT NULL,
    deaths BIGINT NOT NULL,
    kdr DOUBLE NOT NULL,
    synced_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    INDEX idx_player_clan_memberships_clan (clan_uuid, rank_id, player_uuid),
    CONSTRAINT fk_player_clan_memberships_clan
        FOREIGN KEY (clan_uuid) REFERENCES public_clans(clan_uuid) ON DELETE CASCADE,
    CONSTRAINT chk_player_clan_memberships_kills CHECK (kills >= 0),
    CONSTRAINT chk_player_clan_memberships_deaths CHECK (deaths >= 0),
    CONSTRAINT chk_player_clan_memberships_kdr CHECK (kdr >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
