package com.cirm.platform.complaint.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.cirm.platform.complaint.entity.Complaint;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
}
