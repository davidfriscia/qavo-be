/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.security.autoconfigure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.qavo.security.config.QavoSecurityProperties;
import org.qavo.security.web.HttpSecurityCustomizer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * Wiring for hybrid authentication, where both the local issuer and an external OIDC provider
 * mint tokens that must be honored (architecture &sect;5.5). Activates only when the strategy is
 * {@code hybrid} and both the local and external decoder beans are present. Composes them into a
 * single {@link JwtDecoder} that selects the appropriate underlying decoder by inspecting the
 * unverified {@code iss} claim, and contributes a single {@link HttpSecurityCustomizer} so the
 * resource-server filter chain is configured exactly once.
 */
@AutoConfiguration(after = {QavoLocalJwtAuthAutoConfiguration.class, QavoOidcAuthAutoConfiguration.class})
@ConditionalOnExpression("'${qavo.security.strategy:local}'.toLowerCase() == 'hybrid'")
@ConditionalOnBean(name = {
        QavoLocalJwtAuthAutoConfiguration.LOCAL_JWT_DECODER_BEAN,
        QavoOidcAuthAutoConfiguration.EXTERNAL_JWT_DECODER_BEAN})
public class QavoHybridJwtAuthAutoConfiguration {

    @Bean
    public JwtDecoder qavoCompositeJwtDecoder(
            @Qualifier(QavoLocalJwtAuthAutoConfiguration.LOCAL_JWT_DECODER_BEAN) JwtDecoder local,
            @Qualifier(QavoOidcAuthAutoConfiguration.EXTERNAL_JWT_DECODER_BEAN) JwtDecoder external,
            QavoSecurityProperties properties) {
        String localIssuer = properties.getLocal().getJwt().getIssuer();
        String externalIssuer = properties.getOidc().getIssuerUri();
        return new CompositeIssuerJwtDecoder(local, external, localIssuer, externalIssuer);
    }

    @Bean
    public HttpSecurityCustomizer qavoHybridResourceServerCustomizer(
            JwtDecoder qavoCompositeJwtDecoder, QavoSecurityProperties properties) {
        // Hybrid is intentionally permissive on the authorities side: it merges the role claims
        // configured for the local and external issuers so a token from either side maps to the
        // same authority shape downstream.
        return http -> http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                .decoder(qavoCompositeJwtDecoder)
                .jwtAuthenticationConverter(buildAuthenticationConverter(properties))));
    }

    private JwtAuthenticationConverter buildAuthenticationConverter(QavoSecurityProperties properties) {
        QavoSecurityProperties.Local.Jwt localJwt = properties.getLocal().getJwt();
        QavoSecurityProperties.Oidc oidc = properties.getOidc();
        Converter<Jwt, Collection<GrantedAuthority>> authoritiesConverter = token -> {
            List<GrantedAuthority> authorities = new ArrayList<>();
            collect(token, localJwt.getAuthoritiesClaim(), localJwt.getAuthorityPrefix(), authorities);
            collect(token, oidc.getAuthoritiesClaim(), oidc.getAuthorityPrefix(), authorities);
            return authorities;
        };
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    private static void collect(Jwt token, String claimName, String prefix,
                                List<GrantedAuthority> sink) {
        Object claim = token.getClaims().get(claimName);
        if (claim instanceof Collection<?> values) {
            for (Object value : values) {
                sink.add(new SimpleGrantedAuthority(prefix + value));
            }
        }
    }

    /**
     * Selects the underlying decoder by matching the unverified {@code iss} claim in the token
     * against the configured local and external issuers. The chosen decoder still performs full
     * cryptographic and policy validation; this composite only routes.
     */
    static final class CompositeIssuerJwtDecoder implements JwtDecoder {

        private final JwtDecoder local;
        private final JwtDecoder external;
        private final String localIssuer;
        private final String externalIssuer;

        CompositeIssuerJwtDecoder(JwtDecoder local, JwtDecoder external,
                                  String localIssuer, String externalIssuer) {
            this.local = local;
            this.external = external;
            this.localIssuer = localIssuer;
            this.externalIssuer = externalIssuer;
        }

        @Override
        public Jwt decode(String token) throws JwtException {
            String issuer = peekIssuer(token);
            if (issuer == null) {
                throw new BadJwtException("Token is missing the 'iss' claim");
            }
            if (issuer.equals(localIssuer)) {
                return local.decode(token);
            }
            if (issuer.equals(externalIssuer)) {
                return external.decode(token);
            }
            throw new BadJwtException("Unrecognized token issuer '" + issuer + "'");
        }

        private static String peekIssuer(String token) {
            // The JWT payload is the second Base64URL-encoded segment. We only parse it here to
            // route to the right decoder; full validation (including issuer) is then performed by
            // the chosen decoder.
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            try {
                String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]),
                        java.nio.charset.StandardCharsets.UTF_8);
                int idx = payloadJson.indexOf("\"iss\"");
                if (idx < 0) {
                    return null;
                }
                int colon = payloadJson.indexOf(':', idx);
                int firstQuote = payloadJson.indexOf('"', colon + 1);
                int secondQuote = payloadJson.indexOf('"', firstQuote + 1);
                if (firstQuote < 0 || secondQuote < 0) {
                    return null;
                }
                return payloadJson.substring(firstQuote + 1, secondQuote);
            } catch (RuntimeException ex) {
                return null;
            }
        }
    }
}
