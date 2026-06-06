/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.registration.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

import org.qavo.auth.registration.application.EmailVerificationService;
import org.qavo.core.api.ApiConventions;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Email-verification HTTP surface mounted under the reserved {@code /api/v1/auth} namespace.
 *
 * <ul>
 *   <li>{@code GET /verify-email?token=...} — single-use consumption of the verification token.
 *       On success: 200 with a small JSON acknowledgment. On invalid/expired/used token: RFC
 *       9457 problem with the corresponding 400 type.</li>
 *   <li>{@code POST /verify-email/resend} — request a fresh verification email. Always
 *       responds 202 Accepted (anti-enumeration) unless the resend rate limit is exceeded, in
 *       which case the dedicated 429 RFC 9457 type is returned with {@code retryAfterSeconds}.
 *       </li>
 * </ul>
 */
@RestController
@RequestMapping(ApiConventions.AUTH_NAMESPACE)
public class EmailVerificationController {

    private final EmailVerificationService verificationService;

    public EmailVerificationController(EmailVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verify(@RequestParam("token") String token) {
        verificationService.verify(token);
        return ResponseEntity.ok(Map.of("status", "verified"));
    }

    @PostMapping("/verify-email/resend")
    public ResponseEntity<Void> resend(@Valid @RequestBody ResendRequest body) {
        verificationService.resendFor(body.email());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    /** Tiny request body for the resend endpoint. */
    public record ResendRequest(
            @NotBlank @Email
            String email) { }
}
