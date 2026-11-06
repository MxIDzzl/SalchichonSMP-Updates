-- Optional least-privilege runtime account for SalchichonHardcore.
-- Run as a MySQL database administrator after creating the empty database.
-- Restrict the host part to the Paper server's source IP where possible.
CREATE USER 'salchichon_plugin'@'PAPER_SERVER_IP' IDENTIFIED BY 'REPLACE_WITH_A_LONG_RANDOM_PASSWORD';

-- The plugin's idempotent bootstrap creates missing tables on startup.
-- It does not need DROP or ALTER privileges for the current schema.
GRANT CREATE ON salchichon.* TO 'salchichon_plugin'@'PAPER_SERVER_IP';

-- Runtime persistence, Player Value journal/recovery, clan snapshot cache,
-- skins, and legacy trade-compatibility records.
GRANT SELECT, INSERT, UPDATE, DELETE ON salchichon.schema_version TO 'salchichon_plugin'@'PAPER_SERVER_IP';
GRANT SELECT, INSERT, UPDATE, DELETE ON salchichon.player_data TO 'salchichon_plugin'@'PAPER_SERVER_IP';
GRANT SELECT, INSERT, UPDATE, DELETE ON salchichon.admin_audit TO 'salchichon_plugin'@'PAPER_SERVER_IP';
GRANT SELECT, INSERT, UPDATE, DELETE ON salchichon.player_stats TO 'salchichon_plugin'@'PAPER_SERVER_IP';
GRANT SELECT, INSERT, UPDATE, DELETE ON salchichon.stat_events TO 'salchichon_plugin'@'PAPER_SERVER_IP';
GRANT SELECT, INSERT, UPDATE, DELETE ON salchichon.player_value_items TO 'salchichon_plugin'@'PAPER_SERVER_IP';
GRANT SELECT, INSERT, UPDATE, DELETE ON salchichon.vault_operations TO 'salchichon_plugin'@'PAPER_SERVER_IP';
GRANT SELECT, INSERT, UPDATE, DELETE ON salchichon.vault_claims TO 'salchichon_plugin'@'PAPER_SERVER_IP';
GRANT SELECT, INSERT, UPDATE, DELETE ON salchichon.vault_cooldowns TO 'salchichon_plugin'@'PAPER_SERVER_IP';
GRANT SELECT, INSERT, UPDATE, DELETE ON salchichon.player_value_adjustments TO 'salchichon_plugin'@'PAPER_SERVER_IP';
GRANT SELECT, INSERT, UPDATE, DELETE ON salchichon.player_skins TO 'salchichon_plugin'@'PAPER_SERVER_IP';
GRANT SELECT, INSERT, UPDATE, DELETE ON salchichon.public_clans TO 'salchichon_plugin'@'PAPER_SERVER_IP';
GRANT SELECT, INSERT, UPDATE, DELETE ON salchichon.player_clan_memberships TO 'salchichon_plugin'@'PAPER_SERVER_IP';

-- Retained for compatibility with the existing trade persistence contract.
-- Actual player-to-player trades remain owned by the external trade plugin.
GRANT SELECT, INSERT, UPDATE, DELETE ON salchichon.trade_sessions TO 'salchichon_plugin'@'PAPER_SERVER_IP';
GRANT SELECT, INSERT, UPDATE, DELETE ON salchichon.trade_escrow_items TO 'salchichon_plugin'@'PAPER_SERVER_IP';
GRANT SELECT, INSERT, UPDATE, DELETE ON salchichon.trade_item_operations TO 'salchichon_plugin'@'PAPER_SERVER_IP';
GRANT SELECT, INSERT, UPDATE, DELETE ON salchichon.trade_player_locks TO 'salchichon_plugin'@'PAPER_SERVER_IP';
GRANT SELECT, INSERT, UPDATE, DELETE ON salchichon.operation_receipts TO 'salchichon_plugin'@'PAPER_SERVER_IP';

-- Deliberately no DROP, ALTER, GRANT OPTION, or access to API admin credentials.
