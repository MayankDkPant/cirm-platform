package com.cxp.platform.servicerequest.repository;

import com.cxp.platform.servicerequest.entity.ServiceRequestEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceRequestEventRepository extends JpaRepository<ServiceRequestEvent, UUID> {

    List<ServiceRequestEvent> findByServiceRequestIdOrderByCreatedAtAsc(UUID serviceRequestId);
}
