package com.cxp.platform.announcement.domain;

/**
 * Geographic targeting scope for a civic announcement.
 *
 * Each value names the geographic unit the announcement is addressed to.
 * The corresponding FK column (ward_id, zone_id, city_id, district_id, state_id)
 * must be populated at creation time; all other geo FK columns must be null.
 *
 * All announcements are public civic communications — there is no PRIVATE scope.
 * This enum describes geographic reach, not access control.
 *
 * FUTURE — planned scopes (not yet active):
 *   LOCALITY  – sub-ward neighbourhood level; requires locality_id
 *   COUNTRY   – national announcements; requires country_id
 *   CUSTOM    – operator-defined audience via a many-to-many targeting table;
 *               replaces single-FK model when multi-ward or multi-city targeting is needed
 *
 * When multi-target announcements are introduced, the single-FK model here will be
 * replaced by an announcement_target junction table, and this enum value will
 * describe the type of targets rather than imply a single reference ID.
 */
public enum AnnouncementTargetScope {

    /** Ward advisory. Targets citizens within a specific ward. Requires ward_id. */
    WARD,

    /** Zone notice. Targets citizens within all wards of a zone. Requires zone_id. */
    ZONE,

    /** City notice. Targets all citizens within a city. Requires city_id. */
    CITY,

    /** District advisory. Targets all citizens in an administrative district. Requires district_id. */
    DISTRICT,

    /** State advisory. State-wide communication — emergency, policy, or weather alert. Requires state_id. */
    STATE
}
