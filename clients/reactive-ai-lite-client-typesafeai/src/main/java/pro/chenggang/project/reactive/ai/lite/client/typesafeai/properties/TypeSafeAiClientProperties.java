/*
 *    Copyright 2025-2026 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package pro.chenggang.project.reactive.ai.lite.client.typesafeai.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import pro.chenggang.project.reactive.ai.lite.core.option.Capability;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Central configuration properties container for the TypeSafe AI reactive AI client.
 * <p>
 * Manages configuration for connecting to and authenticating with TypeSafe AI's APIs,
 * including base URLs, multi-profile authentication tokens, and capability-specific
 * settings for SystemOne operations.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Getter
@Setter
public class TypeSafeAiClientProperties implements InitializingBean {

    /**
     * Configuration property prefix for all TypeSafe AI client settings.
     */
    public static final String PREFIX = "reactive.ai.lite.client.typesafeai";

    /**
     * The default base URL for the TypeSafe AI API.
     */
    private String baseUrl = "https://api.typesafe.ai";

    /**
     * Authentication credentials for accessing TypeSafe AI services.
     */
    private List<TypeSafeAiCertification> certifications = List.of();

    /**
     * Configuration specific to the SystemOne capability.
     */
    private SystemOneProperties systemOne = new SystemOneProperties();

    /**
     * Validates configuration properties and assigns automatic defaults upon initialization.
     *
     * @throws Exception if validation fails
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if (certifications.size() == 1) {
            certifications.getFirst().setDefault(true);
        }
        this.checkRootProperties();
        if (systemOne != null && systemOne.isEnabled()) {
            this.checkSystemOneProperties(systemOne);
        }
    }

    /**
     * Validates root properties (base URL and certification constraints).
     */
    private void checkRootProperties() {
        Assert.hasLength(this.baseUrl, "The base-url of TypeSafe AI API is required.");
        if (CollectionUtils.isEmpty(this.certifications)) {
            return;
        }
        int defaultCertificationCount = 0;
        Set<String> profiles = new HashSet<>();
        for (TypeSafeAiCertification certification : certifications) {
            Assert.hasText(certification.getToken(), "The token of TypeSafe AI certification is required.");
            Assert.hasText(certification.getProfile(), "The profile of TypeSafe AI certification is required.");
            if (certification.isDefault()) {
                defaultCertificationCount++;
            }
            profiles.add(certification.getProfile());
        }
        Assert.isTrue(defaultCertificationCount > 0, "At least one default TypeSafe AI certification is required when certifications are provided.");
        Assert.isTrue(defaultCertificationCount == 1, "Only one default TypeSafe AI certification is allowed.");
        int certificationSize = certifications.size();
        Assert.isTrue(certificationSize <= 1 || profiles.size() == certificationSize, "All TypeSafe AI certification profiles must be unique.");
    }

    /**
     * Validates SystemOne capability properties.
     *
     * @param systemOne the SystemOne properties to check; must not be null
     */
    private void checkSystemOneProperties(SystemOneProperties systemOne) {
        Assert.hasLength(systemOne.getEndpoint(), "The endpoint of TypeSafe AI SystemOne API is required.");
    }

    /**
     * Resolves the effective base URL for SystemOne requests, cascading to global base URL if not set.
     *
     * @return the resolved base URL for SystemOne requests
     */
    public String getSystemOneBaseUrl() {
        if (Objects.nonNull(systemOne) && Objects.nonNull(systemOne.getBaseUrl())) {
            return systemOne.getBaseUrl();
        }
        return this.baseUrl;
    }

    /**
     * Configuration properties for SystemOne operations.
     */
    @Getter
    @Setter
    public static class SystemOneProperties {

        /**
         * Whether the SystemOne capability is enabled. Defaults to {@code true}.
         */
        private boolean enabled = true;

        /**
         * Optional base URL override for SystemOne requests.
         */
        private String baseUrl;

        /**
         * Endpoint path for SystemOne evaluation operations. Defaults to {@code "/v1/systemone"}.
         */
        private String endpoint = "/v1/systemone";

        /**
         * Whether this provider should be considered the default SystemOne provider.
         */
        private boolean isDefault = true;

        /**
         * Optional restriction to specific model names.
         */
        private Set<String> limitedModels;
    }

    /**
     * Authentication credential definition for TypeSafe AI.
     */
    @Getter
    @Setter
    public static class TypeSafeAiCertification {

        /**
         * Unique profile name for this certification.
         */
        private String profile = "default-profile";

        /**
         * The API token (bearer token) for authenticating requests.
         */
        private String token;

        /**
         * Flag indicating whether this credential is the default profile.
         */
        private boolean isDefault = false;

        /**
         * Optional restriction to a specific capability.
         */
        private Capability capability;
    }
}
