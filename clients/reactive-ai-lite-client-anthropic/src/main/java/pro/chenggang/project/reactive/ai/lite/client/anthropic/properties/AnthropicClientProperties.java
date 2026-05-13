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
package pro.chenggang.project.reactive.ai.lite.client.anthropic.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Configuration properties for the Anthropic client.
 * <p>
 * This class binds to properties prefixed with {@value #PREFIX} to configure
 * connections to Anthropic's AI services (e.g., Claude). It supports configuring
 * multiple authentication profiles and validates the configuration upon initialization.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Getter
@Setter
public class AnthropicClientProperties implements InitializingBean {

    /**
     * The prefix for all Anthropic client properties in the application configuration.
     */
    public static final String PREFIX = "reactive.ai.lite.client.anthropic";

    /**
     * The configuration details specific to the Anthropic chat completion provider.
     */
    private ChatProvider chatProvider = new ChatProvider();

    /**
     * Validates the configuration properties after they have been set by the Spring container.
     *
     * @throws Exception if the configuration is invalid
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        this.checkChatProviderProperties(chatProvider);
    }

    /**
     * Performs detailed validation on the {@link ChatProvider} configuration.
     * <p>
     * It ensures that the base URL and endpoints are defined, and that the configured
     * certifications are valid (e.g., exactly one default certification exists, and all
     * profiles have unique names).
     * </p>
     *
     * @param chatProvider the chat provider configuration to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void checkChatProviderProperties(ChatProvider chatProvider) {
        Assert.notNull(chatProvider, "The chat provider properties are required.");
        Assert.hasLength(chatProvider.getBaseUrl(), "The base-url of Anthropic chat API is required.");
        Assert.hasLength(chatProvider.getChatCompletionEndpoint(), "The chatCompletionEndpoint of Anthropic chat API is required.");
        List<AnthropicCertification> certifications = chatProvider.getCertifications();
        Assert.notEmpty(certifications, "At least one Anthropic certification is required.");
        int defaultCertificationCount = 0;
        Set<String> profiles = new HashSet<>();
        for (AnthropicCertification certification : certifications) {
            Assert.hasText(certification.getToken(), "The token of Anthropic certification is required.");
            Assert.hasText(certification.getProfile(), "The profile of Anthropic certification is required.");
            if (certification.isDefault()) {
                defaultCertificationCount++;
            }
            profiles.add(certification.getProfile());
        }
        Assert.isTrue(defaultCertificationCount > 0, "At least one default Anthropic certification is required.");
        Assert.isTrue(defaultCertificationCount == 1, "Only one default Anthropic certification is allowed.");
        int certificationSize = certifications.size();
        Assert.isTrue(certificationSize <= 1 || profiles.size() == certificationSize, "All Anthropic certification profiles must be unique.");
    }

    /**
     * Configuration properties specifically for the Anthropic chat provider.
     * <p>
     * This includes API endpoints, API versioning, authentication credentials,
     * and optional restrictions on which models can be used.
     * </p>
     *
     * @author Gang Cheng
     */
    @Getter
    @Setter
    public static class ChatProvider {

        /**
         * The base URL for the Anthropic API. Defaults to "https://api.anthropic.com".
         */
        private String baseUrl = "https://api.anthropic.com";

        /**
         * The endpoint path for chat completions. Defaults to "/v1/messages".
         */
        private String chatCompletionEndpoint = "/v1/messages";

        /**
         * The required API version header value for Anthropic requests.
         * Defaults to "2023-06-01".
         */
        private String anthropicVersion = "2023-06-01";

        /**
         * Indicates whether this Anthropic provider should be the default chat provider
         * across the entire application. Defaults to true.
         */
        private boolean isDefault = true;

        /**
         * The list of authentication profiles (API keys) configured for Anthropic.
         */
        private List<AnthropicCertification> certifications = List.of();

        /**
         * A set of specific model names that are allowed to be used with this provider.
         * If null or empty, all models are assumed to be supported.
         */
        private Set<String> limitedModels;
    }

    /**
     * Configuration for an individual Anthropic authentication credential (profile).
     * <p>
     * This class holds an API key and assigns it to a specific profile name, allowing
     * the application to switch between different Anthropic accounts or environments.
     * </p>
     *
     * @author Gang Cheng
     */
    @Getter
    @Setter
    public static class AnthropicCertification {

        /**
         * A unique identifier for this set of credentials. Defaults to "default-profile".
         */
        private String profile = "default-profile";

        /**
         * The API key used to authenticate with Anthropic. This field is required.
         */
        private String token;

        /**
         * Indicates whether this profile should be used as the fallback/default when
         * no specific profile is requested. Exactly one profile must be marked as default.
         * Defaults to false.
         */
        private boolean isDefault = false;
    }
}
