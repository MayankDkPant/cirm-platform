package com.maadhyam.logging.filter;

import com.cxp.platform.auth.application.UserPrincipal;
import com.maadhyam.logging.entity.RequestTrace;
import com.maadhyam.logging.service.RequestTraceService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestTraceFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String SOURCE_PLATFORM_HEADER = "X-Source-Platform";
    private static final String APP_VERSION_HEADER = "X-App-Version";
    private static final int MAX_PAYLOAD_LENGTH = 5000;

    private final RequestTraceService requestTraceService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/actuator/health")
                || path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/webjars")
                || path.equals("/favicon.ico")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/static/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        log.info("REQUEST_TRACE_FILTER_ENTERED method={} path={}", request.getMethod(), request.getRequestURI());

        long startedAt = System.currentTimeMillis();
        String correlationId = resolveCorrelationId(request);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        MDC.put("correlationId", correlationId);

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        Optional<RequestTrace> trace = Optional.empty();
        Exception requestException = null;

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            boolean isAuthenticated = authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal());
            log.info("REQUEST_TRACE_AUTH_STATE path={} authenticated={} principal={}",
                    request.getRequestURI(), isAuthenticated,
                    authentication != null ? authentication.getPrincipal().getClass().getSimpleName() : "null");

            TraceIdentity identity = extractIdentity(authentication);

            log.debug("REQUEST_TRACE_SAVE_START correlationId={} path={}", correlationId, request.getRequestURI());
            trace = requestTraceService.startTrace(
                    correlationId,
                    truncate(request.getRequestURI(), 500),
                    truncate(request.getMethod(), 20),
                    identity.userId(),
                    identity.wardId(),
                    identity.governingBodyId(),
                    truncate(identity.jwtSubject(), 255),
                    truncate(request.getHeader(SOURCE_PLATFORM_HEADER), 50),
                    truncate(request.getHeader(APP_VERSION_HEADER), 50)
            );
            if (trace.isPresent()) {
                log.debug("REQUEST_TRACE_SAVE_SUCCESS correlationId={} traceId={}", correlationId, trace.get().getId());
            } else {
                log.warn("REQUEST_TRACE_SAVE_FAILED correlationId={} path={} reason=startTrace_returned_empty",
                        correlationId, request.getRequestURI());
            }

            filterChain.doFilter(wrappedRequest, response);
        } catch (Exception ex) {
            requestException = ex;
            throw ex;
        } finally {
            try {
                long executionTimeMs = System.currentTimeMillis() - startedAt;
                String errorMessage = requestException == null ? null : buildErrorMessage(requestException);
                String payload = shouldLogPayload(wrappedRequest) ? extractPayload(wrappedRequest) : null;

                requestTraceService.completeTrace(
                        trace.map(RequestTrace::getId).orElse(null),
                        response.getStatus(),
                        executionTimeMs,
                        payload,
                        errorMessage
                );
            } finally {
                MDC.remove("correlationId");
            }
        }
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return truncate(correlationId.trim(), 100);
    }

    private TraceIdentity extractIdentity(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return TraceIdentity.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            UUID userId = userPrincipal.getUserId();
            return new TraceIdentity(userId, null, userPrincipal.getTenantId(), userId == null ? null : userId.toString());
        }

        if (principal instanceof Jwt jwt) {
            return new TraceIdentity(
                    parseUuidClaim(jwt, "user_id"),
                    parseUuidClaim(jwt, "ward_id"),
                    parseUuidClaim(jwt, "governing_body_id"),
                    jwt.getSubject()
            );
        }

        if (principal instanceof String principalValue && !"anonymousUser".equals(principalValue)) {
            return new TraceIdentity(null, null, null, principalValue);
        }

        return TraceIdentity.empty();
    }

    private UUID parseUuidClaim(Jwt jwt, String claimName) {
        Object value = jwt.getClaims().get(claimName);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean shouldLogPayload(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType == null) {
            return false;
        }

        String normalized = contentType.toLowerCase();
        return (normalized.startsWith(MediaType.APPLICATION_JSON_VALUE)
                || normalized.startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                || normalized.startsWith(MediaType.TEXT_PLAIN_VALUE))
                && !normalized.startsWith(MediaType.MULTIPART_FORM_DATA_VALUE);
    }

    private String extractPayload(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length == 0) {
            return null;
        }
        String payload = new String(content, StandardCharsets.UTF_8);
        return truncate(maskSensitiveValues(payload), MAX_PAYLOAD_LENGTH);
    }

    private String buildErrorMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = ex.getClass().getSimpleName();
        }
        return truncate(message, MAX_PAYLOAD_LENGTH);
    }

    private String maskSensitiveValues(String payload) {
        return payload
                .replaceAll("(?i)(\"password\"\\s*:\\s*\")[^\"]*(\")", "$1***$2")
                .replaceAll("(?i)(\"refreshToken\"\\s*:\\s*\")[^\"]*(\")", "$1***$2")
                .replaceAll("(?i)(\"refresh_token\"\\s*:\\s*\")[^\"]*(\")", "$1***$2")
                .replaceAll("(?i)(\"otp\"\\s*:\\s*\")[^\"]*(\")", "$1***$2")
                .replaceAll("(?i)(\"token\"\\s*:\\s*\")[^\"]*(\")", "$1***$2")
                .replaceAll("(?i)(password|refresh_token|refreshToken|otp|token)=([^&\\s]+)", "$1=***");
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record TraceIdentity(UUID userId, UUID wardId, UUID governingBodyId, String jwtSubject) {
        private static TraceIdentity empty() {
            return new TraceIdentity(null, null, null, null);
        }
    }
}
