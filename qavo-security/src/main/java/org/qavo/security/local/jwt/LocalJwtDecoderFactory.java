/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.security.local.jwt;

import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.qavo.security.config.QavoSecurityProperties;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Builds the {@link JwtDecoder} used to validate access tokens that the platform issued itself.
 * Symmetric HMAC-SHA256 with the configured Base64-encoded secret, plus issuer and audience
 * validation so a token from a different deployment is rejected even if the same key were
 * accidentally shared.
 *
 * <p>Kept as a small dedicated factory (rather than a one-liner inside the auto-configuration)
 * to keep the validation policy testable in isolation.
 */
public final class LocalJwtDecoderFactory {

    /** Minimum key length for HMAC-SHA256 (RFC 7518 §3.2). */
    public static final int MIN_KEY_LENGTH_BYTES = 32;

    /** Symmetric HMAC algorithm used for locally issued tokens. */
    public static final String HMAC_ALGORITHM = "HmacSHA256";

    private LocalJwtDecoderFactory() {
    }

    public static JwtDecoder build(QavoSecurityProperties.Local.Jwt properties) {
        SecretKey key = decodeKey(properties.getSecret());
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).build();
        decoder.setJwtValidator(buildValidator(properties));
        return decoder;
    }

    static SecretKey decodeKey(String base64Secret) {
        if (base64Secret == null || base64Secret.isBlank()) {
            throw new IllegalStateException(
                    "qavo.security.local.jwt.secret must be set to a Base64-encoded HMAC-SHA256 key");
        }
        byte[] bytes = Base64.getDecoder().decode(base64Secret);
        if (bytes.length < MIN_KEY_LENGTH_BYTES) {
            throw new IllegalStateException("qavo.security.local.jwt.secret must decode to at least "
                    + MIN_KEY_LENGTH_BYTES + " bytes (HMAC-SHA256). Got " + bytes.length + " bytes.");
        }
        return new SecretKeySpec(bytes, HMAC_ALGORITHM);
    }

    private static OAuth2TokenValidator<Jwt> buildValidator(QavoSecurityProperties.Local.Jwt properties) {
        OAuth2TokenValidator<Jwt> defaults = JwtValidators.createDefaultWithIssuer(properties.getIssuer());
        String expectedAudience = properties.getAudience();
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> {
            if (expectedAudience == null || expectedAudience.isBlank()) {
                return OAuth2TokenValidatorResult.success();
            }
            if (jwt.getAudience() != null && jwt.getAudience().contains(expectedAudience)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new org.springframework.security.oauth2.core.OAuth2Error(
                            "invalid_token",
                            "Token audience does not match expected value '" + expectedAudience + "'",
                            null));
        };
        return new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                defaults, audienceValidator);
    }
}
