/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.security.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * Adds the local-auth package to {@link AutoConfigurationPackages} <em>additively</em>, so the
 * platform's user/role entities and repositories are scanned <strong>alongside</strong> the
 * consuming application's own.
 *
 * <p>Using {@code @EntityScan}/{@code @EnableJpaRepositories} here would be a mistake: a single
 * {@code @EntityScan} replaces Spring Boot's auto-detected packages, which would silently drop the
 * application's entities, and an explicit {@code @EnableJpaRepositories} would disable Boot's
 * repository auto-scan. Registering the package with {@code AutoConfigurationPackages} merges with
 * the application's package instead, which is the correct mechanism for a starter contributing
 * persistent types.
 */
public class QavoLocalAuthPackageRegistrar implements ImportBeanDefinitionRegistrar {

    static final String LOCAL_AUTH_PACKAGE = "org.qavo.security.local";

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {
        AutoConfigurationPackages.register(registry, LOCAL_AUTH_PACKAGE);
    }
}
