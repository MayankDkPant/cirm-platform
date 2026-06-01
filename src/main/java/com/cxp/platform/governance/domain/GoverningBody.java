package com.cxp.platform.governance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Reference/master data entity. Does not use BaseEntity.
 * Maps only the columns required by application queries.
 *
 * Hierarchy: state → district → city → governing_body → zone → ward
 */
@Entity
@Table(name = "governing_body")
@Getter
@Setter
public class GoverningBody {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    // ── Civic hierarchy FKs ───────────────────────────────────────────────────

    /** State the governing body is in (from V5). */
    @Column(name = "state_id")
    private UUID stateId;

    /** Administrative district (from V12). */
    @Column(name = "district_id")
    private UUID districtId;

    /** City administered by this body (from V46). */
    @Column(name = "city_id")
    private UUID cityId;
}
