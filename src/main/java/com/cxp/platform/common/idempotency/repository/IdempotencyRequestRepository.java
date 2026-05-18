package com.cxp.platform.common.idempotency.repository;

import com.cxp.platform.common.idempotency.entity.IdempotencyRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRequestRepository extends JpaRepository<IdempotencyRequest, UUID> {

    Optional<IdempotencyRequest> findByIdempotencyKeyAndGoverningBodyIdAndExpiresAtAfter(
            String idempotencyKey,
            UUID governingBodyId,
            LocalDateTime now
    );
}
