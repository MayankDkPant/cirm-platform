package com.cxp.platform.governance.repository;

import com.cxp.platform.governance.domain.GoverningBody;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoverningBodyRepository extends JpaRepository<GoverningBody, UUID> {

    Optional<GoverningBody> findFirstByIsActiveTrue();

    /** Returns active governing bodies for a city, alphabetically. Used by the discovery API. */
    List<GoverningBody> findByCityIdAndIsActiveTrueOrderByNameAsc(UUID cityId);
}
