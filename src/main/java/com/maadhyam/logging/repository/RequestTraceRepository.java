package com.maadhyam.logging.repository;

import com.maadhyam.logging.entity.RequestTrace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RequestTraceRepository extends JpaRepository<RequestTrace, UUID> {
}
