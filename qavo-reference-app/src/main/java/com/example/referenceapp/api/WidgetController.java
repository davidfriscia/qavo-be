/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package com.example.referenceapp.api;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import com.example.referenceapp.application.WidgetService;
import org.qavo.core.api.ApiConventions;
import org.qavo.core.api.pagination.PagedResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Presentation layer for the widget catalog (architecture &sect;4). It inherits the {@code /api/v1}
 * prefix convention, returns the standard {@link PagedResponse} envelope for collections, and uses
 * method-level authorization via {@code @PreAuthorize} over the platform's uniform permission model
 * (architecture &sect;5.5). It contains no business logic.
 */
@RestController
@RequestMapping(ApiConventions.BASE_PATH + "/widgets")
public class WidgetController {

    private final WidgetService widgetService;

    public WidgetController(WidgetService widgetService) {
        this.widgetService = widgetService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    public PagedResponse<WidgetResponse> list(Pageable pageable) {
        return PagedResponse.from(widgetService.list(pageable), WidgetResponse::from);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:read')")
    public WidgetResponse get(@PathVariable UUID id) {
        return WidgetResponse.from(widgetService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user:write')")
    public ResponseEntity<WidgetResponse> create(@Valid @RequestBody CreateWidgetRequest request) {
        WidgetResponse response = WidgetResponse.from(
                widgetService.create(request.code(), request.name(), request.description()));
        return ResponseEntity.created(URI.create(ApiConventions.BASE_PATH + "/widgets/" + response.id()))
                .body(response);
    }
}
