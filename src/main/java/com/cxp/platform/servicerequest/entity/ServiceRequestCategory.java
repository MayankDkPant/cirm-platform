package com.cxp.platform.servicerequest.entity;

/**
 * Canonical category enum for citizen service requests.
 *
 * Backend is the source of truth for these values. Frontend maps each constant
 * to a localised display label; it must never send raw translated strings to the
 * API. AI classification output is normalised to this enum before persistence —
 * unknown AI strings collapse to OTHER.
 *
 * DB column remains VARCHAR — enum constraint is enforced at the application layer.
 * Add new values here + add to the frontend i18n map; no schema migration required.
 */
public enum ServiceRequestCategory {

    WASTE_MANAGEMENT,
    ROAD_DAMAGE,
    WATER_SUPPLY,
    STREETLIGHT,
    SEWERAGE,
    DRAINAGE,
    ENCROACHMENT,
    PARK_MAINTENANCE,
    TRAFFIC,
    NOISE_COMPLAINT,
    BUILDING_CONSTRUCTION,
    STRAY_ANIMALS,
    TREE_MAINTENANCE,

    /** Catch-all. Used when AI cannot classify confidently or service is unavailable. */
    OTHER
}
