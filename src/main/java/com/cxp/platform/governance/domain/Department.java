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
 * Departments are scoped to a governing body — the governing_body_id FK is NOT NULL
 * in the schema (V5). A department code is unique within a governing body, not globally.
 * Use the discovery API (/api/v1/governing-bodies/{id}/departments) to enumerate
 * departments for a given authority; never assume a department is global.
 *
 * Only active (is_active=true, is_deleted=false) departments are surfaced by the API.
 */
@Entity
@Table(name = "department")
@Getter
@Setter
public class Department {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    // V13 renamed this column from "code" to "Code__c" (Salesforce convention).
    // Backtick quoting forces Hibernate to emit "Code__c" (case-preserved) in SQL.
    @Column(name = "`Code__c`", nullable = false)
    private String code;

    /** Owning governing body — NOT NULL, part of the unique key (governing_body_id, code). */
    @Column(name = "governing_body_id", nullable = false)
    private UUID governingBodyId;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;
}
