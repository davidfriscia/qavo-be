/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package com.example.referenceapp.infrastructure;

import java.util.UUID;

import com.example.referenceapp.domain.Widget;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Infrastructure-layer persistence access for {@link Widget} (architecture &sect;4).
 */
public interface WidgetRepository extends JpaRepository<Widget, UUID> {

    boolean existsByCode(String code);
}
