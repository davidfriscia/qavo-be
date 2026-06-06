/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.registration.api;

import jakarta.validation.Valid;

import org.qavo.auth.registration.application.RegistrationService;
import org.qavo.core.api.ApiConventions;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * Self-service registration endpoint, mounted under the reserved {@code /api/v1/auth} namespace
 * (architecture &sect;5.1, &sect;6). Thin presentation layer: it validates the payload and delegates
 * to {@link RegistrationService}.
 */
@RestController
@RequestMapping(ApiConventions.AUTH_NAMESPACE)
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created."),
            @ApiResponse(responseCode = "400", description = "Validation error."),
            @ApiResponse(responseCode = "409", description = "Username or email already taken."),
            @ApiResponse(responseCode = "503",
                    description = "Registration capacity cap reached. The RFC 9457 Problem "
                            + "Details body carries `opensAt` (ISO-8601 UTC) and `retryAfter` "
                            + "(seconds) extensions; the standard `Retry-After` HTTP header is "
                            + "set to the same value. See ADR 0012.")
    })
    public RegisteredUserView register(@Valid @RequestBody RegisterUserRequest request) {
        return RegisteredUserView.from(registrationService.register(request));
    }
}
