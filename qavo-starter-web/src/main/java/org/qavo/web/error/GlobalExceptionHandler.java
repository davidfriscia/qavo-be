/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.web.error;

import java.net.URI;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.qavo.core.api.error.CoreProblemType;
import org.qavo.core.api.error.FieldErrorDetail;
import org.qavo.core.api.error.ProblemDetailFactory;
import org.qavo.core.config.QavoProperties;
import org.qavo.core.domain.exception.QavoException;
import org.qavo.validation.mapping.ValidationErrorMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Single, centralized translation of every exception into the platform's RFC 9457 Problem Details
 * format (architecture &sect;5.2). Because all errors flow through here, every Qavo-based
 * application returns errors in an identical shape — including the {@code traceId} that links the
 * response to its log entries.
 *
 * <p>Extends Spring's {@link ResponseEntityExceptionHandler} so the framework's own exceptions
 * (malformed body, unsupported media type, etc.) are also rendered in the standard shape.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ProblemDetailFactory problemDetailFactory;
    private final ValidationErrorMapper validationErrorMapper;
    private final QavoProperties properties;

    public GlobalExceptionHandler(ProblemDetailFactory problemDetailFactory,
                                  ValidationErrorMapper validationErrorMapper,
                                  QavoProperties properties) {
        this.problemDetailFactory = problemDetailFactory;
        this.validationErrorMapper = validationErrorMapper;
        this.properties = properties;
    }

    /** Any exception in the platform hierarchy carries its own classification. */
    @ExceptionHandler(QavoException.class)
    public ResponseEntity<ProblemDetail> handleQavoException(QavoException ex, HttpServletRequest request) {
        ProblemDetail problem = problemDetailFactory.create(
                ex.getProblemType(), ex.getProblemType().defaultTitle(), ex.getMessage(), ex.getFieldErrors());
        problem.setInstance(URI.create(request.getRequestURI()));
        ex.getProblemProperties().forEach(problem::setProperty);
        if (ex.getProblemType().status().is5xxServerError()) {
            log.error("Server-side platform exception", ex);
        }
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(ex.getProblemType().status());
        ex.getResponseHeaders().forEach(builder::header);
        return builder.body(problem);
    }

    /** Method/parameter-level constraint violations (e.g. {@code @Validated} on a service). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex,
                                                                   HttpServletRequest request) {
        List<FieldErrorDetail> errors = validationErrorMapper.fromConstraintViolations(ex.getConstraintViolations());
        ProblemDetail problem = problemDetailFactory.create(
                CoreProblemType.VALIDATION, CoreProblemType.VALIDATION.defaultTitle(),
                "One or more fields are invalid", errors);
        problem.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.badRequest().body(problem);
    }

    /**
     * Authorization failure (e.g. from {@code @PreAuthorize}). Handled here so it is rendered in
     * the standard Problem Details shape rather than being swallowed by the catch-all below.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex,
                                                            HttpServletRequest request) {
        ProblemDetail problem = problemDetailFactory.create(
                CoreProblemType.FORBIDDEN, CoreProblemType.FORBIDDEN.defaultTitle(),
                "You do not have permission to perform this action", null);
        problem.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(CoreProblemType.FORBIDDEN.status()).body(problem);
    }

    /** Authentication failure surfaced during handler execution. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex,
                                                              HttpServletRequest request) {
        ProblemDetail problem = problemDetailFactory.create(
                CoreProblemType.UNAUTHORIZED, CoreProblemType.UNAUTHORIZED.defaultTitle(),
                "Authentication is required to access this resource", null);
        problem.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(CoreProblemType.UNAUTHORIZED.status()).body(problem);
    }

    /** Last-resort handler. Never leaks internal details unless explicitly enabled. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        String detail = properties.getError().isIncludeStackTraceDetail()
                ? ex.getMessage()
                : "An unexpected error occurred. Reference the traceId when reporting this problem.";
        ProblemDetail problem = problemDetailFactory.create(CoreProblemType.INTERNAL_ERROR, detail);
        problem.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.internalServerError().body(problem);
    }

    /** Renders Bean Validation failures on {@code @Valid @RequestBody} arguments in the standard shape. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<FieldErrorDetail> errors = validationErrorMapper.fromBindingResult(ex.getBindingResult());
        ProblemDetail problem = problemDetailFactory.create(
                CoreProblemType.VALIDATION, CoreProblemType.VALIDATION.defaultTitle(),
                "One or more fields are invalid", errors);
        setInstance(problem, request);
        return ResponseEntity.badRequest().body(problem);
    }

    /**
     * Central enrichment point: any Problem Details body produced by the framework's own handlers
     * (404, 415, malformed body, …) is given the platform's {@code traceId}/{@code timestamp}
     * extensions, so even framework errors honor the contract.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
                                                             HttpHeaders headers,
                                                             HttpStatusCode statusCode,
                                                             WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem) {
            problemDetailFactory.enrich(problem);
            setInstance(problem, request);
        }
        return response;
    }

    private void setInstance(ProblemDetail problem, WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            problem.setInstance(URI.create(servletWebRequest.getRequest().getRequestURI()));
        }
    }
}
