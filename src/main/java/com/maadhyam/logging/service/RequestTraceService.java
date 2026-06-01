package com.maadhyam.logging.service;

import com.maadhyam.logging.entity.RequestTrace;
import com.maadhyam.logging.enums.RequestState;
import com.maadhyam.logging.repository.RequestTraceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestTraceService {

    private final RequestTraceRepository requestTraceRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<RequestTrace> startTrace(String correlationId,
                                             String endpoint,
                                             String httpMethod,
                                             UUID userId,
                                             UUID wardId,
                                             UUID governingBodyId,
                                             String jwtSubject,
                                             String sourcePlatform,
                                             String appVersion) {
        try {
            RequestTrace trace = RequestTrace.builder()
                    .correlationId(correlationId)
                    .requestState(RequestState.PROCESSING)
                    .requestTimestamp(Instant.now())
                    .endpoint(endpoint)
                    .httpMethod(httpMethod)
                    .userId(userId)
                    .wardId(wardId)
                    .governingBodyId(governingBodyId)
                    .jwtSubject(jwtSubject)
                    .sourcePlatform(sourcePlatform)
                    .appVersion(appVersion)
                    .build();

            log.debug("REQUEST_TRACE_SAVE_START correlationId={} method={} endpoint={}", correlationId, httpMethod, endpoint);
            RequestTrace savedTrace = requestTraceRepository.save(trace);
            log.info("REQUEST_TRACE_SAVE_SUCCESS correlationId={} traceId={} method={} endpoint={}",
                    correlationId, savedTrace.getId(), httpMethod, endpoint);
            return Optional.of(savedTrace);
        } catch (Exception ex) {
            log.error("REQUEST_TRACE_SAVE_FAILED correlationId={} method={} endpoint={} type={} reason={}",
                    correlationId, httpMethod, endpoint, ex.getClass().getName(), ex.getMessage(), ex);
            return Optional.empty();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeTrace(UUID traceId,
                              int responseStatus,
                              long executionTimeMs,
                              String requestPayload,
                              String errorMessage) {
        if (traceId == null) {
            return;
        }

        try {
            requestTraceRepository.findById(traceId).ifPresent(trace -> {
                trace.setResponseStatus(responseStatus);
                trace.setExecutionTimeMs(executionTimeMs);
                trace.setRequestPayload(requestPayload);
                trace.setErrorMessage(errorMessage);
                trace.setRequestState(errorMessage == null && responseStatus < 400
                        ? RequestState.COMPLETED
                        : RequestState.FAILED);
                requestTraceRepository.save(trace);

                log.info("request_trace_completed correlationId={} traceId={} state={} status={} durationMs={}",
                        trace.getCorrelationId(), trace.getId(), trace.getRequestState(), responseStatus, executionTimeMs);
            });
        } catch (Exception ex) {
            log.warn("request_trace_complete_failed traceId={} reason={}", traceId, ex.getMessage());
        }
    }
}
