-- Least-privilege grants for the private admin API datasource.
-- Run as a MySQL database administrator after plugin schema and API V1-V5.
-- Restrict '%' to the API server's source IP where possible.
CREATE USER 'salchichon_admin'@'%' IDENTIFIED BY 'REPLACE_WITH_A_LONG_RANDOM_PASSWORD';

-- Announcement administration: read all fields, write only mutable columns.
GRANT SELECT ON salchichon.public_announcements TO 'salchichon_admin'@'%';
GRANT INSERT (title, body, category, is_published, published_at, expires_at,
              scheduled_at, is_teaser, created_by)
    ON salchichon.public_announcements TO 'salchichon_admin'@'%';
GRANT UPDATE (title, body, category, scheduled_at, expires_at, is_teaser,
              is_published, published_at)
    ON salchichon.public_announcements TO 'salchichon_admin'@'%';

-- Admin audit is append-only from the API; details is private admin-only data.
GRANT SELECT ON salchichon.public_admin_audit TO 'salchichon_admin'@'%';
GRANT INSERT (actor, action, entity_type, entity_id, reason, details)
    ON salchichon.public_admin_audit TO 'salchichon_admin'@'%';

-- Audited corrections are limited to official deaths and playtime.
GRANT SELECT (uuid, username) ON salchichon.player_data TO 'salchichon_admin'@'%';
GRANT SELECT (player_uuid, official_deaths, playtime_ticks)
    ON salchichon.player_stats TO 'salchichon_admin'@'%';
GRANT INSERT (player_uuid, official_deaths, playtime_ticks, updated_at)
    ON salchichon.player_stats TO 'salchichon_admin'@'%';
GRANT UPDATE (official_deaths, playtime_ticks, updated_at)
    ON salchichon.player_stats TO 'salchichon_admin'@'%';
GRANT INSERT (player_uuid, stat_key, previous_value, new_value, actor, reason)
    ON salchichon.stat_events TO 'salchichon_admin'@'%';

-- Read-only recovery inspection. Never grant item_payload or serialized escrow access.
GRANT SELECT (trade_id, player_a, player_b, status, revision, created_at, updated_at)
    ON salchichon.trade_sessions TO 'salchichon_admin'@'%';
GRANT SELECT (uuid, username) ON salchichon.player_data TO 'salchichon_admin'@'%';
GRANT SELECT (operation_id, trade_id, owner_uuid, recipient_uuid, item_fingerprint,
              status, created_at, updated_at, delivered_at)
    ON salchichon.trade_item_operations TO 'salchichon_admin'@'%';

-- Deliberately absent:
--   DELETE on any table
--   SELECT on trade_item_operations.item_payload
--   access to trade_escrow_items, vault tables, Player Value adjustments,
--   trade_player_locks, or plugin runtime secrets/configuration
