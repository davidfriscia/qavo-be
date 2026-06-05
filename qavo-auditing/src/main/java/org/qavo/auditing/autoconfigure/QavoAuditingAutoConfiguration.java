/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auditing.autoconfigure;

import org.qavo.auditing.QavoAuditingProperties;
import org.qavo.auditing.QavoAuditorAware;
import org.qavo.core.security.SecurityContextAccessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Activates Spring Data JPA Auditing platform-wide whenever JPA is on the classpath and the
 * feature is not explicitly disabled. The application does NOT need its own
 * {@code @EnableJpaAuditing} — declaring it here means every Qavo-based service gets uniform
 * created/modified tracking simply by extending {@link org.qavo.auditing.AuditableEntity}.
 *
 * <p>The {@link AuditorAware} bean is gated by {@link ConditionalOnMissingBean} so applications
 * with non-string auditor types (e.g. {@code AuditorAware<UUID>}) can override it without the
 * platform fighting them.
 */
@AutoConfiguration
@ConditionalOnClass({JpaRepository.class, EnableJpaAuditing.class})
@ConditionalOnProperty(prefix = "qavo.auditing", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(QavoAuditingProperties.class)
@EnableJpaAuditing(auditorAwareRef = "qavoAuditorAware")
public class QavoAuditingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AuditorAware.class)
    public QavoAuditorAware qavoAuditorAware(SecurityContextAccessor securityContextAccessor,
                                             QavoAuditingProperties properties) {
        return new QavoAuditorAware(securityContextAccessor, properties.getSystemPrincipal());
    }
}
