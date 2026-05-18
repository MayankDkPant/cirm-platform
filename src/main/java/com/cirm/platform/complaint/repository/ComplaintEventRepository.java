package com.cirm.platform.complaint.repository;

import com.cirm.platform.complaint.domain.ComplaintEvent;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Do not add update/delete methods. This table is append-only.
 */
public interface ComplaintEventRepository 
        extends Repository<ComplaintEvent, UUID> {

    ComplaintEvent save(ComplaintEvent event);

    Optional<ComplaintEvent> findById(UUID id);

    List<ComplaintEvent> findByComplaintId(UUID complaintId);
}
