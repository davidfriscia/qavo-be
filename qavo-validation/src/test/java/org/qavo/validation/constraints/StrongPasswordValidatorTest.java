/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.validation.constraints;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import jakarta.validation.ValidatorFactory;

class StrongPasswordValidatorTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    record Account(@StrongPassword String password) {
    }

    @Test
    void acceptsCompliantPassword() {
        Set<ConstraintViolation<Account>> violations = validator.validate(new Account("Str0ng-P@ssword!"));
        assertThat(violations).isEmpty();
    }

    @Test
    void rejectsWeakPassword() {
        Set<ConstraintViolation<Account>> violations = validator.validate(new Account("weak"));
        assertThat(violations).hasSize(1);
    }

    @Test
    void treatsNullAsValidToComposeWithNotNull() {
        Set<ConstraintViolation<Account>> violations = validator.validate(new Account(null));
        assertThat(violations).isEmpty();
    }
}
