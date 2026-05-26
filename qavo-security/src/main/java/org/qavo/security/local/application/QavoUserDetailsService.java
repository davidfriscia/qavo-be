/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.security.local.application;

import java.util.ArrayList;
import java.util.List;

import org.qavo.security.local.domain.QavoRole;
import org.qavo.security.local.domain.QavoUser;
import org.qavo.security.local.infrastructure.QavoUserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bridges the local {@link QavoUser} store to Spring Security's {@link UserDetailsService}.
 * Roles are exposed as {@code ROLE_}-prefixed authorities and permissions as bare authorities,
 * matching the split that {@code AuthenticatedPrincipalAdapter} reverses when building the
 * uniform security context (see architecture &sect;5.5).
 */
public class QavoUserDetailsService implements UserDetailsService {

    private final QavoUserRepository userRepository;

    public QavoUserDetailsService(QavoUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        QavoUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No user with username '%s'".formatted(username)));

        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(toAuthorities(user))
                .disabled(!user.isEnabled())
                .accountLocked(!user.isAccountNonLocked())
                .build();
    }

    private List<GrantedAuthority> toAuthorities(QavoUser user) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (QavoRole role : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            for (String permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission));
            }
        }
        return authorities;
    }
}
