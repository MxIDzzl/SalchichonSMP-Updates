ALTER TABLE public_announcements
    ADD COLUMN IF NOT EXISTS scheduled_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS is_teaser BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_public_announcements_upcoming
    ON public_announcements (scheduled_at ASC, id ASC)
    WHERE is_published = TRUE AND scheduled_at IS NOT NULL;
