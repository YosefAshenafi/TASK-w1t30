package com.meridian.common.web;

import com.meridian.auth.PasswordPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorEnvelope handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> details = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (a, b) -> a
                ));
        return ErrorEnvelope.of("VALIDATION_FAILED", "Validation failed", details);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorEnvelope handleNotFound(NoResourceFoundException ex) {
        return ErrorEnvelope.of("NOT_FOUND", "The requested resource was not found");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorEnvelope handleAccessDenied(AccessDeniedException ex) {
        return ErrorEnvelope.of("FORBIDDEN", "Access denied");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorEnvelope> handleResponseStatus(ResponseStatusException ex) {
        String code = ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString();
        return ResponseEntity.status(ex.getStatusCode())
                .body(ErrorEnvelope.of(code, ex.getMessage()));
    }

    @ExceptionHandler(PasswordPolicy.PasswordPolicyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorEnvelope handlePasswordPolicy(PasswordPolicy.PasswordPolicyException ex) {
        return ErrorEnvelope.of("VALIDATION_FAILED", ex.getMessage(),
                Map.of("password", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorEnvelope handleIllegalArgument(IllegalArgumentException ex) {
        return ErrorEnvelope.of("BAD_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorEnvelope handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ErrorEnvelope.of("INTERNAL_ERROR", "An unexpected error occurred");
    }
}
