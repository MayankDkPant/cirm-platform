package com.cxp.platform.governance.repository;

import com.cxp.platform.governance.domain.Ward;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WardRepository extends JpaRepository<Ward, UUID> {
}
