/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.auth.login.autoconfigure;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Adds the login plugin's JWT package to {@link AutoConfigurationPackages} additively, so the
 * {@link org.qavo.auth.login.jwt.RefreshToken} entity and
 * {@link org.qavo.auth.login.jwt.RefreshTokenRepository} are scanned alongside the application's
 * own. Uses the same merging pattern as {@code QavoLocalAuthPackageRegistrar}; declaring
 * {@code @EntityScan} or {@code @EnableJpaRepositories} here would silently drop the
 * application's packages and break unrelated repositories.
 */
public class QavoAuthLoginPackageRegistrar implements ImportBeanDefinitionRegistrar {

    static final String LOGIN_JWT_PACKAGE = "org.qavo.auth.login.jwt";

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {
        AutoConfigurationPackages.register(registry, LOGIN_JWT_PACKAGE);
    }
}
