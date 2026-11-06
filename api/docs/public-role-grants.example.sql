-- Run as the PostgreSQL database owner after the plugin schema and API V1-V5
-- migrations have been applied. Replace the password before execution.
CREATE ROLE salchichon_readonly LOGIN PASSWORD 'REPLACE_WITH_A_LONG_RANDOM_PASSWORD';

GRANT CONNECT ON DATABASE salchichon TO salchichon_readonly;
GRANT USAGE ON SCHEMA public TO salchichon_readonly;

-- These are the only tables read by PublicDataRepository.
GRANT SELECT ON TABLE
    player_data,
    player_stats,
    player_skins,
    public_clans,
    player_clan_memberships,
    public_announcements
TO salchichon_readonly;

-- Deliberately no INSERT, UPDATE, DELETE, sequence, vault, or trade permissions.
