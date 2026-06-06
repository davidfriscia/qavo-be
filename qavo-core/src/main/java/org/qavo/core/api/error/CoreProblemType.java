/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.api.error;

import org.springframework.http.HttpStatus;

/**
 * The baseline set of {@link ProblemType}s recognized by the platform.
 *
 * <p>These cover the conditions every application encounters. Domain-specific problems should
 * be expressed as application-defined {@link ProblemType} implementations rather than being
 * added here, keeping the core stable.
 */
public enum CoreProblemType implements ProblemType {

    VALIDATION("validation", "Validation error", HttpStatus.BAD_REQUEST),
    MALFORMED_REQUEST("malformed-request", "Malformed request", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND("resource-not-found", "Resource not found", HttpStatus.NOT_FOUND),
    CONFLICT("conflict", "Conflict", HttpStatus.CONFLICT),
    BUSINESS_RULE("business-rule", "Business rule violation", HttpStatus.UNPROCESSABLE_ENTITY),
    UNAUTHORIZED("unauthorized", "Authentication required", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("forbidden", "Access denied", HttpStatus.FORBIDDEN),
    ACCOUNT_LOCKED("account-locked", "Account temporarily locked", HttpStatus.LOCKED),
    EMAIL_NOT_VERIFIED("email-not-verified", "Email address not verified", HttpStatus.FORBIDDEN),
    INVALID_VERIFICATION_TOKEN("invalid-verification-token", "Invalid verification token", HttpStatus.BAD_REQUEST),
    VERIFICATION_TOKEN_EXPIRED("verification-token-expired", "Verification token expired", HttpStatus.BAD_REQUEST),
    VERIFICATION_TOKEN_ALREADY_USED("verification-token-already-used", "Verification token already used", HttpStatus.BAD_REQUEST),
    RESEND_RATE_LIMITED("resend-rate-limited", "Too many resend attempts", HttpStatus.TOO_MANY_REQUESTS),
    REGISTRATION_CAP_EXCEEDED("registration-cap-exceeded", "Registration Temporarily Unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    METHOD_NOT_ALLOWED("method-not-allowed", "Method not allowed", HttpStatus.METHOD_NOT_ALLOWED),
    UNSUPPORTED_MEDIA_TYPE("unsupported-media-type", "Unsupported media type", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    TOO_MANY_REQUESTS("too-many-requests", "Too many requests", HttpStatus.TOO_MANY_REQUESTS),
    INTERNAL_ERROR("internal-error", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE("service-unavailable", "Service temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE);

    private final String code;
    private final String defaultTitle;
    private final HttpStatus status;

    CoreProblemType(String code, String defaultTitle, HttpStatus status) {
        this.code = code;
        this.defaultTitle = defaultTitle;
        this.status = status;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String defaultTitle() {
        return defaultTitle;
    }

    @Override
    public HttpStatus status() {
        return status;
    }
}
