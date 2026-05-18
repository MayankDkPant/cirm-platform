package com.cxp.platform.complaint.exception;

import java.util.UUID;

public class ComplaintNotFoundException extends RuntimeException {

    public ComplaintNotFoundException(UUID complaintId) {
        super("Complaint not found with id " + complaintId);
    }
}
