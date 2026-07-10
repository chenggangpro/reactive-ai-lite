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
package pro.chenggang.project.reactive.ai.lite.client.deepseek.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration properties for the Deepseek AI client.
 * <p>
 * This class serves as the root container for all configuration required to connect
 * to Deepseek's API. It supports a multi‑profile certification model, allowing applications
 * to switch between different API keys and organizations without code changes. The
 * configuration hierarchy is designed so that each capability (e.g., chat) can override the
 * global base URL and manage its own endpoint and model restrictions.
 * </p>
 * <p>
 * The class implements {@link InitializingBean} to perform automatic validation and
 * consistency checks once all properties have been injected. This includes ensuring that
 * exactly one certification is marked as default, that all profiles are unique, and that
 * required fields (such as the base URL and token) are not empty. Additionally, when
 * only a single certification is provided, it is automatically designated as the default,
 * simplifying single‑key setups.
 * </p>
 * <p>
 * Users configure these properties via the {@code reactive.ai.lite.client.deepseek} prefix
 * in their application configuration files (YAML or properties). Example:
 * <pre>{@code
 * reactive:
 *   ai:
 *     lite:
 *       client:
 *         deepseek:
 *           base-url: https://api.deepseek.com
 *           certifications:
 *             - profile: production
 *               token: sk-...
 *               default: true
 *             - profile: staging
 *               token: sk-...
 * }</pre>
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see InitializingBean
 * @see DeepseekCertification
 * @see ChatProperties
 */
@Getter
@Setter
public class DeepseekClientProperties implements InitializingBean {

    /**
     * The configuration prefix for all Deepseek client properties.
     * This constant is provided for external references when building property sources.
     */
    public static final String PREFIX = "reactive.ai.lite.client.deepseek";

    /**
     * The base URL used for Deepseek API calls. All capability‑specific base URLs
     * fall back to this value if not explicitly overridden. Defaults to the official
     * Deepseek API endpoint.
     */
    private String baseUrl = "https://api.deepseek.com";

    /**
     * A list of certification profiles, each containing an API token and a unique
     * profile name. The application selects the appropriate certification based on
     * the default or a user‑specified profile. The list must contain at least one
     * entry and have exactly one entry marked as default.
     */
    private List<DeepseekCertification> certifications = List.of();

    /**
     * Configuration specific to the chat completion capability. This nested object
     * allows fine‑grained control over the chat endpoint, allowed models, and
     * whether it is the default chat provider.
     */
    private ChatProperties chat = new ChatProperties();

    /**
     * Invoked by the Spring container after all properties have been set.
     * <p>
     * This method first ensures a single certification is set as default when
     * only one certification is provided (a common single‑key scenario). Then it
     * runs a series of validation checks on the root properties and, if the chat
     * capability is enabled, on the chat sub‑properties. Validation failures result
     * in an {@link IllegalArgumentException} via Spring's {@link Assert}.
     * </p>
     *
     * @throws Exception if any validation assertion fails (caught by Spring as
     *         a bean initialization error)
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
     * Validates the root‑level provider configuration.
     * <p>
     * Checks performed:
     * <ul>
     *   <li>The base URL must not be empty.</li>
     *   <li>At least one certification must be defined.</li>
     *   <li>Each certification must have a non‑empty token and a unique profile name.</li>
     *   <li>Exactly one certification must be marked as {@code default}.</li>
     *   <li>All certification profiles must be unique (enforced when more than
     *       one certification is present).</li>
     * </ul>
     * These checks prevent ambiguous or incomplete configuration that would later
     * cause runtime errors when the client tries to select an API key or build a
     * request.
     * </p>
     */
    private void checkRootProperties() {
        Assert.hasLength(this.baseUrl, "The base-url of Deepseek API is required.");
        Assert.notEmpty(this.certifications, "At least one Deepseek certification is required.");
        int defaultCertificationCount = 0;
        Set<String> profiles = new HashSet<>();
        for (DeepseekCertification certification : certifications) {
            Assert.hasText(certification.getToken(), "The token of Deepseek certification is required.");
            Assert.hasText(certification.getProfile(), "The profile of Deepseek certification is required.");
            if (certification.isDefault()) {
                defaultCertificationCount++;
            }
            profiles.add(certification.getProfile());
        }
        Assert.isTrue(defaultCertificationCount > 0, "At least one default Deepseek certification is required.");
        Assert.isTrue(defaultCertificationCount == 1, "Only one default Deepseek certification is allowed.");
        int certificationSize = certifications.size();
        Assert.isTrue(certificationSize <= 1 || profiles.size() == certificationSize, "All Deepseek certification profiles must be unique.");
    }

