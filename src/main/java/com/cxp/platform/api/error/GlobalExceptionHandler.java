package com.cxp.platform.api.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                        (a, b) -> a,
                        LinkedHashMap::new));
        String message = fieldErrors.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", "));
        log.warn("GLOBAL_EXCEPTION type=MethodArgumentNotValidException status=400 path={} fields={}",
                request.getRequestURI(), fieldErrors);
        return new ApiError(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "VALIDATION_FAILED", message, request.getRequestURI(), fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        log.warn("GLOBAL_EXCEPTION type=ConstraintViolationException status=400 path={} message={}",
                request.getRequestURI(), ex.getMessage());
        return new ApiError(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.name(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(Exception ex, HttpServletRequest request) {
        log.warn("GLOBAL_EXCEPTION type={} status=404 path={}", ex.getClass().getSimpleName(), request.getRequestURI());
        return new ApiError(LocalDateTime.now(), HttpStatus.NOT_FOUND.value(), "NOT_FOUND",
                "Resource not found: " + request.getRequestURI(), request.getRequestURI());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiError handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("GLOBAL_EXCEPTION type=HttpRequestMethodNotSupportedException status=405 path={} method={}",
                request.getRequestURI(), ex.getMethod());
        return new ApiError(LocalDateTime.now(), HttpStatus.METHOD_NOT_ALLOWED.value(), "METHOD_NOT_ALLOWED",
                ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("GLOBAL_EXCEPTION type=HttpMessageNotReadableException status=400 path={} cause={}",
                request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return new ApiError(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), "MALFORMED_REQUEST",
                "Request body could not be parsed: " + ex.getMostSpecificCause().getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        log.error("GLOBAL_EXCEPTION type={} status=400 path={} message={}",
                ex.getClass().getName(), request.getRequestURI(), ex.getMessage(), ex);
        return new ApiError(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.name(), ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("GLOBAL_EXCEPTION type={} status=500 path={} message={}",
                ex.getClass().getName(), request.getRequestURI(), ex.getMessage(), ex);
        return new ApiError(LocalDateTime.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.name(), "An unexpected error occurred", request.getRequestURI());
    }
}
