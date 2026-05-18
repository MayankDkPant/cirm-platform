package com.cirm.platform.governance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Reference/master data entity. Does not use BaseEntity.
 */
@Entity
@Table(name = "ward")
@Getter
@Setter
public class Ward {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "zone_id")
    private UUID zoneId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "governing_body_id", nullable = false)
    private GoverningBody governingBody;
}
