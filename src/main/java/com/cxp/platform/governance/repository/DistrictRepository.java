package com.cxp.platform.governance.repository;

import com.cxp.platform.governance.domain.District;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DistrictRepository extends JpaRepository<District, UUID> {

    List<District> findByStateIdOrderByNameAsc(UUID stateId);
}
