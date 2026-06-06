/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.registration.autoconfigure;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Adds the registration plugin's persistence packages to {@link AutoConfigurationPackages}
 * additively, so {@link org.qavo.auth.registration.domain.QavoEmailVerificationToken} and
 * {@link org.qavo.auth.registration.infrastructure.QavoEmailVerificationTokenRepository} are
 * scanned alongside the application's own. Mirrors the pattern used by
 * {@code QavoLocalAuthPackageRegistrar} and {@code QavoAuthLoginPackageRegistrar}; declaring
 * {@code @EntityScan} or {@code @EnableJpaRepositories} here would silently drop the
 * application's own packages.
 */
public class QavoAuthRegistrationPackageRegistrar implements ImportBeanDefinitionRegistrar {

    static final String DOMAIN_PACKAGE = "org.qavo.auth.registration.domain";
    static final String INFRASTRUCTURE_PACKAGE = "org.qavo.auth.registration.infrastructure";

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {
        AutoConfigurationPackages.register(registry, DOMAIN_PACKAGE, INFRASTRUCTURE_PACKAGE);
    }
}
