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
package pro.chenggang.project.reactive.ai.lite.client.openai.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import pro.chenggang.project.reactive.ai.lite.core.option.Capability;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration properties for the OpenAI client.
 * <p>
 * This class holds the hierarchical configuration settings required to connect and authenticate
 * with OpenAI's API services. It implements {@link InitializingBean} to perform
 * validation after all properties are set, ensuring that the application fails fast during startup
 * if any critical configuration is missing or inconsistent.
 * The properties support multiple authentication profiles (certifications) with the ability to specify
 * a default. Each capability (chat, embedding) can be independently enabled and fine-tuned with
 * its own base URL, endpoint, and allowed model list.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Getter
@Setter
public class OpenaiClientProperties implements InitializingBean {

    /**
     * The prefix for all OpenAI client properties in configuration files.
     * <p>
     * This constant is used as the root namespace for property binding, e.g., in YAML files.
     * For example:
     * <pre>{@code
     * reactive.ai.lite.client.openai.base-url=https://api.openai.com
     * }</pre>
     * </p>
     */
    public static final String PREFIX = "reactive.ai.lite.client.openai";

    /**
     * The default base URL for the OpenAI API.
     * <p>
     * This value is used as a fallback if no capability-specific base URL is provided.
     * It defaults to the standard OpenAI endpoint "https://api.openai.com". 
     * Override this property if using a custom proxy or a different OpenAI-compatible service.
     * </p>
     */
    private String baseUrl = "https://api.openai.com";

    /**
     * A list of authentication credentials (tokens and optional organizational/project IDs)
     * for accessing the API.
     * <p>
     * Each credential is associated with a unique profile name and one must be marked as default.
     * This list is validated at initialization to ensure at least one credential exists,
     * the default is set correctly, and profiles are uniquely defined.
     * </p>
     */
    private List<OpenaiCertification> certifications = List.of();

    /**
     * Holds the configuration for the chat capability.
     * <p>
     * Includes settings such as enabled flag, custom base URL, endpoint, and model restrictions.
     * Validation of these properties occurs when the chat capability is enabled.
     * </p>
     */
    private ChatProperties chat = new ChatProperties();

    /**
     * Holds the configuration for the embedding capability.
     * <p>
     * Similar to chat configuration, this allows fine-grained control over the embedding service
     * including enabling/disabling, custom endpoints, and model limitations.
     * </p>
     */
    private EmbeddingProperties embedding = new EmbeddingProperties();

    /**
     * Validates and completes the initialization of the properties.
     * <p>
     * If exactly one certification is present, it is automatically set as default.
     * This method then delegates to specialized methods to validate the root properties,
     * chat properties, and embedding properties to ensure consistency and correctness
     * before the application context completes its startup.
     * </p>
     *
     * @throws Exception if any property configuration is invalid
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if (certifications.size() == 1) {
            certifications.getFirst().setDefault(true);
        }
        this.checkRootProperties();
        if (chat != null && chat.isEnabled()) {
            this.checkChatProperties(chat);
        }
        if (embedding != null && embedding.isEnabled()) {
            this.checkEmbeddingProperties(embedding);
        }
    }

    /**
     * Performs validation checks on the root provider properties.
     * <p>
     * This method ensures the basic configuration integrity before the client can be used:
     * <ul>
     *   <li>{@code baseUrl} must not be empty.</li>
     *   <li>At least one certification must be provided.</li>
     *   <li>Each certification must have a non-blank token and profile.</li>
     *   <li>Exactly one certification must be marked as default.</li>
     *   <li>If multiple certifications are defined, their profiles must be unique
     *       to avoid ambiguity during credential resolution.</li>
     *   <li>If an {@code organizationId} or {@code projectId} is set, both must be set together.</li>
     * </ul>
     * These checks prevent misconfiguration that could lead to authentication failures
     * or incorrect API routing.
     * </p>
     *
     * @throws IllegalArgumentException if any of the validation rules is violated
     */
    private void checkRootProperties() {
        Assert.hasLength(this.baseUrl, "The base-url of OpenAI API is required.");
        Assert.notEmpty(this.certifications, "At least one OpenAI certification is required.");
        int defaultCertificationCount = 0;
        Set<String> profiles = new HashSet<>();
        for (OpenaiCertification certification : certifications) {
            Assert.hasText(certification.getToken(), "The token of OpenAI certification is required.");
            Assert.hasText(certification.getProfile(), "The profile of OpenAI certification is required.");
            if (certification.isDefault()) {
                defaultCertificationCount++;
            }
            profiles.add(certification.getProfile());
            if (Objects.nonNull(certification.getOrganizationId()) || Objects.nonNull(certification.getProjectId())) {
                Assert.hasText(certification.getOrganizationId(), "The organizationId is required when projectId is specified.");
                Assert.hasText(certification.getProjectId(), "The projectId is required when organizationId is specified.");
            }
        }
        Assert.isTrue(defaultCertificationCount > 0, "At least one default OpenAI certification is required.");
        Assert.isTrue(defaultCertificationCount == 1, "Only one default OpenAI certification is allowed.");
        int certificationSize = certifications.size();
        Assert.isTrue(certificationSize <= 1 || profiles.size() == certificationSize, "All OpenAI certification profiles must be unique.");
    }

