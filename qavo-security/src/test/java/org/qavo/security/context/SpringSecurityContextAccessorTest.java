/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.security.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.qavo.core.security.AuthenticatedPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class SpringSecurityContextAccessorTest {

    private final SpringSecurityContextAccessor accessor = new SpringSecurityContextAccessor();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void emptyWhenNoAuthentication() {
        assertThat(accessor.currentPrincipal()).isEmpty();
    }

    @Test
    void splitsRolesAndPermissions() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "alice", "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("user:read")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        AuthenticatedPrincipal principal = accessor.currentPrincipal().orElseThrow();

        assertThat(principal.username()).isEqualTo("alice");
        assertThat(principal.roles()).containsExactly("ADMIN");
        assertThat(principal.permissions()).containsExactly("user:read");
    }
}
