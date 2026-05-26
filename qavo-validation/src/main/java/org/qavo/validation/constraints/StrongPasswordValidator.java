/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.validation.constraints;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates {@link StrongPassword}. A {@code null} value is treated as valid so the constraint
 * composes with {@code @NotNull}/{@code @NotBlank} rather than duplicating presence checks.
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, CharSequence> {

    private int minLength;
    private boolean requireUppercase;
    private boolean requireLowercase;
    private boolean requireDigit;
    private boolean requireSpecial;

    @Override
    public void initialize(StrongPassword constraint) {
        this.minLength = constraint.minLength();
        this.requireUppercase = constraint.requireUppercase();
        this.requireLowercase = constraint.requireLowercase();
        this.requireDigit = constraint.requireDigit();
        this.requireSpecial = constraint.requireSpecial();
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        String password = value.toString();
        if (password.length() < minLength) {
            return false;
        }
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else {
                hasSpecial = true;
            }
        }
        return (!requireUppercase || hasUpper)
                && (!requireLowercase || hasLower)
                && (!requireDigit || hasDigit)
                && (!requireSpecial || hasSpecial);
    }
}
