-- Run as a MySQL database administrator after the plugin MySQL schema
-- and API Flyway migrations V1-V5 have been applied.
-- Restrict the host part to the API server's source IP where possible.
CREATE USER 'salchichon_readonly'@'%' IDENTIFIED BY 'REPLACE_WITH_A_LONG_RANDOM_PASSWORD';

GRANT SELECT ON salchichon.player_data TO 'salchichon_readonly'@'%';
GRANT SELECT ON salchichon.player_stats TO 'salchichon_readonly'@'%';
GRANT SELECT ON salchichon.player_skins TO 'salchichon_readonly'@'%';
GRANT SELECT ON salchichon.public_clans TO 'salchichon_readonly'@'%';
GRANT SELECT ON salchichon.player_clan_memberships TO 'salchichon_readonly'@'%';
GRANT SELECT ON salchichon.public_announcements TO 'salchichon_readonly'@'%';

-- Deliberately no INSERT, UPDATE, DELETE, or access to audit, vault, or trade data.
