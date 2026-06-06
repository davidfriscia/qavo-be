/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.registration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.qavo.auth.registration.config.QavoRegistrationProperties;
import org.qavo.auth.registration.domain.QavoEmailVerificationToken;
import org.qavo.auth.registration.infrastructure.QavoEmailVerificationTokenRepository;
import org.qavo.core.notifications.NotificationDispatcher;
import org.qavo.core.notifications.NotificationRequest;
import org.qavo.core.notifications.NotificationResult;
import org.qavo.security.local.domain.QavoUser;
import org.qavo.security.local.infrastructure.QavoUserRepository;

/**
 * Unit-level coverage of the email-verification lifecycle: issuing a token persists a hashed row
 * and dispatches an email containing the raw token; verifying mutates both rows transactionally;
 * the well-known failure modes (unknown / used / expired) each raise a distinct typed exception
 * so the global exception handler can map them to RFC 9457; and the resend ceiling is enforced
 * against a deterministic {@link Clock}.
 */
class EmailVerificationServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String EMAIL = "alice@example.com";

    private QavoEmailVerificationTokenRepository tokenRepo;
    private QavoUserRepository userRepo;
    private NotificationDispatcher dispatcher;
    private QavoRegistrationProperties properties;
    private MutableClock clock;
    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        tokenRepo = mock(QavoEmailVerificationTokenRepository.class);
        userRepo = mock(QavoUserRepository.class);
        dispatcher = mock(NotificationDispatcher.class);
        when(dispatcher.dispatch(any(NotificationRequest.class)))
                .thenReturn(NotificationResult.success("provider-id"));
        when(tokenRepo.save(any(QavoEmailVerificationToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.save(any(QavoUser.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        properties = new QavoRegistrationProperties();
        properties.getEmailVerification().setEnabled(true);
        properties.getEmailVerification().setBaseUrl("https://app.example.com");
        properties.getEmailVerification().setTokenDuration(Duration.ofHours(2));
        properties.getEmailVerification().setResendMaxPerHour(2);
        clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        service = new EmailVerificationService(tokenRepo, userRepo, dispatcher, properties, clock);
    }

    @Test
    void issueForPersistsHashedTokenAndDispatchesEmailContainingTheRawToken() {
        QavoUser user = newUser();

        String rawToken = service.issueFor(user);

        // The persisted row contains the SHA-256 digest, not the raw token.
        ArgumentCaptor<QavoEmailVerificationToken> savedRow =
                ArgumentCaptor.forClass(QavoEmailVerificationToken.class);
        verify(tokenRepo).save(savedRow.capture());
        assertThat(savedRow.getValue().getTokenHash())
                .isEqualTo(EmailVerificationService.sha256Hex(rawToken));
        assertThat(savedRow.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(savedRow.getValue().getExpiresAt())
                .isEqualTo(clock.instant().plus(Duration.ofHours(2)));
        assertThat(savedRow.getValue().isConsumed()).isFalse();

        // The email body contains a fully-formed verification URL with the RAW token.
        ArgumentCaptor<NotificationRequest> sent = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(dispatcher).dispatch(sent.capture());
        assertThat(sent.getValue().recipient()).isEqualTo(EMAIL);
        assertThat(sent.getValue().body())
                .contains("https://app.example.com/api/v1/auth/verify-email?token=" + rawToken);
    }

    @Test
    void issueForSkipsDispatchWhenBaseUrlIsBlank() {
        properties.getEmailVerification().setBaseUrl("  ");
        QavoUser user = newUser();

        service.issueFor(user);

        verify(tokenRepo, times(1)).save(any());
        verify(dispatcher, never()).dispatch(any());
    }

    @Test
    void verifyFlipsUserAndMarksTokenConsumed() {
        String raw = "raw-token";
        QavoEmailVerificationToken row = new QavoEmailVerificationToken(
                EmailVerificationService.sha256Hex(raw),
                USER_ID,
                clock.instant().plus(Duration.ofMinutes(30)),
                clock.instant());
        when(tokenRepo.findById(row.getTokenHash())).thenReturn(Optional.of(row));
        QavoUser user = newUser();
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));

        service.verify(raw);

        assertThat(row.isConsumed()).isTrue();
        assertThat(user.isEmailVerified()).isTrue();
    }

    @Test
    void verifyRejectsUnknownToken() {
        when(tokenRepo.findById(EmailVerificationService.sha256Hex("ghost")))
                .thenReturn(Optional.empty());

        assertThatExceptionOfType(InvalidVerificationTokenException.class)
                .isThrownBy(() -> service.verify("ghost"));
    }

    @Test
    void verifyRejectsAlreadyUsedToken() {
        String raw = "raw";
        QavoEmailVerificationToken row = new QavoEmailVerificationToken(
                EmailVerificationService.sha256Hex(raw), USER_ID,
                clock.instant().plus(Duration.ofMinutes(30)), clock.instant());
        row.consume();
        when(tokenRepo.findById(row.getTokenHash())).thenReturn(Optional.of(row));

        assertThatExceptionOfType(VerificationTokenAlreadyUsedException.class)
                .isThrownBy(() -> service.verify(raw));
    }

    @Test
    void verifyRejectsExpiredToken() {
        String raw = "raw";
        QavoEmailVerificationToken row = new QavoEmailVerificationToken(
                EmailVerificationService.sha256Hex(raw), USER_ID,
                clock.instant().minusSeconds(1), clock.instant().minus(Duration.ofHours(3)));
        when(tokenRepo.findById(row.getTokenHash())).thenReturn(Optional.of(row));

        assertThatExceptionOfType(VerificationTokenExpiredException.class)
                .isThrownBy(() -> service.verify(raw));
    }

    @Test
    void resendIsSilentForUnknownEmail() {
        when(userRepo.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        service.resendFor("ghost@example.com");

        verify(tokenRepo, never()).save(any());
        verify(dispatcher, never()).dispatch(any());
    }

    @Test
    void resendIsSilentForAlreadyVerifiedUser() {
        QavoUser user = newUser();
        user.setEmailVerified(true);
        when(userRepo.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        service.resendFor(EMAIL);

        verify(tokenRepo, never()).save(any());
        verify(dispatcher, never()).dispatch(any());
    }

    @Test
    void resendThrowsWhenRateLimitExceeded() {
        QavoUser user = newUser();
        when(userRepo.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(tokenRepo.countByUserIdAndCreatedAtGreaterThanEqual(eq(USER_ID), any(Instant.class)))
                .thenReturn(2L); // configured ceiling is 2/hour

        assertThatExceptionOfType(ResendRateLimitedException.class)
                .isThrownBy(() -> service.resendFor(EMAIL))
                .satisfies(ex -> assertThat(ex.getProblemProperties())
                        .containsEntry("retryAfterSeconds", Duration.ofHours(1).toSeconds()));
        verify(tokenRepo, never()).save(any());
    }

    @Test
    void resendIssuesAndDispatchesWhenUnderCeiling() {
        QavoUser user = newUser();
        when(userRepo.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(tokenRepo.countByUserIdAndCreatedAtGreaterThanEqual(eq(USER_ID), any(Instant.class)))
                .thenReturn(1L);

        service.resendFor(EMAIL);

        verify(tokenRepo).save(any(QavoEmailVerificationToken.class));
        verify(dispatcher).dispatch(any(NotificationRequest.class));
    }

    private QavoUser newUser() {
        // QavoUser's constructor leaves emailVerified=false (no field initializer) so the
        // freshly built user is the right starting state for verify().
        return new QavoUser(USER_ID, "alice", EMAIL, "{noop}whatever");
    }

    /** Tiny mutable clock so tests can express deterministic time without sleeping. */
    private static final class MutableClock extends Clock {
        private Instant now;
        MutableClock(Instant initial) { this.now = initial; }
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
