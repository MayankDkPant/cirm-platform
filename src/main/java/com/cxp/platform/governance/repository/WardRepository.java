package com.cxp.platform.governance.repository;

import com.cxp.platform.governance.domain.Ward;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WardRepository extends JpaRepository<Ward, UUID> {

    @Query("SELECT w.governingBody.id FROM Ward w WHERE w.id = :id")
    Optional<UUID> findGoverningBodyIdById(@Param("id") UUID id);

    @Query("SELECT w FROM Ward w JOIN FETCH w.governingBody WHERE LOWER(w.name) = LOWER(:name)")
    Optional<Ward> findByNameFetchingGoverningBody(@Param("name") String name);

    /** Returns all wards for a governing body, alphabetically. Used by the discovery API. */
    @Query("SELECT w FROM Ward w WHERE w.governingBody.id = :governingBodyId ORDER BY w.name ASC")
    List<Ward> findByGoverningBodyIdOrderByNameAsc(@Param("governingBodyId") UUID governingBodyId);
}
