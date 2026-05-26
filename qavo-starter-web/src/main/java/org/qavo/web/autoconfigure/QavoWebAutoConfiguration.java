/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.web.autoconfigure;

import org.qavo.core.api.error.ProblemDetailFactory;
import org.qavo.core.autoconfigure.QavoCoreAutoConfiguration;
import org.qavo.core.config.QavoProperties;
import org.qavo.validation.mapping.ValidationErrorMapper;
import org.qavo.web.error.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

/**
 * Wires the presentation-layer cross-cutting concerns shared by every Qavo web application
 * (architecture &sect;5.1, &sect;5.2, &sect;5.6): the global RFC 9457 exception handler and the
 * standard pagination contract. Each bean is overridable, so applications can customize behavior
 * without losing the rest of the baseline.
 */
@AutoConfiguration(after = QavoCoreAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class QavoWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler qavoGlobalExceptionHandler(ProblemDetailFactory problemDetailFactory,
                                                             ValidationErrorMapper validationErrorMapper,
                                                             QavoProperties properties) {
        return new GlobalExceptionHandler(problemDetailFactory, validationErrorMapper, properties);
    }

    /**
     * Enforces a conservative maximum page size and zero-based paging, keeping the {@code page} /
     * {@code size} / {@code sort} contract uniform and resistant to oversized-page requests.
     */
    @Bean
    @ConditionalOnMissingBean
    public PageableHandlerMethodArgumentResolverCustomizer qavoPageableCustomizer() {
        return resolver -> {
            resolver.setMaxPageSize(200);
            resolver.setOneIndexedParameters(false);
            resolver.setFallbackPageable(org.springframework.data.domain.PageRequest.of(0, 20));
        };
    }
}
