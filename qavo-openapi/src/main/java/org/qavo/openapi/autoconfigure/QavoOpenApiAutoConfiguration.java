/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.openapi.autoconfigure;

import java.util.List;
import java.util.stream.Collectors;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

import org.qavo.core.api.ApiConventions;
import org.qavo.core.autoconfigure.QavoCoreAutoConfiguration;
import org.qavo.core.config.QavoProperties;
import org.qavo.core.plugin.PluginRegistry;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for API documentation (architecture &sect;5.6). Builds the OpenAPI info block
 * from {@code qavo.api.*} so every application's contract is described consistently, and adds a
 * plugin-aware customizer that records the active plugins as a top-level extension — making the
 * composed capability set visible in the generated document and the Swagger UI.
 */
@AutoConfiguration(after = QavoCoreAutoConfiguration.class)
@ConditionalOnClass(OpenAPI.class)
public class QavoOpenApiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OpenAPI qavoOpenApi(QavoProperties properties) {
        QavoProperties.Api api = properties.getApi();
        return new OpenAPI().info(new Info()
                .title(api.getTitle())
                .version(api.getVersion())
                .description(api.getDescription())
                .contact(new Contact().name(api.getContactName()).email(api.getContactEmail()))
                .license(new License().name(api.getLicenseName())));
    }

    /** Default group exposing the platform-versioned API surface (e.g. {@code /api/v1/**}). */
    @Bean
    @ConditionalOnMissingBean(name = "qavoPlatformApiGroup")
    public GroupedOpenApi qavoPlatformApiGroup() {
        return GroupedOpenApi.builder()
                .group("platform")
                .pathsToMatch(ApiConventions.BASE_PATH + "/**")
                .build();
    }

    /**
     * Records the active plugins as the {@code x-qavo-plugins} OpenAPI extension, so consumers and
     * generated clients can see which capabilities the running application composed.
     */
    @Bean
    public OpenApiCustomizer qavoPluginInfoCustomizer(PluginRegistry pluginRegistry) {
        return openApi -> {
            List<String> plugins = pluginRegistry.getPlugins().stream()
                    .map(p -> p.id() + "@" + p.version())
                    .collect(Collectors.toList());
            openApi.getInfo().addExtension("x-qavo-plugins", plugins);
        };
    }
}
