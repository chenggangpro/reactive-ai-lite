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
package pro.chenggang.project.reactive.ai.lite.client.ollama.properties;

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
 * Central configuration container for the Ollama reactive AI client.
 * <p>
 * This class aggregates all hierarchical properties needed to connect to and authenticate with
 * Ollama's API services. It implements {@link InitializingBean} to perform comprehensive
 * validation of the configuration after Spring property binding is complete.
 * </p>
 * <p>
 * Key features:
 * <ul>
 *   <li><b>Base URL cascade:</b> A global {@code baseUrl} serves as the fallback for each capability.
 *       Chat and embedding endpoints can override this via their own {@code baseUrl} settings.</li>
 *   <li><b>Capability separation:</b> Chat and embedding features are configured independently,
 *       each with its own enabled flag, endpoint path, and optional model restrictions.</li>
 *   <li><b>Multiple certifications:</b> Several API tokens can be supplied, each associated with a
 *       unique profile name and an optional {@link Capability} restriction. Exactly one certification
 *       must be marked as default when multiple are present.</li>
 *   <li><b>Automatic defaults:</b> If only one certification is provided, it is automatically
 *       designated as the default.</li>
 * </ul>
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Getter
@Setter
public class OllamaClientProperties implements InitializingBean {

    /**
     * Configuration property prefix for all Ollama client settings.
     * <p>
     * Used by Spring's property binding to map external configuration
     * (e.g., {@code reactive.ai.lite.client.ollama.*}) to this class.
     */
    public static final String PREFIX = "reactive.ai.lite.client.ollama";

    /**
     * The default base URL for the Ollama API.
     * <p>
     * Defaults to {@code http://localhost:11434}, the standard Ollama server address.
     * This value is used as a fallback for any capability that does not define its own
     * {@code baseUrl}. Setting this property ensures all endpoints share the same host
     * unless explicitly overridden.
     * </p>
     */
    private String baseUrl = "http://localhost:11434";

    /**
     * A list of authentication credentials for accessing Ollama's API.
     * <p>
     * Each entry in this list defines a token, a profile name, and an optional capability
     * restriction. The client can switch between credentials based on the required model
     * capability. When multiple certifications are present, exactly one must be marked as default
     * (either automatically or via the {@code isDefault} flag). This design supports
     * multi‑tenancy and token rotation scenarios.
     * </p>
     * @see OllamaCertification
     */
    private List<OllamaCertification> certifications = List.of();

    /**
     * Configuration for the chat completion capability.
     * <p>
     * Controls whether the chat feature is enabled, defines the API endpoint path,
     * optional base URL override, and allowed models. If the chat {@code enabled} flag is
     * {@code false}, the corresponding client component is not instantiated.
     * </p>
     */
    private ChatProperties chat = new ChatProperties();

    /**
     * Configuration for the embedding generation capability.
     * <p>
     * Similar to {@link #chat}, but for embedding endpoints. By default, embeddings are
     * disabled ({@code enabled = false}) to explicitly require opt‑in configuration.
     * </p>
     */
    private EmbeddingProperties embedding = new EmbeddingProperties();

