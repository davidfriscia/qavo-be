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

/**
 * Reusable constraint asserting that a password meets a configurable strength policy
 * (see architecture &sect;5.4). Serves as the canonical example of a platform-provided,
 * parameterizable constraint that applications and plugins can apply directly.
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({FIELD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface StrongPassword {

    String message() default "{org.qavo.validation.StrongPassword.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** Minimum length. */
    int minLength() default 12;

    /** Require at least one uppercase letter. */
    boolean requireUppercase() default true;

    /** Require at least one lowercase letter. */
    boolean requireLowercase() default true;

    /** Require at least one digit. */
    boolean requireDigit() default true;

    /** Require at least one non-alphanumeric character. */
    boolean requireSpecial() default true;
}
