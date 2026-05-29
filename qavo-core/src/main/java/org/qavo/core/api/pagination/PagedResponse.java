/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.api.pagination;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Uniform envelope for paginated collections returned by every Qavo-based API
 * (see architecture &sect;5.6). Decouples the wire contract from Spring Data's {@code Page},
 * which is intentionally not exposed directly because its JSON shape is unstable across versions.
 *
 * @param content       the page of items
 * @param number        zero-based index of the current page
 * @param size          requested page size
 * @param totalElements total number of items across all pages
 * @param totalPages    total number of pages
 * @param first         whether this is the first page
 * @param last          whether this is the last page
 * @param <T>           element type
 */
public record PagedResponse<T>(
        List<T> content,
        int number,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    /** Wraps a Spring Data {@link Page} into the platform envelope. */
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    /** Wraps a {@link Page} of entities, mapping each element to a DTO with the given function. */
    public static <E, T> PagedResponse<T> from(Page<E> page, Function<? super E, ? extends T> mapper) {
        return new PagedResponse<>(
                page.getContent().stream().<T>map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
