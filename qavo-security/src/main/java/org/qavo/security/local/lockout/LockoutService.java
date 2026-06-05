/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.security.local.lockout;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.qavo.security.config.QavoSecurityProperties;
import org.qavo.security.local.domain.QavoUser;
import org.qavo.security.local.infrastructure.QavoUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Encapsulates the temporary-lockout policy for the local user store: counts consecutive failed
 * login attempts and, on reaching the configured threshold, sets a {@code locked_until} timestamp
 * that the {@link org.qavo.security.local.application.QavoUserDetailsService} reads to mark the
 * account locked. Successful authentications reset both counters.
 *
 * <p>The service is intentionally idempotent: callers pass usernames (not user ids) because the
 * username is the only datum available on Spring Security's authentication-failure events.
 * Missing usernames are silently ignored so the failure pipeline never throws.
 */
public class LockoutService {

    private static final Logger log = LoggerFactory.getLogger(LockoutService.class);

    private final QavoUserRepository userRepository;
    private final QavoSecurityProperties.Local.Lockout policy;
    private final Clock clock;

    public LockoutService(QavoUserRepository userRepository,
                          QavoSecurityProperties.Local.Lockout policy,
                          Clock clock) {
        this.userRepository = userRepository;
        this.policy = policy;
        this.clock = clock;
    }

    /**
     * Returns whether the user is currently within an active lock window. A lock that has
     * naturally expired is treated as not-locked so the caller can authenticate again.
     */
    public boolean isLocked(QavoUser user) {
        Instant lockedUntil = user.getLockedUntil();
        return lockedUntil != null && lockedUntil.isAfter(clock.instant());
    }

    /** Convenience for callers that only have a {@link Duration}; never negative. */
    public Duration remainingLock(QavoUser user) {
        Instant lockedUntil = user.getLockedUntil();
        if (lockedUntil == null) {
            return Duration.ZERO;
        }
        Duration remaining = Duration.between(clock.instant(), lockedUntil);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    /**
     * Records a failed login. Increments the counter and, when the configured threshold is
     * reached, applies a temporary lock. Unknown usernames are ignored to avoid leaking which
     * usernames exist via differential timing.
     */
    @Transactional
    public void recordFailure(String username) {
        if (!policy.isEnabled() || username == null || username.isBlank()) {
            return;
        }
        userRepository.findByUsername(username).ifPresent(user -> {
            // Don't keep incrementing once the account is already locked: the lock window itself
            // is the deterrent and a flood of bad attempts during the lock must not extend it.
            if (isLocked(user)) {
                return;
            }
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= policy.getMaxAttempts()) {
                Instant unlocksAt = clock.instant().plus(policy.getDuration());
                user.setLockedUntil(unlocksAt);
                log.warn("Locking local user '{}' until {} after {} failed attempts",
                        username, unlocksAt, attempts);
            }
            userRepository.save(user);
        });
    }

    /**
     * Records a successful login. Clears both the attempt counter and any active lock so the
     * user starts the next session with a clean slate.
     */
    @Transactional
    public void recordSuccess(String username) {
        if (!policy.isEnabled() || username == null || username.isBlank()) {
            return;
        }
        userRepository.findByUsername(username).ifPresent(user -> {
            if (user.getFailedLoginAttempts() != 0 || user.getLockedUntil() != null) {
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
                userRepository.save(user);
            }
        });
    }

    /**
     * Returns the active lock-expiry timestamp for a username, or empty if the account is not
     * currently locked. Used by the login controller to translate Spring Security's
     * {@code LockedException} into the platform's {@link AccountLockedException} carrying
     * machine-readable {@code unlocksAt}.
     */
    @Transactional(readOnly = true)
    public Optional<Instant> lookupUnlocksAt(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByUsername(username)
                .map(QavoUser::getLockedUntil)
                .filter(when -> when.isAfter(clock.instant()));
    }
}
