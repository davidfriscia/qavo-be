/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.validation.constraints;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;

/**
 * Reusable constraint asserting that a value is a URL-safe "slug": lowercase alphanumeric
 * segments separated by single hyphens (e.g. {@code my-resource-name}). Demonstrates composing
 * a domain constraint from a standard {@link Pattern}.
 */
@Documented
@Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$")
@Constraint(validatedBy = {})
@Target({FIELD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface Slug {

    String message() default "{org.qavo.validation.Slug.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
