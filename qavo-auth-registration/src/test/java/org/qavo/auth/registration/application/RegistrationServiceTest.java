/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.registration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qavo.auth.registration.api.RegisterUserRequest;
import org.qavo.auth.registration.config.QavoRegistrationProperties;
import org.qavo.security.local.domain.QavoRole;
import org.qavo.security.local.domain.QavoUser;
import org.qavo.security.local.infrastructure.QavoRoleRepository;
import org.qavo.security.local.infrastructure.QavoUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Focused tests for the registration ↔ verification handoff: when email verification is
 * enabled, registration must call {@link EmailVerificationService#issueFor(QavoUser)} and a
 * dispatch failure must NOT roll back the user creation; when verification is disabled, the
 * service must not be invoked at all and the verified flag falls back to the legacy
 * {@code requireEmailVerification} semantics.
 */
class RegistrationServiceTest {

    private QavoUserRepository userRepo;
    private QavoRoleRepository roleRepo;
    private PasswordEncoder encoder;
    private QavoRegistrationProperties properties;
    private EmailVerificationService verificationService;
    private RegistrationService service;

    @BeforeEach
    void setUp() {
        userRepo = mock(QavoUserRepository.class);
        roleRepo = mock(QavoRoleRepository.class);
        encoder = mock(PasswordEncoder.class);
        verificationService = mock(EmailVerificationService.class);
        properties = new QavoRegistrationProperties();
        properties.setDefaultRole("USER");
        when(userRepo.existsByUsername(any())).thenReturn(false);
        when(userRepo.existsByEmail(any())).thenReturn(false);
        when(roleRepo.findByName("USER"))
                .thenReturn(Optional.of(new QavoRole("USER", Set.of("user:read"))));
        when(encoder.encode(any())).thenReturn("hashed");
        when(userRepo.save(any(QavoUser.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        service = new RegistrationService(userRepo, roleRepo, encoder, properties, verificationService);
    }

    @Test
    void verificationDisabledLeavesUserVerifiedAndDoesNotInvokeVerificationService() {
        properties.getEmailVerification().setEnabled(false);
        properties.setRequireEmailVerification(false);

        QavoUser saved = service.register(new RegisterUserRequest("alice", "alice@example.com",
                "Sup3rStrong#PWord1!"));

        assertThat(saved.isEmailVerified()).isTrue();
        verify(verificationService, never()).issueFor(any());
    }

    @Test
    void verificationEnabledIssuesTokenAndLeavesUserUnverified() {
        properties.getEmailVerification().setEnabled(true);

        QavoUser saved = service.register(new RegisterUserRequest("alice", "alice@example.com",
                "Sup3rStrong#PWord1!"));

        assertThat(saved.isEmailVerified()).isFalse();
        verify(verificationService, times(1)).issueFor(saved);
    }

    @Test
    void registrationStillSucceedsWhenVerificationDispatchFails() {
        properties.getEmailVerification().setEnabled(true);
        org.mockito.Mockito.doThrow(new RuntimeException("smtp down"))
                .when(verificationService).issueFor(any());

        QavoUser saved = service.register(new RegisterUserRequest("alice", "alice@example.com",
                "Sup3rStrong#PWord1!"));

        // The contract is: a dispatch failure logs WARN but does NOT roll back the registration.
        assertThat(saved).isNotNull();
        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.isEmailVerified()).isFalse();
        verify(userRepo, times(1)).save(any(QavoUser.class));
    }
}
