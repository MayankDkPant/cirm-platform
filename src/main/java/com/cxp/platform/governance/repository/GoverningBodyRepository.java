package com.cxp.platform.governance.repository;

import com.cxp.platform.governance.domain.GoverningBody;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GoverningBodyRepository extends JpaRepository<GoverningBody, UUID> {
}
