/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.security.context;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.qavo.core.security.AuthenticatedPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Adapts a Spring Security {@link Authentication} into the strategy-independent
 * {@link AuthenticatedPrincipal}. Authorities prefixed with {@code ROLE_} become roles (with the
 * prefix stripped); all other authorities are treated as fine-grained permissions. This single
 * mapping is what lets business code stay unaware of whether the token came from the local store
 * or an external IdP (see architecture &sect;5.5).
 */
final class AuthenticatedPrincipalAdapter implements AuthenticatedPrincipal {

    private static final String ROLE_PREFIX = "ROLE_";

    private final String id;
    private final String username;
    private final Set<String> roles;
    private final Set<String> permissions;
    private final Map<String, Object> attributes;

    private AuthenticatedPrincipalAdapter(String id, String username, Set<String> roles,
                                          Set<String> permissions, Map<String, Object> attributes) {
        this.id = id;
        this.username = username;
        this.roles = roles;
        this.permissions = permissions;
        this.attributes = attributes;
    }

    static AuthenticatedPrincipalAdapter from(Authentication authentication) {
        Set<String> roles = new LinkedHashSet<>();
        Set<String> permissions = new LinkedHashSet<>();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String value = authority.getAuthority();
            if (value.startsWith(ROLE_PREFIX)) {
                roles.add(value.substring(ROLE_PREFIX.length()));
            } else {
                permissions.add(value);
            }
        }

        String name = authentication.getName();
        // Attributes are left empty in the baseline adapter to keep it classloader-safe when the
        // optional OAuth2 dependency is absent. The OIDC auto-configuration can register a richer
        // SecurityContextAccessor that surfaces JWT claims when that strategy is active.
        Map<String, Object> attributes = new LinkedHashMap<>();

        return new AuthenticatedPrincipalAdapter(name, name, roles, permissions, attributes);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String username() {
        return username;
    }

    @Override
    public Set<String> roles() {
        return roles;
    }

    @Override
    public Set<String> permissions() {
        return permissions;
    }

    @Override
    public Map<String, Object> attributes() {
        return attributes;
    }
}
