/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.security.autoconfigure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.qavo.security.config.QavoSecurityProperties;
import org.qavo.security.web.HttpSecurityCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * OIDC / OAuth2 resource-server extension point (architecture &sect;5.5). Activates only when the
 * OAuth2 resource-server dependency is present and the strategy includes OIDC. It contributes a
 * {@link JwtDecoder} built from the configured issuer and an {@link HttpSecurityCustomizer} that
 * enables JWT-based resource-server protection on the shared filter chain — so the secure-by-default
 * baseline is reused and only the token-validation concern is added here.
 *
 * <p>The application supplies provider-specific values ({@code qavo.security.oidc.issuer-uri},
 * claim name, prefix); token validation (signature, expiry, issuer) and claim/role mapping are
 * handled by the platform.
 */
@AutoConfiguration
@ConditionalOnClass(JwtDecoder.class)
@ConditionalOnExpression("'${qavo.security.strategy:local}'.toLowerCase() == 'oidc' "
        + "or '${qavo.security.strategy:local}'.toLowerCase() == 'hybrid'")
@ConditionalOnProperty(prefix = "qavo.security.oidc", name = "issuer-uri")
public class QavoOidcAuthAutoConfiguration {

    /** Qualifier used to look up the external/OIDC decoder when a composite is built. */
    public static final String EXTERNAL_JWT_DECODER_BEAN = "qavoExternalJwtDecoder";

    @Bean(EXTERNAL_JWT_DECODER_BEAN)
    @ConditionalOnMissingBean(name = EXTERNAL_JWT_DECODER_BEAN)
    public JwtDecoder qavoExternalJwtDecoder(QavoSecurityProperties properties) {
        String jwkSetUri = properties.getOidc().getJwkSetUri();
        if (jwkSetUri != null && !jwkSetUri.isBlank()) {
            return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        }
        return JwtDecoders.fromIssuer(properties.getOidc().getIssuerUri());
    }

    /**
     * Wires the OAuth2 resource server with the OIDC decoder. Active only when the strategy is
     * purely OIDC; the hybrid path is wired by {@link QavoHybridJwtAuthAutoConfiguration} with a
     * composite decoder so {@code oauth2ResourceServer} is configured exactly once.
     */
    @Bean
    @ConditionalOnExpression("'${qavo.security.strategy:local}'.toLowerCase() == 'oidc'")
    public HttpSecurityCustomizer qavoOidcResourceServerCustomizer(
            QavoSecurityProperties properties, JwtDecoder qavoExternalJwtDecoder) {
        JwtAuthenticationConverter authenticationConverter =
                buildAuthenticationConverter(properties.getOidc());
        return http -> http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                .decoder(qavoExternalJwtDecoder)
                .jwtAuthenticationConverter(authenticationConverter)));
    }

    private JwtAuthenticationConverter buildAuthenticationConverter(QavoSecurityProperties.Oidc oidc) {
        Converter<Jwt, Collection<GrantedAuthority>> authoritiesConverter = jwt -> {
            List<GrantedAuthority> authorities = new ArrayList<>();
            Object claim = jwt.getClaims().get(oidc.getAuthoritiesClaim());
            if (claim instanceof Collection<?> values) {
                for (Object value : values) {
                    authorities.add(new SimpleGrantedAuthority(oidc.getAuthorityPrefix() + value));
                }
            }
            return authorities;
        };
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    /** Small indirection so the issuer-location call is isolated and easy to stub in tests. */
    static final class JwtDecoders {
        private JwtDecoders() {
        }

        static JwtDecoder fromIssuer(String issuerUri) {
            return org.springframework.security.oauth2.jwt.JwtDecoders.fromIssuerLocation(issuerUri);
        }
    }
}
