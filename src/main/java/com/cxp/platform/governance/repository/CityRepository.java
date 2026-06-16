package com.cxp.platform.governance.repository;

import com.cxp.platform.governance.domain.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CityRepository extends JpaRepository<City, UUID> {

    /** Returns active cities for a district, alphabetically. Used by the discovery API. */
    List<City> findByDistrictIdAndActiveTrueOrderByNameAsc(UUID districtId);
}
