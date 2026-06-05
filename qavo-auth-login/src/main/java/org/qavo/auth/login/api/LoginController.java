/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.login.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.time.Duration;
import java.time.Instant;

import org.qavo.auth.login.application.AuthenticationFailedException;
import org.qavo.auth.login.jwt.IssuedTokens;
import org.qavo.auth.login.jwt.TokenService;
import org.qavo.core.api.ApiConventions;
import org.qavo.core.security.AuthenticatedPrincipal;
import org.qavo.core.security.SecurityContextAccessor;
import org.qavo.security.local.lockout.AccountLockedException;
import org.qavo.security.local.lockout.LockoutService;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.LockedException;
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
 * <p>The login endpoint validates credentials through the platform's
 * {@link AuthenticationManager} and, on success, issues a signed JWT access token and a rotating
 * refresh token via the {@link TokenService}. The refresh endpoint exchanges a valid refresh
 * token for a fresh pair (atomically invalidating the presented one). The logout endpoint
 * revokes every active refresh token of the current principal. The {@code /me} endpoint reports
 * the principal carried by the request, regardless of whether authentication was performed via
 * the bearer token issued here or by another mechanism.
 */
@RestController
@RequestMapping(ApiConventions.AUTH_NAMESPACE)
@Tag(name = "Authentication", description = "Local login, refresh and logout under " + ApiConventions.AUTH_NAMESPACE)
public class LoginController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextAccessor securityContextAccessor;
    private final TokenService tokenService;
    private final LockoutService lockoutService;

    public LoginController(AuthenticationManager authenticationManager,
                           SecurityContextAccessor securityContextAccessor,
                           TokenService tokenService,
                           LockoutService lockoutService) {
        this.authenticationManager = authenticationManager;
        this.securityContextAccessor = securityContextAccessor;
        this.tokenService = tokenService;
        this.lockoutService = lockoutService;
    }

    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Authenticate with local credentials and obtain a bearer token pair",
            description = "Validates the supplied username/password against the local user store"
                    + " and, on success, returns a signed JWT access token and a rotating refresh token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credentials accepted",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Malformed or missing fields",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Authentication failed",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "423", description = "Account temporarily locked",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            AuthenticatedUserView user = AuthenticatedUserViewFactory.from(authentication);
            AuthenticatedPrincipal principal = new ViewPrincipal(user);
            IssuedTokens tokens = tokenService.issueFor(principal);
            return LoginResponse.of(
                    tokens.accessToken(),
                    secondsUntil(tokens.accessTokenExpiresAt()),
                    tokens.refreshToken(),
                    user);
        } catch (LockedException ex) {
            // Look up the live unlock timestamp so the client can render an accurate countdown.
            // If the lock has just been cleared between events, fall back to UNAUTHORIZED.
            Instant unlocksAt = lockoutService.lookupUnlocksAt(request.username())
                    .orElseThrow(AuthenticationFailedException::new);
            throw new AccountLockedException(unlocksAt);
        } catch (AuthenticationException ex) {
            throw new AuthenticationFailedException();
        }
    }

    @PostMapping(value = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Rotate a refresh token into a fresh access/refresh pair",
            description = "Atomically revokes the presented refresh token and returns a new pair."
                    + " Reuse of a revoked or expired token results in 401.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens rotated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Malformed or missing fields",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Refresh token invalid, expired, or revoked",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public LoginResponse refresh(@Valid @RequestBody RefreshRequest request) {
        IssuedTokens tokens = tokenService.refresh(request.refreshToken());
        AuthenticatedUserView user = securityContextAccessor.currentPrincipal()
                .map(AuthenticatedUserView::from)
                .orElse(null);
        return LoginResponse.of(
                tokens.accessToken(),
                secondsUntil(tokens.accessTokenExpiresAt()),
                tokens.refreshToken(),
                user);
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke every active refresh token of the current principal",
            description = "Marks all active refresh tokens of the authenticated user as revoked."
                    + " Issued access tokens remain valid until they expire by design (stateless JWTs).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Refresh tokens revoked"),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> logout() {
        securityContextAccessor.currentPrincipal().ifPresent(tokenService::revokeAllFor);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Return the current authenticated principal")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated principal view",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthenticatedUserView.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AuthenticatedUserView> currentUser() {
        return securityContextAccessor.currentPrincipal()
                .map(AuthenticatedUserView::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(401).build());
    }

    private static long secondsUntil(Instant expiry) {
        long seconds = Duration.between(Instant.now(), expiry).getSeconds();
        return Math.max(seconds, 0);
    }

    /**
     * Bridges the wire view back to an {@link AuthenticatedPrincipal} so the {@link TokenService}
     * sees a uniform shape regardless of the underlying authentication mechanism.
     */
    private record ViewPrincipal(AuthenticatedUserView view) implements AuthenticatedPrincipal {
        @Override public String id() { return view.id(); }
        @Override public String username() { return view.username(); }
        @Override public java.util.Set<String> roles() { return view.roles(); }
        @Override public java.util.Set<String> permissions() { return view.permissions(); }
        @Override public java.util.Map<String, Object> attributes() { return java.util.Map.of(); }
    }
}
