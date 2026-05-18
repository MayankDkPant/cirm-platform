-- These columns store the location routing snapshot derived during
-- complaint creation using the LocationIntelligenceService.
-- They are used for audit, analytics, and external system integration.

ALTER TABLE complaints
    ADD COLUMN reported_location_text TEXT,
    ADD COLUMN municipality_name VARCHAR(255),
    ADD COLUMN ward_id UUID,
    ADD COLUMN ward_name VARCHAR(255),
    ADD COLUMN reportedLocationText VARCHAR(500);
