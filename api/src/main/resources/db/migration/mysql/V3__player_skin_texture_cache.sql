CREATE TABLE IF NOT EXISTS player_skins (
    player_uuid CHAR(36) NOT NULL PRIMARY KEY,
    texture_hash VARCHAR(64) NOT NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_player_skins_texture_hash
        CHECK (CHAR_LENGTH(texture_hash) BETWEEN 32 AND 64),
    CONSTRAINT fk_player_skins_player
        FOREIGN KEY (player_uuid) REFERENCES player_data(uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
