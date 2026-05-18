package com.cxp.platform.complaint.repository;

import com.cxp.platform.complaint.domain.Complaint;
import com.cxp.platform.complaint.domain.enums.ExternalSyncStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Complaint repository for persistence operations.
 * All read queries must be tenant scoped using governingBodyId.
 */
public interface ComplaintRepository 
        extends JpaRepository<Complaint, UUID> {

    List<Complaint> findByGoverningBodyIdAndStatus(UUID governingBodyId, String status);

    List<Complaint> findByGoverningBodyIdOrderByCreatedAtDesc(UUID governingBodyId);

    Optional<Complaint> findByIdAndGoverningBodyId(UUID id, UUID governingBodyId);

    Page<Complaint> findTop20ByExternalSyncStatusInAndExternalSyncAttemptsLessThan(
            List<ExternalSyncStatus> statuses,
            int maxAttempts,
            Pageable pageable
    );

    /**
     * Do not use without tenant filter.
     */
    @Deprecated
    @Override
    List<Complaint> findAll();

    /**
     * Do not use without tenant filter.
     */
    @Deprecated
    @Override
    Optional<Complaint> findById(UUID id);
}
