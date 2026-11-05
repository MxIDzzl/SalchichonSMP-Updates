-- Example least-privilege grants for the private Salchichón admin API role.
-- Create the role separately and replace salchichon_admin with the actual role.
-- Run as a database owner after plugin schema v1-v6 and API migrations V1-V4.
-- Do not grant this role membership in the plugin runtime or migration roles.

-- Public announcement administration.
GRANT SELECT, INSERT, UPDATE ON TABLE public_announcements TO salchichon_admin;
GRANT SELECT, INSERT ON TABLE public_admin_audit TO salchichon_admin;

-- Audited corrections are restricted to official deaths and playtime.
GRANT SELECT (uuid, username) ON TABLE player_data TO salchichon_admin;
GRANT INSERT ON TABLE player_stats TO salchichon_admin;
GRANT SELECT (player_uuid, official_deaths, playtime_ticks)
    ON TABLE player_stats TO salchichon_admin;
GRANT UPDATE (official_deaths, playtime_ticks, updated_at)
    ON TABLE player_stats TO salchichon_admin;
GRANT INSERT ON TABLE stat_events TO salchichon_admin;
GRANT SELECT (id) ON TABLE stat_events TO salchichon_admin;

-- Read-only trade recovery inspection. Column-level access intentionally
-- excludes item_payload, which contains serialized Bukkit ItemStacks.
GRANT SELECT (trade_id, player_a, player_b, status, revision, created_at, updated_at)
    ON TABLE trade_sessions TO salchichon_admin;
GRANT SELECT (
    operation_id, trade_id, owner_uuid, recipient_uuid, item_fingerprint,
    status, created_at, updated_at, delivered_at
) ON TABLE trade_item_operations TO salchichon_admin;

-- Deliberately absent:
--   DELETE on any table
--   SELECT on trade_item_operations.item_payload
--   access to trade_escrow_items, vault tables, Player Value adjustments,
--   trade_player_locks, or plugin runtime secrets/configuration
