/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.login.api;

import jakarta.validation.Valid;

import org.qavo.auth.login.application.AuthenticationFailedException;
import org.qavo.core.api.ApiConventions;
import org.qavo.core.security.SecurityContextAccessor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Local login flow contributed by the login plugin, mounted under the reserved
 * {@code /api/v1/auth} namespace (architecture &sect;5.1, &sect;5.5).
 *
 * <p>The endpoint validates credentials against the local store via the platform's
 * {@link AuthenticationManager}. Issuing a signed bearer token is a planned hardening item
 * (see the roadmap); at this snapshot stage the endpoint confirms the credentials and returns the
 * resolved principal, demonstrating the plugin wiring and security integration end to end.
 */
@RestController
@RequestMapping(ApiConventions.AUTH_NAMESPACE)
public class LoginController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextAccessor securityContextAccessor;

    public LoginController(AuthenticationManager authenticationManager,
                           SecurityContextAccessor securityContextAccessor) {
        this.authenticationManager = authenticationManager;
        this.securityContextAccessor = securityContextAccessor;
    }

    @PostMapping("/login")
    public AuthenticatedUserView login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            return AuthenticatedUserViewFactory.from(authentication);
        } catch (AuthenticationException ex) {
            throw new AuthenticationFailedException();
        }
    }

    @GetMapping("/me")
    public ResponseEntity<AuthenticatedUserView> currentUser() {
        return securityContextAccessor.currentPrincipal()
                .map(AuthenticatedUserView::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(401).build());
    }
}
