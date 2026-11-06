CREATE TABLE IF NOT EXISTS public_announcements (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    body TEXT NOT NULL,
    category VARCHAR(32) NOT NULL DEFAULT 'UPDATE',
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    expires_at DATETIME(6) NULL,
    created_by VARCHAR(64) NOT NULL DEFAULT 'system',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_public_announcements_expiry
        CHECK (expires_at IS NULL OR expires_at > published_at),
    INDEX idx_public_announcements_visible (is_published, published_at DESC, id DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
