package com.cxp.platform.integration.salesforce;

import com.cxp.platform.integration.salesforce.application.ServiceRequestSyncPersistenceService;
import com.cxp.platform.servicerequest.entity.ServiceRequest;
import com.cxp.platform.servicerequest.entity.ServiceRequestExternalSyncStatus;
import com.cxp.platform.servicerequest.repository.ServiceRequestRepository;
import com.cxp.platform.servicerequest.service.ServiceRequestEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ServiceRequestSyncPersistenceService.
 *
 * The primary consistency invariant tested here:
 *   recordSyncSuccess/Failure (event append) and serviceRequestRepository.save (status update)
 *   must be called as a unit. If either throws, the @Transactional wrapper rolls both back.
 *
 * Transaction rollback is infrastructure-level and cannot be tested without a real DB.
 * These tests verify:
 *   1. Both writes are issued in the correct order on the happy path.
 *   2. Status and error fields are set correctly before save.
 *   3. If the event write throws, the exception propagates (triggering transaction rollback).
 */
@ExtendWith(MockitoExtension.class)
class ServiceRequestSyncPersistenceServiceTest {

    @Mock private ServiceRequestRepository serviceRequestRepository;
    @Mock private ServiceRequestEventService eventService;

    private ServiceRequestSyncPersistenceService service;

    private static final UUID SR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String CASE_ID = "5001X000002ABCdEAF";
    private static final String ERROR_MSG = "Salesforce timeout";

    @BeforeEach
    void setUp() {
        service = new ServiceRequestSyncPersistenceService(serviceRequestRepository, eventService);
    }

    // ── persistSyncSuccess ────────────────────────────────────────────────────

    @Test
    void persistSyncSuccess_setsStatusToSynced() {
        ServiceRequest req = makeRequest();
        when(serviceRequestRepository.save(any())).thenReturn(req);

        service.persistSyncSuccess(req, CASE_ID);

        assertThat(req.getExternalSyncStatus()).isEqualTo(ServiceRequestExternalSyncStatus.SYNCED);
        assertThat(req.getExternalSyncError()).isNull();
        assertThat(req.getExternalSyncLastAttemptAt()).isNotNull();
    }

    @Test
    void persistSyncSuccess_recordsEventBeforeSave() {
        ServiceRequest req = makeRequest();
        when(serviceRequestRepository.save(any())).thenReturn(req);

        service.persistSyncSuccess(req, CASE_ID);

        InOrder order = inOrder(eventService, serviceRequestRepository);
        order.verify(eventService).recordSyncSuccess(SR_ID, CASE_ID);
        order.verify(serviceRequestRepository).save(req);
    }

    @Test
    void persistSyncSuccess_eventThrows_exceptionPropagates() {
        ServiceRequest req = makeRequest();
        doThrow(new RuntimeException("event write failed"))
                .when(eventService).recordSyncSuccess(any(), any());

        assertThatThrownBy(() -> service.persistSyncSuccess(req, CASE_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("event write failed");
    }

    // ── persistSyncFailure ────────────────────────────────────────────────────

    @Test
    void persistSyncFailure_notExhausted_setsRetrying() {
        ServiceRequest req = makeRequest();
        when(serviceRequestRepository.save(any())).thenReturn(req);

        service.persistSyncFailure(req, false, ERROR_MSG);

        assertThat(req.getExternalSyncStatus()).isEqualTo(ServiceRequestExternalSyncStatus.RETRYING);
        assertThat(req.getExternalSyncError()).isEqualTo(ERROR_MSG);
    }

    @Test
    void persistSyncFailure_exhausted_setsFailed() {
        ServiceRequest req = makeRequest();
        when(serviceRequestRepository.save(any())).thenReturn(req);

        service.persistSyncFailure(req, true, ERROR_MSG);

        assertThat(req.getExternalSyncStatus()).isEqualTo(ServiceRequestExternalSyncStatus.FAILED);
    }

    @Test
    void persistSyncFailure_recordsEventBeforeSave() {
        ServiceRequest req = makeRequest();
        when(serviceRequestRepository.save(any())).thenReturn(req);

        service.persistSyncFailure(req, false, ERROR_MSG);

        InOrder order = inOrder(eventService, serviceRequestRepository);
        order.verify(eventService).recordSyncFailure(SR_ID, ERROR_MSG);
        order.verify(serviceRequestRepository).save(req);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private ServiceRequest makeRequest() {
        ServiceRequest sr = new ServiceRequest();
        ReflectionTestUtils.setField(sr, "id", SR_ID);
        return sr;
    }
}