    /**
     * Validates the configuration and auto‑assigns a default certification when appropriate.
     * <p>
     * After all property values have been injected, this method:
     * <ol>
     *   <li>Sets the single certification as default if exactly one is defined.</li>
     *   <li>Invokes {@link #checkRootProperties()} to validate base URL and certifications.</li>
     *   <li>If chat is enabled, calls {@link #checkChatProperties(ChatProperties)}.</li>
     *   <li>If embedding is enabled, calls {@link #checkEmbeddingProperties(EmbeddingProperties)}.</li>
     * </ol>
     * This ensures the application context fails fast on misconfiguration.
     * </p>
     *
     * @throws IllegalArgumentException if any validation fails.
     * @throws Exception if {@link InitializingBean} contract requires; only {@link IllegalArgumentException} is thrown.
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
     * Validates root‑level properties: base URL and certification constraints.
     * <p>
     * Asserts that:
     * <ul>
     *   <li>The {@code baseUrl} is not empty.</li>
     *   <li>If certifications are provided, each must have a non‑blank token and profile name.</li>
     *   <li>Exactly one default certification exists when certifications are present.</li>
     *   <li>All certification profiles are unique.</li>
     * </ul>
     * These rules enforce a consistent authentication setup and avoid ambiguous token selection.
     * </p>
     *
     * @throws IllegalArgumentException if any constraint is violated.
     */
    private void checkRootProperties() {
        Assert.hasLength(this.baseUrl, "The base-url of Ollama API is required.");
        if (CollectionUtils.isEmpty(this.certifications)) {
            return;
        }
        int defaultCertificationCount = 0;
        Set<String> profiles = new HashSet<>();
        for (OllamaCertification certification : certifications) {
            Assert.hasText(certification.getToken(), "The token of Ollama certification is required.");
            Assert.hasText(certification.getProfile(), "The profile of Ollama certification is required.");
            if (certification.isDefault()) {
                defaultCertificationCount++;
            }
            profiles.add(certification.getProfile());
        }
        Assert.isTrue(defaultCertificationCount > 0, "At least one default Ollama certification is required when certifications are provided.");
        Assert.isTrue(defaultCertificationCount == 1, "Only one default Ollama certification is allowed.");
        int certificationSize = certifications.size();
        Assert.isTrue(certificationSize <= 1 || profiles.size() == certificationSize, "All Ollama certification profiles must be unique.");
    }

    /**
     * Validates the chat capability’s endpoint property.
     *
     * @param chat the chat properties to validate; must not be null.
     * @throws IllegalArgumentException if the chat endpoint is empty.
     */
    private void checkChatProperties(ChatProperties chat) {
        Assert.hasLength(chat.getEndpoint(), "The endpoint of Ollama chat API is required.");
    }

    /**
     * Validates the embedding capability’s endpoint property.
     *
     * @param embedding the embedding properties to validate; must not be null.
     * @throws IllegalArgumentException if the embedding endpoint is empty.
     */
    private void checkEmbeddingProperties(EmbeddingProperties embedding) {
        Assert.hasLength(embedding.getEndpoint(), "The endpoint of Ollama embedding API is required.");
    }

    /**
     * Resolves the effective base URL for chat requests.
     * <p>
     * Follows a cascade: if the chat capability defines its own {@code baseUrl} (non‑null),
     * that value is returned; otherwise, the global {@link #baseUrl} is used.
     * This allows per‑capability server overrides without duplicating the common host.
     * </p>
     *
     * @return the chat base URL, never null.
     */
    public String getChatBaseUrl() {
        if (Objects.nonNull(chat) && Objects.nonNull(chat.getBaseUrl())) {
            return chat.getBaseUrl();
        }
        return this.baseUrl;
    }

    /**
     * Resolves the effective base URL for embedding requests.
     * <p>
     * Works identically to {@link #getChatBaseUrl()} but for the embedding capability.
     * </p>
     *
     * @return the embedding base URL, never null.
     */
    public String getEmbeddingBaseUrl() {
        if (Objects.nonNull(embedding) && Objects.nonNull(embedding.getBaseUrl())) {
            return embedding.getBaseUrl();
        }
        return this.baseUrl;
    }

    /**
     * Configuration properties specific to the Ollama chat completion capability.
     * <p>
     * Allows fine‑grained control over the chat API endpoint, enabling/disabling the feature,
     * overriding the base URL, and restricting the set of models that can be used.
     * The {@code isDefault} flag indicates whether this provider should be selected as the
     * primary chat client when multiple implementations are present in the context.
     * </p>
     */
    @Getter
    @Setter
    public static class ChatProperties {

        /**
         * Whether the chat capability is enabled.
         * <p>
         * When {@code true} (the default), the corresponding chat client bean is created.
         * Setting this to {@code false} effectively disables chat functionality without
         * requiring the removal of other configuration.
         * </p>
         */
        private boolean enabled = true;

        /**
         * An optional base URL override for chat endpoints.
         * <p>
         * If specified, it supersedes the global {@link OllamaClientProperties#baseUrl}
         * when building chat request URIs. This is useful when chat and embedding services
         * are deployed on different hosts or ports.
         * </p>
         */
        private String baseUrl;

        /**
         * The relative path of the chat completions API.
         * <p>
         * Defaults to {@code /api/chat}, the standard Ollama chat endpoint.
         * This path is appended to the resolved base URL.
         * </p>
         */
        private String endpoint = "/api/chat";

