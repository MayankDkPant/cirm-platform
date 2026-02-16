package com.cirm.platform.complaint.repository;

import com.cirm.platform.complaint.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import org.springframework.context.annotation.Profile;

@Profile("complaint")  

public interface ComplaintRepository 
        extends JpaRepository<Complaint, UUID> {
}
