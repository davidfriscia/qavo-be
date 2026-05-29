/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.security.local.domain;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

/**
 * A role in the local authentication store. A role aggregates fine-grained permissions
 * (e.g. {@code user:read}); the security context exposes roles and permissions separately
 * (see architecture &sect;5.5). Part of the local-auth baseline that ships with the platform.
 */
@Entity
@Table(name = "qavo_roles")
public class QavoRole {

    @Id
    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "qavo_role_permissions", joinColumns = @JoinColumn(name = "role_name"))
    @Column(name = "permission", nullable = false, length = 128)
    private Set<String> permissions = new HashSet<>();

    protected QavoRole() {
        // Required by JPA.
    }

    public QavoRole(String name, Set<String> permissions) {
        this.name = name;
        this.permissions = new HashSet<>(permissions);
    }

    public String getName() {
        return name;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }
}
