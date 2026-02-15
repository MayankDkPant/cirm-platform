package com.cirm.platform.complaint.repository;

import com.cirm.platform.complaint.entity.ComplaintEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ComplaintEventRepository 
        extends JpaRepository<ComplaintEvent, UUID> {
}
