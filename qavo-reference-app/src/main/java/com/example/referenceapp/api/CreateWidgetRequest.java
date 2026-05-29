/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package com.example.referenceapp.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.qavo.validation.constraints.Slug;

/**
 * Inbound DTO for creating a widget. Demonstrates declarative boundary validation combining
 * standard constraints with the platform's reusable {@link Slug} constraint (architecture &sect;5.4).
 */
public record CreateWidgetRequest(
        @NotBlank @Slug @Size(max = 128) String code,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 2000) String description) {
}