    /**
     * Performs validation checks on the {@link ChatProperties}.
     * <p>
     * Ensures that when the chat capability is enabled, the necessary API endpoint
     * is properly defined. Without a valid endpoint, the client would not know
     * which API path to call for chat completions.
     * </p>
     *
     * @param chat the chat properties to validate; must not be {@code null}
     * @throws IllegalArgumentException if the endpoint is missing or empty
     */
    private void checkChatProperties(ChatProperties chat) {
        Assert.hasLength(chat.getEndpoint(), "The endpoint of OpenAI chat API is required.");
    }

    /**
     * Performs validation checks on the {@link EmbeddingProperties}.
     * <p>
     * Similar to chat, validates that the embedding endpoint is specified
     * when the embedding capability is enabled, ensuring the client can
     * construct the correct URL for embedding requests.
     * </p>
     *
     * @param embedding the embedding properties to validate; must not be {@code null}
     * @throws IllegalArgumentException if the endpoint is missing or empty
     */
    private void checkEmbeddingProperties(EmbeddingProperties embedding) {
        Assert.hasLength(embedding.getEndpoint(), "The endpoint of OpenAI embedding API is required.");
    }

    /**
     * Retrieves the active base URL for chat API requests.
     * <p>
     * If a specific base URL is configured at the chat level (i.e., {@code chat.baseUrl} is not {@code null}),
     * it takes precedence. This allows per-capability overrides, useful for scenarios where
     * different services are hosted on different domains. Otherwise, the provider-level
     * {@link #baseUrl} is used as a global fallback.
     * </p>
     *
     * @return the resolved chat base URL, never {@code null} if the global base URL is set
     */
    public String getChatBaseUrl() {
        if (Objects.nonNull(chat) && Objects.nonNull(chat.getBaseUrl())) {
            return chat.getBaseUrl();
        }
        return this.baseUrl;
    }

    /**
     * Retrieves the active base URL for embedding API requests.
     * <p>
     * Works analogously to {@link #getChatBaseUrl()}: the embedding-level override
     * takes priority if set; otherwise the global {@link #baseUrl} is returned.
     * This allows independent routing for embedding requests.
     * </p>
     *
     * @return the resolved embedding base URL, never {@code null} if the global base URL is set
     */
    public String getEmbeddingBaseUrl() {
        if (Objects.nonNull(embedding) && Objects.nonNull(embedding.getBaseUrl())) {
            return embedding.getBaseUrl();
        }
        return this.baseUrl;
    }

    /**
     * Configuration properties for the OpenAI chat capability.
     * <p>
     * Holds settings that define how the chat client interacts with the OpenAI API,
     * such as enabling/disabling this capability, customizing the endpoint,
     * restricting allowed models, and marking this provider as the default.
     * These properties are validated when the chat capability is enabled
     * to ensure a valid endpoint exists.
     * </p>
     */
    @Getter
    @Setter
    public static class ChatProperties {
        /**
         * A flag indicating if the chat capability is enabled.
         * <p>
         * When set to {@code false}, the chat client will not be initialized,
         * preventing any chat-related requests even if other chat configuration exists.
         * Defaults to {@code true} to enable chat out-of-the-box.
         * </p>
         */
        private boolean enabled = true;

        /**
         * The base URL for the chat API.
         * <p>
         * If provided, this value overrides the provider-level {@code baseUrl} for chat requests.
         * If {@code null}, the global base URL is used as fallback.
         * This is useful when the chat service is hosted on a different domain or behind a proxy.
         * </p>
         */
        private String baseUrl;

        /**
         * The endpoint for the chat completions API.
         * <p>
         * Defines the API path relative to the base URL for chat completions.
         * Defaults to the standard OpenAI path {@code "/v1/chat/completions"}.
         * Must be non-empty when the chat capability is enabled.
         * </p>
         */
        private String endpoint = "/v1/chat/completions";

