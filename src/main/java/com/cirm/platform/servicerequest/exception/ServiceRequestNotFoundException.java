package com.cirm.platform.servicerequest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public class ServiceRequestNotFoundException extends ResponseStatusException {

    public ServiceRequestNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "Service request not found with id " + id);
    }
}
