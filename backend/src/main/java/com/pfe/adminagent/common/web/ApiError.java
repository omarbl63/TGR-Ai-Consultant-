package com.pfe.adminagent.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

/**
 * RFC-7807-inspired error payload returned by the API for every failure.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldViolation> violations
) {
    public static ApiError of(HttpStatus status, String message, String path) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, null);
    }

    public static ApiError of(HttpStatus status, String message, String path, List<FieldViolation> violations) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, violations);
    }

    public record FieldViolation(String field, String message) {
    }
}
