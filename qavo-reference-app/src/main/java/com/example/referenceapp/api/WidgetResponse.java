/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package com.example.referenceapp.api;

import java.util.UUID;

import com.example.referenceapp.domain.Widget;

/**
 * Outbound DTO for a widget. Keeping a dedicated response type decouples the wire contract from the
 * persistence model (architecture &sect;4).
 */
public record WidgetResponse(UUID id, String code, String name, String description) {

    public static WidgetResponse from(Widget widget) {
        return new WidgetResponse(widget.getId(), widget.getCode(), widget.getName(), widget.getDescription());
    }
}
