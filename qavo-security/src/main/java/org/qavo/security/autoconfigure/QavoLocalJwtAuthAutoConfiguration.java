/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.security.autoconfigure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.qavo.security.config.QavoSecurityProperties;
import org.qavo.security.local.jwt.LocalJwtDecoderFactory;
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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * Wires the resource-server side of the local JWT bearer flow (architecture &sect;5.5). When the
 * local strategy is active and the platform has been given a signing secret, this configuration
 * contributes a {@link JwtDecoder} that verifies tokens issued by the login plugin and a single
 * {@link HttpSecurityCustomizer} that enables JWT-based authentication on the shared filter
 * chain. The signing side lives in {@code qavo-auth-login}; the two share the same secret and
 * algorithm so a token minted by one is accepted by the other.
 *
 * <p>The {@link #qavoLocalJwtDecoder local decoder} is registered as a named bean so a hybrid
 * deployment that also has an external OIDC issuer can compose both decoders (the OIDC config
 * publishes its own named decoder); for pure-local or pure-OIDC the single bean acts as the
 * primary {@link JwtDecoder} by default.
 */
@AutoConfiguration(before = QavoOidcAuthAutoConfiguration.class)
@ConditionalOnClass(JwtDecoder.class)
@ConditionalOnExpression("'${qavo.security.strategy:local}'.toLowerCase() != 'oidc'")
@ConditionalOnProperty(prefix = "qavo.security.local.jwt", name = "secret")
public class QavoLocalJwtAuthAutoConfiguration {

    /** Qualifier used to look up the local decoder when a composite is built. */
    public static final String LOCAL_JWT_DECODER_BEAN = "qavoLocalJwtDecoder";

    @Bean(LOCAL_JWT_DECODER_BEAN)
    @ConditionalOnMissingBean(name = LOCAL_JWT_DECODER_BEAN)
    public JwtDecoder qavoLocalJwtDecoder(QavoSecurityProperties properties) {
        return LocalJwtDecoderFactory.build(properties.getLocal().getJwt());
    }

    /**
     * Registered only when the strategy is purely local. In hybrid deployments a dedicated
     * composite customizer (registered when both local and external decoders are present) is
     * expected to wire the resource-server filter; emitting two customizers would attempt to
     * configure {@code oauth2ResourceServer} twice.
     */
    @Bean
    @ConditionalOnExpression("'${qavo.security.strategy:local}'.toLowerCase() == 'local'")
    public HttpSecurityCustomizer qavoLocalJwtResourceServerCustomizer(
            QavoSecurityProperties properties, JwtDecoder qavoLocalJwtDecoder) {
        JwtAuthenticationConverter authenticationConverter =
                buildAuthenticationConverter(properties.getLocal().getJwt());
        return http -> http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                .decoder(qavoLocalJwtDecoder)
                .jwtAuthenticationConverter(authenticationConverter)));
    }

    private JwtAuthenticationConverter buildAuthenticationConverter(QavoSecurityProperties.Local.Jwt jwt) {
        Converter<Jwt, Collection<GrantedAuthority>> authoritiesConverter = token -> {
            List<GrantedAuthority> authorities = new ArrayList<>();
            Object claim = token.getClaims().get(jwt.getAuthoritiesClaim());
            if (claim instanceof Collection<?> values) {
                for (Object value : values) {
                    authorities.add(new SimpleGrantedAuthority(jwt.getAuthorityPrefix() + value));
                }
            }
            return authorities;
        };
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }
}
