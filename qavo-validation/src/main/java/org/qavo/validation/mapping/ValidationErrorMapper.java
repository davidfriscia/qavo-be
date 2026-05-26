/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.validation.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;

import org.qavo.core.api.error.FieldErrorDetail;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

/**
 * Translates Bean Validation failures into the platform's {@link FieldErrorDetail} list, so the
 * global exception handler can surface them in the {@code errors} array of the RFC 9457 response
 * (see architecture &sect;5.4). This is the single point where constraint violations become wire
 * format, guaranteeing every application reports validation errors identically.
 */
public class ValidationErrorMapper {

    /** Maps the field and global errors of a Spring {@link BindingResult} (e.g. from {@code @Valid}). */
    public List<FieldErrorDetail> fromBindingResult(BindingResult bindingResult) {
        List<FieldErrorDetail> details = new ArrayList<>();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            details.add(FieldErrorDetail.of(
                    fieldError.getField(),
                    resolveMessage(fieldError),
                    fieldError.getCode()));
        }
        for (ObjectError globalError : bindingResult.getGlobalErrors()) {
            details.add(FieldErrorDetail.of(
                    globalError.getObjectName(),
                    resolveMessage(globalError),
                    globalError.getCode()));
        }
        return details;
    }

    /** Maps a set of {@link ConstraintViolation}s (e.g. from method-level validation). */
    public List<FieldErrorDetail> fromConstraintViolations(Set<? extends ConstraintViolation<?>> violations) {
        List<FieldErrorDetail> details = new ArrayList<>(violations.size());
        for (ConstraintViolation<?> violation : violations) {
            details.add(FieldErrorDetail.of(
                    leafNode(violation.getPropertyPath()),
                    violation.getMessage(),
                    constraintCode(violation)));
        }
        return details;
    }

    private String resolveMessage(ObjectError error) {
        return error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value";
    }

    /** The last path segment is the offending property name; falls back to the full path. */
    private String leafNode(Path propertyPath) {
        String leaf = null;
        for (Path.Node node : propertyPath) {
            leaf = node.getName();
        }
        return leaf != null ? leaf : propertyPath.toString();
    }

    private String constraintCode(ConstraintViolation<?> violation) {
        return violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();
    }
}
