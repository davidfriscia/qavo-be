/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.registration.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Behavior of the registration plugin under {@code qavo.auth.registration.*} (architecture
 * &sect;6.1). Once the plugin is imported, these flags tune its runtime behavior — for example
 * whether self-service sign-up is currently open and whether email verification is required.
 */
@ConfigurationProperties(prefix = "qavo.auth.registration")
public class QavoRegistrationProperties {

    /** Whether the registration plugin is active. */
    private boolean enabled = true;

    /** Whether self-service (public) registration is currently open. */
    private boolean selfService = true;

    /** Whether newly registered accounts must verify their email before becoming active. */
    private boolean requireEmailVerification = false;

    /** Role granted to newly registered users. */
    private String defaultRole = "USER";

    @NestedConfigurationProperty
    private final EmailVerification emailVerification = new EmailVerification();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSelfService() {
        return selfService;
    }

    public void setSelfService(boolean selfService) {
        this.selfService = selfService;
    }

    public boolean isRequireEmailVerification() {
        return requireEmailVerification;
    }

    public void setRequireEmailVerification(boolean requireEmailVerification) {
        this.requireEmailVerification = requireEmailVerification;
    }

    public String getDefaultRole() {
        return defaultRole;
    }

    public void setDefaultRole(String defaultRole) {
        this.defaultRole = defaultRole;
    }

    public EmailVerification getEmailVerification() {
        return emailVerification;
    }

    /**
     * Sub-block bound to {@code qavo.auth.registration.email-verification.*}. When
     * {@code enabled=true}, registration emits a verification email through the platform's
     * {@link org.qavo.core.notifications.NotificationDispatcher}.
     */
    public static class EmailVerification {
        /** Master switch for the email-verification feature. */
        private boolean enabled = false;

        /**
         * Public base URL applications expose so the link in the verification email lands on a
         * front-end (or directly on the GET verify endpoint). The full link sent is
         * {@code {baseUrl}/api/v1/auth/verify-email?token={rawToken}}. Required when
         * {@code enabled=true}.
         */
        private String baseUrl;

        /** Email subject line. */
        private String subject = "Please verify your email address";

        /** Validity window for newly issued verification tokens. */
        private Duration tokenDuration = Duration.ofHours(24);

        /**
         * When {@code true}, the login plugin rejects credential authentication for users whose
         * {@code email_verified=false} with a 403 RFC 9457 error
         * ({@link org.qavo.core.api.error.CoreProblemType#EMAIL_NOT_VERIFIED}).
         */
        private boolean requireVerifiedEmailToLogin = false;

        /** Maximum verification emails an end-user can request per rolling hour. */
        private int resendMaxPerHour = 3;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public Duration getTokenDuration() {
            return tokenDuration;
        }

        public void setTokenDuration(Duration tokenDuration) {
            this.tokenDuration = tokenDuration;
        }

        public boolean isRequireVerifiedEmailToLogin() {
            return requireVerifiedEmailToLogin;
        }

        public void setRequireVerifiedEmailToLogin(boolean requireVerifiedEmailToLogin) {
            this.requireVerifiedEmailToLogin = requireVerifiedEmailToLogin;
        }

        public int getResendMaxPerHour() {
            return resendMaxPerHour;
        }

        public void setResendMaxPerHour(int resendMaxPerHour) {
            this.resendMaxPerHour = resendMaxPerHour;
        }
    }
}
