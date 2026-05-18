package com.cirm.platform.governance.repository;

import com.cirm.platform.governance.domain.Ward;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WardRepository extends JpaRepository<Ward, UUID> {
}
