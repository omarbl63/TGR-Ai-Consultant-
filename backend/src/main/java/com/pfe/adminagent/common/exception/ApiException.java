package com.pfe.adminagent.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for domain exceptions that carry an HTTP status.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
