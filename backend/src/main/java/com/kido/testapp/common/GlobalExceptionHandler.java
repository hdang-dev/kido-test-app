package com.kido.testapp.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps exceptions to {@link ErrorResponse}. Messages never echo submitted values (could be passwords).
 * Extends {@link ResponseEntityExceptionHandler} so Spring MVC's own client errors (405, 415, 400 type
 * mismatch, 404 no resource, …) keep their 4xx status instead of falling into the 500 fallback.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> fieldErrors.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErrorResponse("Validation failed", fieldErrors));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.badRequest().body(ErrorResponse.of("Malformed request body"));
    }

    /** Every other Spring MVC exception: keep its status, replace the ProblemDetail body with ours. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        String message = status != null ? status.getReasonPhrase() : "Request failed";
        return ResponseEntity.status(statusCode).headers(headers).body(ErrorResponse.of(message));
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> api(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new ErrorResponse(ex.getMessage(), ex.getFieldErrors()));
    }

    /**
     * @PreAuthorize failures surface inside the controller, before ExceptionTranslationFilter sees them.
     * Mirror its behaviour: anonymous → 401 (go log in), authenticated → 403.
     */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> accessDenied() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || trustResolver.isAnonymous(authentication)) {
            return unauthenticated();
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse.of("Access denied"));
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErrorResponse> unauthenticated() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse.of("Authentication required"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.of("Unexpected error"));
    }
}
