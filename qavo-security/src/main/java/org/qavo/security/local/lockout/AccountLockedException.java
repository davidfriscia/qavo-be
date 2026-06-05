/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.security.local.lockout;

import java.time.Instant;
import java.util.Map;

import org.qavo.core.api.error.CoreProblemType;
import org.qavo.core.domain.exception.QavoException;

/**
 * Raised when authentication is refused because the local account is temporarily locked after
 * repeated failed attempts. Renders as RFC 9457 Problem Details with HTTP 423 and an
 * {@code unlocksAt} extension property so clients can show an accurate retry-after countdown
 * without parsing a free-form message.
 */
public class AccountLockedException extends QavoException {

    private final Instant unlocksAt;

    public AccountLockedException(Instant unlocksAt) {
        super(CoreProblemType.ACCOUNT_LOCKED,
                "Account is temporarily locked until %s".formatted(unlocksAt));
        this.unlocksAt = unlocksAt;
    }

    public Instant getUnlocksAt() {
        return unlocksAt;
    }

    @Override
    public Map<String, Object> getProblemProperties() {
        return Map.of("unlocksAt", unlocksAt.toString());
    }
}
