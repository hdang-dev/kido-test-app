package com.kido.testapp.common;

import java.util.Map;

import org.springframework.http.HttpStatus;

/** A business-rule failure with a known HTTP status, mapped to {@link ErrorResponse} by the handler. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final Map<String, String> fieldErrors;

    public ApiException(HttpStatus status, String message) {
        this(status, message, Map.of());
    }

    public ApiException(HttpStatus status, String message, Map<String, String> fieldErrors) {
        super(message);
        this.status = status;
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    /** Validation-style failure on one form field, e.g. ("currentPassword", "Current password is incorrect"). */
    public static ApiException field(HttpStatus status, String field, String message) {
        return new ApiException(status, message, Map.of(field, message));
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