    /**
     * Validates the chat‑specific sub‑configuration when the chat capability is enabled.
     * <p>
     * Currently mandates that the chat endpoint URL is provided (i.e., not empty).
     * This ensures that the client knows the exact API path for sending chat
     * requests.
     * </p>
     *
     * @param chat the chat properties object to validate
     */
    private void checkChatProperties(ChatProperties chat) {
        Assert.hasLength(chat.getEndpoint(), "The endpoint of Deepseek chat API is required.");
    }

    /**
     * Resolves the effective base URL for chat API requests.
     * <p>
     * If a dedicated {@link ChatProperties#baseUrl} is configured, it takes precedence.
     * Otherwise, the provider‑level {@link #baseUrl} is used. This fallback mechanism
     * allows per‑capability overrides while keeping the common configuration simple.
     * </p>
     *
     * @return the non‑null base URL to be used when building chat request URIs
     */
    public String getChatBaseUrl() {
        if (Objects.nonNull(chat) && Objects.nonNull(chat.getBaseUrl())) {
            return chat.getBaseUrl();
        }
        return this.baseUrl;
    }


    /**
     * Configuration properties for the Deepseek chat completion capability.
     * <p>
     * This nested class encapsulates all settings that are specific to the chat
     * feature. It allows enabling/disabling the entire capability, overriding
     * the base URL, specifying a custom endpoint (e.g., for different API versions),
     * and restricting which models are eligible for requests. The {@code isDefault}
     * flag marks this provider as the primary chat provider when multiple AI clients
     * are present.
     * </p>
     */
    @Getter
    @Setter
    public static class ChatProperties {
        /**
         * Master switch to enable or disable the chat capability. When
         * {@code false}, the chat client will not be instantiated even if other
         * properties are set. Defaults to {@code true}.
         */
        private boolean enabled = true;
        /**
         * An optional override for the base URL used exclusively by chat requests.
         * If left empty, the provider‑level {@link DeepseekClientProperties#baseUrl}
         * is used. This is handy when routing chat traffic through a proxy or
         * a different API gateway.
         */
        private String baseUrl;
        /**
         * The API endpoint path for chat completions, e.g.,
         * {@code "/chat/completions"}. This path is appended to the resolved
         * base URL to form the full request URI.
         */
        private String endpoint = "/chat/completions";
        /**
         * Indicates whether this chat provider should be treated as the default
         * when multiple AI providers are configured. The default provider will be
         * injected into primary beans unless explicitly overridden by a qualifier.
         * Defaults to {@code true}.
         */
        private boolean isDefault = true;
        /**
         * An optional white‑list of model names that are allowed for chat
         * completions. When set, only these models can be used; requests for
         * other models will be rejected early. If not specified (or empty),
         * all models supported by the backend are allowed. This provides a
         * simple mechanism for access control and cost management.
         */
        private Set<String> limitedModels;
    }

    /**
     * Deepseek certification configuration.
     * <p>
     * Represents authentication credentials and organizational information
     * for accessing Deepseek's API. Each certification is identified by a
     * unique {@code profile} string, holds an API {@code token}, and can be
     * marked as the default that will be used when no specific profile is
     * requested. This design enables multi‑tenant or multi‑key scenarios
     * within a single application.
     * </p>
     */
    @Getter
    @Setter
    public static class DeepseekCertification {
        /**
         * A unique name for this certification profile. It serves as the key
         * when programmatically selecting which credentials to use. Defaults to
         * {@code "default-profile"}.
         */
        private String profile = "default-profile";
        /**
         * The API token (Bearer token) used to authenticate requests to Deepseek.
         * This field must be provided and cannot be empty. It is typically a
         * secret such as {@code sk-...}.
         */
        private String token;
        /**
         * Marks this certification as the default one to use when no explicit
         * profile is selected. Exactly one certification in the list must have
         * this flag set to {@code true}. In a single‑certification setup the
         * flag is automatically forced to {@code true} during initialization.
         */
        private boolean isDefault = false;
    }
}