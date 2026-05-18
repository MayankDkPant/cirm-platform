package com.cxp.platform.servicerequest.repository;

import com.cxp.platform.servicerequest.entity.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {

    List<ServiceRequest> findByGoverningBodyIdOrderByCreatedAtDesc(UUID governingBodyId);

    Optional<ServiceRequest> findByIdAndGoverningBodyId(UUID id, UUID governingBodyId);

    @Deprecated
    @Override
    List<ServiceRequest> findAll();

    @Deprecated
    @Override
    Optional<ServiceRequest> findById(UUID id);
}
