/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
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
    public RegisteredUserView register(@Valid @RequestBody RegisterUserRequest request) {
        return RegisteredUserView.from(registrationService.register(request));
    }
}
