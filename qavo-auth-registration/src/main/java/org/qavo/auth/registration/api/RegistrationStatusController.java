/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.registration.api;

import java.time.Duration;

import org.qavo.core.api.ApiConventions;
import org.qavo.core.registration.RegistrationCapService;
import org.qavo.core.registration.RegistrationCapStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * Read-only endpoint that reports whether self-service registration is currently accepted.
 * Always returns HTTP 200: the semantic is carried in the response body via the {@code open}
 * flag. The endpoint is publicly accessible so a frontend can poll it before rendering the
 * registration form (ADR 0012). {@code Cache-Control: no-store} prevents intermediate caches
 * from serving a stale "closed" response after the cap reopens.
 */
@RestController
@RequestMapping(ApiConventions.AUTH_NAMESPACE)
public class RegistrationStatusController {

    private final RegistrationCapService capService;

    public RegistrationStatusController(RegistrationCapService capService) {
        this.capService = capService;
    }

    @GetMapping("/registration-status")
    @Operation(summary = "Check registration availability",
            description = "Returns whether self-service registration is currently accepted. "
                    + "Always returns HTTP 200 — the body's `open` flag carries the semantic. "
                    + "When `open=false`, the body also exposes `currentCount`, "
                    + "`maxRegistrations`, `windowDuration`, `opensAt`, and `retryAfter`. "
                    + "The response is uncacheable (`Cache-Control: no-store`) so the cap-open "
                    + "transition is observed by the next poll.")
    @ApiResponse(responseCode = "200", description = "Current cap status.",
            content = @Content(schema = @Schema(implementation = RegistrationStatusResponse.class)))
    public ResponseEntity<RegistrationStatusResponse> status() {
        RegistrationCapStatus status = capService.checkCap();
        RegistrationStatusResponse body;
        if (status.open()) {
            body = RegistrationStatusResponse.open(status.checkedAt());
        } else {
            long retryAfter = 0L;
            if (status.opensAt() != null && status.checkedAt() != null) {
                retryAfter = Math.max(0L,
                        Duration.between(status.checkedAt(), status.opensAt()).getSeconds());
            }
            body = new RegistrationStatusResponse(false,
                    status.currentCount(),
                    status.maxRegistrations(),
                    status.windowDuration(),
                    status.opensAt(),
                    retryAfter,
                    status.checkedAt());
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(body);
    }
}