        /**
         * Marker indicating whether this chat provider should be considered the default
         * among multiple implementations.
         * <p>
         * In a multi‑provider setup, the one with {@code isDefault = true} will be used
         * when no explicit provider is specified. Only one chat provider should be marked
         * as default; otherwise selection may be non‑deterministic.
         * </p>
         */
        private boolean isDefault = true;

        /**
         * An optional set of model names that this chat provider is allowed to handle.
         * <p>
         * If empty or {@code null}, all models are permitted. When populated, only
         * requests for the listed models will be routed to this provider, enabling
         * model‑level access control.
         * </p>
         */
        private Set<String> limitedModels;
    }

    /**
     * Configuration properties specific to the Ollama embedding capability.
     * <p>
     * Mirrors the structure of {@link ChatProperties} but for embedding endpoints.
     * By default, embeddings are disabled to ensure explicit opt‑in.
     * </p>
     */
    @Getter
    @Setter
    public static class EmbeddingProperties {

        /**
         * Whether the embedding capability is enabled.
         * <p>
         * Defaults to {@code false}. Because embedding models often require different
         * resources, this default forces developers to intentionally enable the feature.
         * </p>
         */
        private boolean enabled = false;

        /**
         * An optional base URL override for embedding endpoints.
         * <p>
         * Same behavior as {@link ChatProperties#baseUrl}, applied to embedding requests.
         * </p>
         */
        private String baseUrl;

        /**
         * The relative path of the embeddings API.
         * <p>
         * Defaults to {@code /api/embed}, the standard Ollama embedding endpoint.
         * </p>
         */
        private String endpoint = "/api/embed";

        /**
         * Marker indicating whether this embedding provider should be considered the default
         * among multiple implementations.
         * <p>
         * Analogous to {@link ChatProperties#isDefault}.
         * </p>
         */
        private boolean isDefault = true;

        /**
         * An optional set of model names that this embedding provider is allowed to handle.
         * <p>
         * Same semantics as {@link ChatProperties#limitedModels}.
         * </p>
         */
        private Set<String> limitedModels;
    }

    /**
     * Represents a named authentication credential for Ollama API access.
     * <p>
     * Each certification holds an API token and a profile identifier. The optional
     * {@link Capability} field restricts the credential to a specific feature (chat or
     * embedding), enabling separate tokens for different service tiers or usage limits.
     * Exactly one certification must be marked as the default when multiple are defined;
     * the default is used when no explicit profile matches a request.
     * </p>
     * <p>
     * Example:
     * <pre>
     * certifications:
     *   - profile: "chat-only"
     *     token: "sk-123"
     *     capability: CHAT
     *     isDefault: true
     *   - profile: "embedding-only"
     *     token: "sk-456"
     *     capability: EMBEDDING
     * </pre>
     * Here, the "chat-only" credential is the default and can only serve chat, while the
     * "embedding-only" credential is used for embedding tasks.
     * </p>
     */
    @Getter
    @Setter
    public static class OllamaCertification {

        /**
         * Unique profile name for this certification.
         * <p>
         * Used to reference this credential in configurations, for example when selecting
         * a specific certification for a given model. Must be non‑blank and unique across
         * all certifications.
         * </p>
         */
        private String profile = "default-profile";

        /**
         * The API token required to authenticate with Ollama.
         * <p>
         * This token is sent in the request headers (typically as a Bearer token)
         * when communicating with the Ollama server. Must not be blank.
         * </p>
         */
        private String token;

        /**
         * Whether this certification is the default.
         * <p>
         * When multiple certifications exist, exactly one must be set as default.
         * The default credential is used when no profile override is specified.
         * If only a single certification is provided, it is automatically flagged
         * as default during initialization.
         * </p>
         */
        private boolean isDefault = false;

        /**
         * Restricts this certification to a specific capability.
         * <ul>
         *   <li>{@code null} – the credential can be used for any capability (chat and embedding).</li>
         *   <li>{@link Capability#CHAT} – only chat requests can use this token.</li>
         *   <li>{@link Capability#EMBEDDING} – only embedding requests can use this token.</li>
         * </ul>
         * <p>
         * This separation is useful when different tokens have different rate limits
         * or permissions for chat vs embedding operations.
         * </p>
         */
        private Capability capability;
    }
}