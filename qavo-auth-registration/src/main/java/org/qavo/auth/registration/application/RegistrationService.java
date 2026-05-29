/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.registration.application;

import java.util.Set;
import java.util.UUID;

import org.qavo.auth.registration.api.RegisterUserRequest;
import org.qavo.auth.registration.config.QavoRegistrationProperties;
import org.qavo.core.domain.exception.BusinessException;
import org.qavo.core.domain.exception.ConflictException;
import org.qavo.security.local.domain.QavoRole;
import org.qavo.security.local.domain.QavoUser;
import org.qavo.security.local.infrastructure.QavoRoleRepository;
import org.qavo.security.local.infrastructure.QavoUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use-case orchestration for self-service registration (architecture &sect;4 application layer).
 * Creates a user in the local store with the configured default role, storing only a strong
 * password hash. Email-verification gating is honored via the {@code emailVerified} flag; sending
 * the verification email is a planned enhancement (see roadmap).
 */
public class RegistrationService {

    private final QavoUserRepository userRepository;
    private final QavoRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final QavoRegistrationProperties properties;

    public RegistrationService(QavoUserRepository userRepository,
                               QavoRoleRepository roleRepository,
                               PasswordEncoder passwordEncoder,
                               QavoRegistrationProperties properties) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Transactional
    public QavoUser register(RegisterUserRequest request) {
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

        QavoUser user = new QavoUser(
                UUID.randomUUID(),
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password()));
        user.setRoles(Set.of(role));
        user.setEmailVerified(!properties.isRequireEmailVerification());

        return userRepository.save(user);
    }
}
