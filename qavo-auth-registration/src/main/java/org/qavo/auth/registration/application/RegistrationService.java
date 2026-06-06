/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.registration.application;

import java.util.Set;
import java.util.UUID;

import org.qavo.auth.registration.api.RegisterUserRequest;
import org.qavo.auth.registration.config.QavoRegistrationProperties;
import org.qavo.core.domain.exception.BusinessException;
import org.qavo.core.domain.exception.ConflictException;
import org.qavo.core.registration.RegistrationCapExceededException;
import org.qavo.core.registration.RegistrationCapService;
import org.qavo.core.registration.RegistrationCapStatus;
import org.qavo.security.local.domain.QavoRole;
import org.qavo.security.local.domain.QavoUser;
import org.qavo.security.local.infrastructure.QavoRoleRepository;
import org.qavo.security.local.infrastructure.QavoUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use-case orchestration for self-service registration (architecture &sect;4 application layer).
 * Creates a user in the local store with the configured default role, storing only a strong
 * password hash. When {@code qavo.auth.registration.email-verification.enabled=true}, also
 * issues a single-use verification token and (best-effort) dispatches the verification email
 * via {@link EmailVerificationService} — a delivery failure is logged and does NOT roll back
 * the registration, so the end-user can still request a resend.
 *
 * <p>As of 0.0.3-SNAPSHOT the flow also consults {@link RegistrationCapService} before
 * accepting a request and records a registration event on success (ADR 0012). When the cap is
 * disabled the default {@code NoOpRegistrationCapService} short-circuits both calls.
 */
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final QavoUserRepository userRepository;
    private final QavoRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final QavoRegistrationProperties properties;
    private final EmailVerificationService emailVerificationService;
    private final RegistrationCapService capService;

    public RegistrationService(QavoUserRepository userRepository,
                               QavoRoleRepository roleRepository,
                               PasswordEncoder passwordEncoder,
                               QavoRegistrationProperties properties,
                               EmailVerificationService emailVerificationService,
                               RegistrationCapService capService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.emailVerificationService = emailVerificationService;
        this.capService = capService;
    }

    @Transactional
    public QavoUser register(RegisterUserRequest request) {
        // Step 1: cap check (ADR 0012). The no-op service always reports open=true when the
        // feature is disabled, so the legacy code path is unchanged for default deployments.
        RegistrationCapStatus capStatus = capService.checkCap();
        if (!capStatus.open()) {
            throw new RegistrationCapExceededException(capStatus);
        }
        if (!properties.isSelfService()) {
            throw new BusinessException("Self-service registration is currently closed");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email is already registered");
        }

        QavoRole role = roleRepository.findByName(properties.getDefaultRole())
                .orElseThrow(() -> new BusinessException(
                        "Default role '%s' is not provisioned".formatted(properties.getDefaultRole())));

        boolean emailVerificationActive = properties.getEmailVerification().isEnabled();
        QavoUser user = new QavoUser(
                UUID.randomUUID(),
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password()));
        user.setRoles(Set.of(role));
        // Email-verification flow leaves the flag false until the link is consumed; legacy
        // requireEmailVerification semantics remain intact for callers not opting into the
        // 0.0.2 verification feature.
        user.setEmailVerified(!emailVerificationActive && !properties.isRequireEmailVerification());

        QavoUser saved = userRepository.save(user);

        // Step 5: record the registration *after* the user is persisted. The cap service uses
        // REQUIRES_NEW so the audit row commits independently of this transaction.
        capService.recordRegistration(saved.getId().toString());

        if (emailVerificationActive && emailVerificationService != null) {
            try {
                emailVerificationService.issueFor(saved);
            } catch (RuntimeException ex) {
                // Honor the "do not roll back registration" contract: log and move on.
                log.warn("Verification email pipeline failed for user={}; user remains unverified",
                        saved.getId(), ex);
            }
        }
        return saved;
    }
}
