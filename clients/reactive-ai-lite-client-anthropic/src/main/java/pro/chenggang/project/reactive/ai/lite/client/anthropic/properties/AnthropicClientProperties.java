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
import java.util.Objects;
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
     * The default base URL for the Anthropic API.
     * <p>
     * All API calls will be relative to this URL unless overridden by a specific capability
     * (e.g., {@link ChatProperties#baseUrl}). Defaults to "https://api.anthropic.com".
     * </p>
     */
    private String baseUrl = "https://api.anthropic.com";

    /**
     * The required API version header value for Anthropic requests.
     * <p>
     * This value is sent as the {@code anthropic-version} HTTP header and determines
     * the API behavior version. Defaults to "2023-06-01".
     * </p>
     */
    private String anthropicVersion = "2023-06-01";

    /**
     * The list of authentication profiles (API keys) configured for Anthropic.
     * <p>
     * Each entry defines a named profile with its API key. At least one profile must be
     * configured, and exactly one must be marked as the default.
     * </p>
     */
    private List<AnthropicCertification> certifications = List.of();

    /**
     * The configuration details specific to the Anthropic chat completion provider.
     */
    private ChatProperties chat = new ChatProperties();

    /**
     * Validates the configuration properties after they have been set by the Spring container.
     * <p>
     * If only one certification profile is provided, it is automatically set as the default.
     * Then the root Anthropic properties and, if chat is enabled, the chat properties are
     * validated for completeness and correctness.
     * </p>
     *
     * @throws Exception if the configuration is invalid
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if(certifications.size() == 1){
            certifications.getFirst().setDefault(true);
        }
        this.checkRootProperties();
        if (chat != null && chat.isEnabled()) {
            this.checkChatProperties(chat);
        }
    }

    /**
     * Performs detailed validation on the root Anthropic API configuration.
     * <p>
     * Ensures that the base URL and API version are provided, that at least one
     * certification is present, that every certification has a non‑empty token and profile,
     * that exactly one default certification exists, and that all profile names are unique
     * when multiple profiles are configured.
     * </p>
     */
    private void checkRootProperties() {
        Assert.hasLength(this.baseUrl, "The base-url of Anthropic API is required.");
        Assert.hasLength(this.anthropicVersion, "The anthropic-version of Anthropic API is required.");
        Assert.notEmpty(this.certifications, "At least one Anthropic certification is required.");
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
     * Performs validation on the chat capability configuration.
     * <p>
     * Ensures that, when chat is enabled, the required endpoint path is present.
     * </p>
     *
     * @param chat the chat properties to validate; must not be null
     */
    private void checkChatProperties(ChatProperties chat) {
        Assert.hasLength(chat.getEndpoint(), "The endpoint of Anthropic chat API is required.");
    }

    /**
     * Returns the base URL that will be used for chat API calls.
     * <p>
     * If chat‑specific properties define a non‑null {@link ChatProperties#baseUrl},
     * that value is returned. Otherwise, the global {@link #baseUrl} is used as a fallback.
     * </p>
     *
     * @return the effective base URL for chat interactions
     */
    public String getChatBaseUrl() {
        if (Objects.nonNull(chat) && Objects.nonNull(chat.getBaseUrl())) {
            return chat.getBaseUrl();
        }
        return this.baseUrl;
    }

    /**
     * Configuration properties specifically for the Anthropic chat provider.
     * <p>
     * Allows overriding the base URL, endpoint, and model restrictions for the chat
     * completion API. The entire chat capability can be disabled through the
     * {@link #enabled} flag.
     * </p>
     */
    @Getter
    @Setter
    public static class ChatProperties {

        /**
         * A flag indicating if the chat capability is enabled.
         * <p>
         * When set to {@code false}, no chat‑related beans will be created and no
         * validation of chat‑specific properties occurs. Defaults to {@code true}.
         * </p>
         */
        private boolean enabled = true;

        /**
         * The base URL for the chat completions API.
         * <p>
         * If {@code null}, the provider‑level {@link AnthropicClientProperties#baseUrl}
         * is used instead. This allows chat to target a different host if needed.
         * </p>
         */
        private String baseUrl;

        /**
         * The endpoint path for chat completions.
         * <p>
         * Appended to the resolved base URL to form the full API URI. Defaults to
         * "/v1/messages".
         * </p>
         */
        private String endpoint = "/v1/messages";

        /**
         * Indicates whether this Anthropic provider should be the default chat provider
         * across the entire application.
         * <p>
         * When multiple AI providers are present, the one marked as default will be
         * used if no explicit provider is chosen. Defaults to {@code true}.
         * </p>
         */
        private boolean isDefault = true;

        /**
         * A set of specific model names that are allowed to be used with this provider.
         * <p>
         * If {@code null} or empty, all models are assumed to be supported. When defined,
         * only requests for models in this set will be routed to this provider.
         * </p>
         */
        private Set<String> limitedModels;
    }

    /**
     * Configuration for an individual Anthropic authentication credential (profile).
     * <p>
     * Each profile associates a unique name with an API key and a default flag.
     * Multiple profiles allow dynamic selection of credentials at runtime.
     * </p>
     */
    @Getter
    @Setter
    public static class AnthropicCertification {

        /**
         * A unique identifier for this set of credentials.
         * <p>
         * Used to select a profile when multiple API keys are configured. Defaults
         * to "default-profile".
         * </p>
         */
        private String profile = "default-profile";

        /**
         * The API key used to authenticate with Anthropic.
         * <p>
         * This value is sent as the {@code x-api-key} header. It is required and must
         * not be blank.
         * </p>
         */
        private String token;

        /**
         * Indicates whether this profile should be used as the fallback/default when
         * no specific profile is requested.
         * <p>
         * Exactly one profile in the list must be marked as default. Defaults to
         * {@code false}.
         * </p>
         */
        private boolean isDefault = false;
    }
}