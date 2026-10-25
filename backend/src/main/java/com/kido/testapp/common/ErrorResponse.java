package com.kido.testapp.common;

import java.util.Map;

/** Every API error body (design.md §7): a human message plus per-field messages for forms. */
public record ErrorResponse(String message, Map<String, String> fieldErrors) {

    public static ErrorResponse of(String message) {
        return new ErrorResponse(message, Map.of());
    }
}
