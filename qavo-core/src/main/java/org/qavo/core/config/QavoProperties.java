/* SPDX-License-Identifier: Apache-2.0 — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.core.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.qavo.core.api.ApiConventions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Root of the platform's strongly-typed configuration under the {@code qavo.*} namespace
 * (see architecture &sect;5.6). Application-specific configuration lives under {@code app.*}
 * and is owned by the application, keeping the two concerns visually separated in every
 * {@code application.yml}.
 *
 * <p>Every property has a sensible default; applications override only what they need.
 */
@ConfigurationProperties(prefix = "qavo")
public class QavoProperties {

    /** REST/API conventions and the OpenAPI info block. */
    @NestedConfigurationProperty
    private final Api api = new Api();

    /** Error response (RFC 9457) settings. */
    @NestedConfigurationProperty
    private final Error error = new Error();

    /**
     * Statically-evaluated feature flags under {@code qavo.features.*}. A flag absent from the
     * map is treated as disabled. Dynamic, request-time flags are served by the
     * {@code FeatureFlagService}, which consults this map as one of its sources.
     */
    private final Map<String, Boolean> features = new LinkedHashMap<>();

    public Api getApi() {
        return api;
    }

    public Error getError() {
        return error;
    }

    public Map<String, Boolean> getFeatures() {
        return features;
    }

    /** REST conventions and OpenAPI metadata. */
    public static class Api {

        /** Base path inherited by all controllers. Path-based versioning is enforced. */
        private String basePath = ApiConventions.BASE_PATH;

        /** Title surfaced in the generated OpenAPI document. */
        private String title = "Qavo Application API";

        /** Version surfaced in the generated OpenAPI document. */
        private String version = "v1";

        /** Description surfaced in the generated OpenAPI document. */
        private String description = "API built on the Qavo platform.";

        /** Contact name for the OpenAPI info block. */
        private String contactName = "Qavo";

        /** Contact email for the OpenAPI info block. */
        private String contactEmail = "support@qavo.org";

        /** License name for the OpenAPI info block. */
        private String licenseName = "Apache-2.0";

        public String getBasePath() {
            return basePath;
        }

        public void setBasePath(String basePath) {
            this.basePath = basePath;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getContactName() {
            return contactName;
        }

        public void setContactName(String contactName) {
            this.contactName = contactName;
        }

        public String getContactEmail() {
            return contactEmail;
        }

        public void setContactEmail(String contactEmail) {
            this.contactEmail = contactEmail;
        }

        public String getLicenseName() {
            return licenseName;
        }

        public void setLicenseName(String licenseName) {
            this.licenseName = licenseName;
        }
    }

    /** RFC 9457 Problem Details configuration. */
    public static class Error {

        /** Base URI used to build the {@code type} member of Problem Details responses. */
        private String baseUri = "https://errors.qavo.org";

        /**
         * Whether to include exception messages in the {@code detail} field for server-side
         * (5xx) errors. Kept {@code false} by default to avoid leaking internals.
         */
        private boolean includeStackTraceDetail = false;

        public String getBaseUri() {
            return baseUri;
        }

        public void setBaseUri(String baseUri) {
            this.baseUri = baseUri;
        }

        public boolean isIncludeStackTraceDetail() {
            return includeStackTraceDetail;
        }

        public void setIncludeStackTraceDetail(boolean includeStackTraceDetail) {
            this.includeStackTraceDetail = includeStackTraceDetail;
        }
    }
}