        /**
         * A flag indicating if this chat provider is the default one across all clients.
         * <p>
         * Only one chat provider should typically be default; this flag can be used
         * by dependent components to decide which provider to use when no explicit choice is made.
         * Defaults to {@code true}.
         * </p>
         */
        private boolean isDefault = true;

        /**
         * A set of specific models that are allowed for chat completions.
         * <p>
         * If this set is not empty, only model names present in the set may be used
         * when constructing chat requests. If empty or {@code null}, all models are permitted.
         * This restriction can be used to enforce organizational policy or cost control.
         * </p>
         */
        private Set<String> limitedModels = Set.of();
    }

    /**
     * Configuration properties for the OpenAI embedding capability.
     * <p>
     * Similar to {@link ChatProperties}, this encapsulates the settings specific to
     * the embedding client: enable/disable, custom base URL, endpoint, default flag,
     * and model restrictions. Validation enforces endpoint presence when enabled.
     * </p>
     */
    @Getter
    @Setter
    public static class EmbeddingProperties {
        /**
         * A flag indicating if the embedding capability is enabled.
         * <p>
         * When {@code false}, the embedding client is not created, effectively disabling
         * any embedding operations. Defaults to {@code true} for convenience.
         * </p>
         */
        private boolean enabled = true;

        /**
         * The base URL for the embedding API.
         * <p>
         * If set, overrides the global {@code baseUrl} for embedding requests.
         * Leave {@code null} to use the global default.
         * </p>
         */
        private String baseUrl;

        /**
         * The endpoint for the embeddings API.
         * <p>
         * Defines the relative API path for embedding requests.
         * Defaults to the OpenAI standard {@code "/v1/embeddings"}.
         * Must be provided when embedding is enabled.
         * </p>
         */
        private String endpoint = "/v1/embeddings";

        /**
         * A flag indicating if this embedding provider is the default one across all clients.
         * <p>
         * Used by the system to select the primary embedding provider when multiple are configured.
         * Defaults to {@code true}.
         * </p>
         */
        private boolean isDefault = true;

        /**
         * A set of specific models that are allowed for embeddings.
         * <p>
         * When non-empty, restricts the embedding model to names listed in this set.
         * An empty set allows all models. This is useful for enforcing model governance.
         * </p>
         */
        private Set<String> limitedModels = Set.of();
    }

    /**
     * OpenAI certification configuration.
     * <p>
     * Represents authentication credentials and optional organizational/project scoping
     * for accessing OpenAI's API. Each certification is identified by a unique profile name
     * and can be designated as the default. The {@code capability} field allows restricting
     * a certification to only chat or only embedding, or leaving it as {@code null} for all purposes.
     * The token is required; organization/project IDs must be provided together if either is present.
     * </p>
     */
    @Getter
    @Setter
    public static class OpenaiCertification {
        /**
         * A unique name for this certification profile.
         * <p>
         * Used to differentiate multiple certifications (e.g., one per team or environment).
         * The profile name must be unique among all certifications to avoid ambiguity
         * when resolving which credentials to use. Defaults to "default-profile".
         * </p>
         */
        private String profile = "default-profile";

        /**
         * The API token for authentication with OpenAI.
         * <p>
         * This is the secret key used to authenticate all API requests associated with this profile.
         * It is mandatory and must not be blank.
         * </p>
         */
        private String token;

        /**
         * The identifier of the organization associated with the API key.
         * <p>
         * Optional; if set, the API will be scoped to this organization.
         * Must be provided together with a {@link #projectId} if either is present.
         * </p>
         */
        private String organizationId;

        /**
         * The identifier of the project associated with the API key.
         * <p>
         * Optional; allows scoping API calls to a specific project within an organization.
         * Must be provided together with an {@link #organizationId} if either is present.
         * </p>
         */
        private String projectId;

        /**
         * A flag indicating if this is the default certification to use.
         * <p>
         * Exactly one certification in the list must be marked as default.
         * When no explicit profile is selected, this certification is used.
         * Defaults to {@code false}.
         * </p>
         */
        private boolean isDefault = false;

        /**
         * The capability scope of this certification.
         * <p>
         * Determines which AI function(s) this credential is authorized for:
         * <ul>
         *   <li>{@code null}: for all capabilities (chat and embeddings)</li>
         *   <li>{@link Capability#CHAT}: only for chat completions</li>
         *   <li>{@link Capability#EMBEDDING}: only for embedding generation</li>
         * </ul>
         * This restriction is useful when separate API keys are used for different
         * services to enhance security and cost tracking.
         * </p>
         */
        private Capability capability;
    }
}