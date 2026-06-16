package com.cxp.platform.servicerequest.repository;

import com.cxp.platform.servicerequest.entity.ServiceRequest;
import com.cxp.platform.servicerequest.entity.ServiceRequestExternalSyncStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {

    // Operator queries — scoped to governing body (TenantContext)
    List<ServiceRequest> findByGoverningBodyIdOrderByCreatedAtDesc(UUID governingBodyId);
    Page<ServiceRequest> findByGoverningBodyIdOrderByCreatedAtDesc(UUID governingBodyId, Pageable pageable);
    Optional<ServiceRequest> findByIdAndGoverningBodyId(UUID id, UUID governingBodyId);

    // Citizen queries — scoped to the submitting user
    List<ServiceRequest> findByCreatedByUserIdOrderByCreatedAtDesc(UUID createdByUserId);
    Page<ServiceRequest> findByCreatedByUserIdOrderByCreatedAtDesc(UUID createdByUserId, Pageable pageable);
    Optional<ServiceRequest> findByIdAndCreatedByUserId(UUID id, UUID createdByUserId);

    Page<ServiceRequest> findTop20ByExternalSyncStatusInAndExternalSyncAttemptsLessThan(
            List<ServiceRequestExternalSyncStatus> statuses,
            int maxAttempts,
            Pageable pageable
    );

    @Deprecated
    @Override
    List<ServiceRequest> findAll();

    @Deprecated
    @Override
    Optional<ServiceRequest> findById(UUID id);
}
