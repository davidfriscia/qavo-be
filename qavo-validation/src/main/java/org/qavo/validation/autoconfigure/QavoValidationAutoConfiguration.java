/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.validation.autoconfigure;

import jakarta.validation.Validator;

import org.qavo.validation.mapping.ValidationErrorMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the validation module. Activates only when Bean Validation is on the
 * classpath, and contributes the {@link ValidationErrorMapper} that the web layer uses to render
 * constraint violations in the standard error format.
 */
@AutoConfiguration
@ConditionalOnClass(Validator.class)
public class QavoValidationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ValidationErrorMapper validationErrorMapper() {
        return new ValidationErrorMapper();
    }
}
