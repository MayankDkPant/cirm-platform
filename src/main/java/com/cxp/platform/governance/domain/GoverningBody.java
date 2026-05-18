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
 */
@Entity
@Table(name = "governing_body")
@Getter
@Setter
public class GoverningBody {

    @Id
    @Column(nullable = false)
    private UUID id;
}
