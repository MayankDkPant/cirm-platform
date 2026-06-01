package com.cxp.platform.governance.repository;

import com.cxp.platform.governance.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    /** Returns active, non-deleted departments for a governing body, alphabetically. Used by the discovery API. */
    List<Department> findByGoverningBodyIdAndIsActiveTrueAndIsDeletedFalseOrderByNameAsc(UUID governingBodyId);
}
