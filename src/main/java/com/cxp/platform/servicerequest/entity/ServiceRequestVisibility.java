package com.cxp.platform.servicerequest.entity;

/**
 * Persisted citizen intent for who may view this service request.
 *
 * IMPORTANT — this is NOT an access-control list.
 * It records the scope the citizen chose at submission time.
 * Actual audience resolution (the concrete set of viewers) happens dynamically
 * at runtime using ward_id, governing_body_id, city context, citizen profile,
 * and the governance hierarchy. No component should read this enum and directly
 * decide visibility without going through the runtime audience resolver.
 *
 * Adding new scopes here requires only a column-length check (VARCHAR 50) — no
 * schema change is needed as long as the name stays under 50 characters.
 */
public enum ServiceRequestVisibility {

    /** Visible only to the submitting citizen and authorised operators. Default. */
    PRIVATE,

    /** Visible to all authenticated citizens within the same city. */
    CITY_PUBLIC,

    /**
     * Visible to authenticated citizens within the same ward.
     * Future-ready: ward resolution at runtime will use the persisted ward_id.
     */
    WARD_PUBLIC

    // ── Planned future scopes (not yet active) ──────────────────────────────
    // LOCALITY_PUBLIC      – neighbourhood/locality level
    // DEPARTMENT_PUBLIC    – citizens following a specific department
    // VERIFIED_RESIDENTS   – residents with a governance-verified address in the ward
}
