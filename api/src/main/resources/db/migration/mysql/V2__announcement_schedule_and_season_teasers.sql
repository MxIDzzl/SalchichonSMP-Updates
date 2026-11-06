ALTER TABLE public_announcements
    ADD COLUMN scheduled_at DATETIME(6) NULL,
    ADD COLUMN is_teaser BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_public_announcements_upcoming
    ON public_announcements (is_published, scheduled_at, id);
