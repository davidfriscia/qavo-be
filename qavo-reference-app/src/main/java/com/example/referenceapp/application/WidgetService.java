/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package com.example.referenceapp.application;

import java.util.UUID;

import com.example.referenceapp.domain.Widget;
import com.example.referenceapp.infrastructure.WidgetRepository;
import org.qavo.core.domain.exception.ConflictException;
import org.qavo.core.domain.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application-layer use-case orchestration for the widget catalog (architecture &sect;4). It
 * coordinates the domain and the repository and owns transaction boundaries; it raises the
 * platform's exceptions, which the global handler renders as RFC 9457 responses.
 */
@Service
public class WidgetService {

    private final WidgetRepository widgetRepository;

    public WidgetService(WidgetRepository widgetRepository) {
        this.widgetRepository = widgetRepository;
    }

    @Transactional(readOnly = true)
    public Page<Widget> list(Pageable pageable) {
        return widgetRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Widget getById(UUID id) {
        return widgetRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Widget", id));
    }

    @Transactional
    public Widget create(String code, String name, String description) {
        if (widgetRepository.existsByCode(code)) {
            throw new ConflictException("A widget with code '%s' already exists".formatted(code));
        }
        return widgetRepository.save(new Widget(UUID.randomUUID(), code, name, description));
    }
}
