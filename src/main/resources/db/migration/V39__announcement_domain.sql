-- ============================================================================
-- Announcement domain — government-to-citizen communication infrastructure.
--
-- Announcements represent targeted civic communications issued by a governing
-- body (city, ward, department) to citizen audiences. This table is designed
-- to be the backbone of a localized civic information feed, ward/city targeting
-- system, and future engagement/notification engine.
--
-- Design notes:
--
-- visibility_scope  Persisted intent of the issuing authority — NOT a direct
--                   access-control list. Actual audience resolution (who concretely
--                   receives/sees the announcement) happens at runtime in the
--                   audience-resolver service, using governing_body_id, ward_id,
--                   locality_id, citizen profile, and governance hierarchy.
--
-- is_public         Derived convenience flag (true when visibility_scope != PRIVATE).
--                   Enables cheap boolean pre-filter on civic-feed queries before
--                   the full audience-resolution logic runs.
--
-- metadata          Flexible JSONB store for future extensions (translations,
--                   related links, AI summarisation output, media attachments, etc.)
--                   without schema changes.
--
-- tags              JSON array of topic labels for future topic-following,
--                   notification routing, and civic engagement analytics.
-- ============================================================================

CREATE TABLE IF NOT EXISTS announcement (
    id                  UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- ── Core content ─────────────────────────────────────────────────────────
    title               VARCHAR(300) NOT NULL,
    content             TEXT         NOT NULL,
    summary             VARCHAR(500),
    category            VARCHAR(100),
    priority            VARCHAR(30)  NOT NULL DEFAULT 'NORMAL',
    status              VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',

    -- ── Governance / targeting scope ──────────────────────────────────────────
    -- governing_body_id : the authority that issued this announcement.
    -- ward_id           : null = city-wide; non-null = ward-scoped feed.
    -- locality_id       : future sub-ward granularity (not yet active).
    governing_body_id   UUID        NOT NULL,
    ward_id             UUID,
    locality_id         UUID,

    CONSTRAINT fk_announcement_governing_body
        FOREIGN KEY (governing_body_id) REFERENCES governing_body(id),

    -- ── Visibility ────────────────────────────────────────────────────────────
    is_public           BOOLEAN     NOT NULL DEFAULT FALSE,
    visibility_scope    VARCHAR(50) NOT NULL DEFAULT 'PRIVATE',

    -- ── Publishing lifecycle ──────────────────────────────────────────────────
    published_at        TIMESTAMP WITH TIME ZONE,
    expires_at          TIMESTAMP WITH TIME ZONE,
    pinned              BOOLEAN     NOT NULL DEFAULT FALSE,

    -- ── Future extensibility ─────────────────────────────────────────────────
    -- metadata: flexible KV store — translations, AI summaries, media refs, etc.
    -- tags    : JSON array of topic labels, e.g. ["water", "road", "safety"]
    metadata            TEXT,
    tags                TEXT,

    -- ── Audit ────────────────────────────────────────────────────────────────
    created_by_user_id  UUID,
    updated_by_user_id  UUID,

    -- ── Soft delete / BaseEntity ─────────────────────────────────────────────
    is_active           BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE announcement IS
    'Government-to-citizen civic communications. Designed for city/ward-targeted feeds, '
    'future notification routing, and civic engagement analytics.';

COMMENT ON COLUMN announcement.visibility_scope IS
    'Persisted intent of the issuing authority (PRIVATE | CITY_PUBLIC | WARD_PUBLIC …). '
    'Actual viewer resolution happens at runtime via the audience-resolver service — '
    'this column is audience intent, not an access-control list.';

COMMENT ON COLUMN announcement.is_public IS
    'Derived flag: true when visibility_scope != PRIVATE. '
    'Enables fast pre-filtering of civic-feed queries.';

COMMENT ON COLUMN announcement.metadata IS
    'Flexible JSON object store for future extensions: multilingual content, '
    'AI-generated summaries, media attachment refs, reaction counts, etc.';

COMMENT ON COLUMN announcement.tags IS
    'JSON array of topic labels (e.g. ["water", "road"]). '
    'Used for topic-following, notification routing, and analytics.';

-- ── Indexes ──────────────────────────────────────────────────────────────────

-- Operator dashboard: list all announcements for a governing body, newest first.
CREATE INDEX IF NOT EXISTS idx_ann_tenant_created
    ON announcement(governing_body_id, created_at DESC);

-- Civic feed: published, public, pinned-first. Partial index avoids scanning private rows.
CREATE INDEX IF NOT EXISTS idx_ann_public_feed
    ON announcement(governing_body_id, published_at DESC, pinned DESC)
    WHERE is_public = TRUE AND status = 'PUBLISHED';

-- Ward-scoped feed sub-filter.
CREATE INDEX IF NOT EXISTS idx_ann_ward
    ON announcement(ward_id)
    WHERE ward_id IS NOT NULL AND is_public = TRUE;

-- Expiry sweep: find rows that need status transition to EXPIRED.
CREATE INDEX IF NOT EXISTS idx_ann_expires_at
    ON announcement(expires_at)
    WHERE expires_at IS NOT NULL AND status = 'PUBLISHED';
