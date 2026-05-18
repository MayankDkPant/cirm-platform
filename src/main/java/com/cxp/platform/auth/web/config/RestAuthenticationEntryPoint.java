package com.cxp.platform.auth.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Authentication entry point returning a consistent JSON response for 401 errors.
 *
 * This component centralizes unauthenticated API error handling and ensures
 * clients receive deterministic machine-readable authentication failures.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * Writes a 401 JSON response when authentication is missing or invalid.
     *
     * @param request incoming HTTP request
     * @param response outgoing HTTP response
     * @param authException security exception that triggered the entry point
     * @throws IOException when writing the response fails
     * @throws ServletException when servlet handling fails
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        log.warn("unauthorized_request path={} reason={}", request.getRequestURI(), authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(
                response.getOutputStream(),
                Map.of(
                        "error", "Unauthorized",
                        "message", "Invalid or missing token"
                )
        );
    }
}
