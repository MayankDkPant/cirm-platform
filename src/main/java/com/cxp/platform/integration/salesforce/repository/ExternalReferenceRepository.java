package com.cxp.platform.integration.salesforce.repository;

import java.util.Optional;
import java.util.UUID;

public interface ExternalReferenceRepository {

    Optional<String> findExternalId(String entityType, UUID entityUuid, UUID governingBodyId);
}
