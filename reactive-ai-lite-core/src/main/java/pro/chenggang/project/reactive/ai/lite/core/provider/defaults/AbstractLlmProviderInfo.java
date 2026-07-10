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
package pro.chenggang.project.reactive.ai.lite.core.provider.defaults;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;

import java.util.Objects;
import java.util.Set;

/**
 * An abstract base implementation of {@link LlmProviderInfo} that encapsulates
 * common provider metadata and default behaviors.
 * <p>
 * This class stores the essential properties of an LLM provider—base URL, endpoint,
 * active profiles, and a set of explicitly supported models. The model support
 * logic uses a <em>broad‑support fallback</em>: if no supported models are
 * configured (i.e., the set is empty), any model is considered supported. This
 * allows concrete providers to opt out of model filtering entirely while still
 * adhering to the contractual interface.
 * </p>
 * <p>
 * Instances are typically created via the Lombok {@code @SuperBuilder}‑generated
 * builder, which enforces the mandatory profiles set and provides a fluent API
 * for setting other properties. Concrete subclasses must implement the
 * {@link LlmProviderInfo#name()} method to provide a unique identifier for the
 * provider.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see LlmProviderInfo
 */
@Slf4j
@SuperBuilder
public abstract class AbstractLlmProviderInfo implements LlmProviderInfo {

    /**
     * The base URL of the provider's API, for example {@code https://api.openai.com}.
     * This value is used as the root when constructing complete request URIs together
     * with the {@link #endpoint}.
     */
    protected final String baseUrl;

    /**
     * The specific API endpoint path relative to the {@link #baseUrl}, such as
     * {@code /v1/chat/completions}. Together with the base URL it forms the full
     * service URL for LLM interactions.
     */
    protected final String endpoint;

    /**
     * Whether this provider should be treated as the default when multiple
     * providers are configured. Resolving logic may prefer the default provider
     * if no explicit model‑to‑provider mapping exists.
     */
    protected final boolean isDefault;

    /**
     * The set of Spring‑style profile names for which this provider is activated.
     * This enables conditional registration and selection of providers based on
     * the active runtime environment.
     */
    protected final Set<String> profiles;

    /**
     * An explicit set of model names that are advertised as supported by this
     * provider. If the set is empty (either because no models were supplied or
     * a {@code null} or empty collection was passed), the implementation assumes
     * <em>all</em> models are supported, simplifying configuration for providers
     * that do not need to restrict model availability.
     */
    protected final Set<String> supportedModels;

    /**
     * Constructs a new {@link AbstractLlmProviderInfo} instance, normally invoked
     * by the Lombok‑generated builder.
     * <p>
     * The {@code supportedModels} parameter is handled specially: if it is
     * non‑null and non‑empty, an immutable copy is stored; otherwise an empty
     * set is assigned and a log message is emitted to signal that the provider
     * will accept any model.
     * </p>
     *
     * @param baseUrl         the API base URL, may be {@code null} if not applicable
     * @param endpoint        the API endpoint, may be {@code null} if not applicable
     * @param isDefault       {@code true} if this provider is the default
     * @param profiles        the set of profile names; must not be {@code null}
     * @param supportedModels the set of explicitly supported model names, or
     *                        {@code null}/empty to indicate all models are supported
     */
    protected AbstractLlmProviderInfo(String baseUrl, String endpoint, boolean isDefault, @NonNull Set<String> profiles, Set<String> supportedModels) {
        this.baseUrl = baseUrl;
        this.endpoint = endpoint;
        this.isDefault = isDefault;
        this.profiles = profiles;
        if (Objects.nonNull(supportedModels) && !supportedModels.isEmpty()) {
            this.supportedModels = Set.copyOf(supportedModels);
        } else {
            log.info("No supported models provided. Assuming all models are supported.");
            this.supportedModels = Set.of();
        }
    }

    /**
     * Determines whether a given model name is supported by this provider.
     * <p>
     * The check follows a broad‑support fallback strategy:
     * <ul>
     *   <li>If no supported models were configured (i.e., the internal
     *       {@code supportedModels} set is empty), the method returns
     *       {@code true}, assuming the provider is capable of handling any model.</li>
     *   <li>Otherwise, only explicit membership in the set is considered support.</li>
     * </ul>
     * This design enables providers that do not wish to restrict model usage to
     * bypass explicit model lists without additional configuration.
     * </p>
     *
     * @param modelName the name of the model to check; must not be {@code null}
     * @return {@code true} if the model is supported, {@code false} otherwise
     */
    @Override
    public boolean supportModel(@NonNull String modelName) {
        if (supportedModels.isEmpty()) {
            return true;
        }
        return supportedModels.contains(modelName);
    }

    /**
     * Returns the API base URL configured for this provider.
     *
     * @return the base URL string, or {@code null} if not set
     */
    @Override
    public String baseUrl() {
        return this.baseUrl;
    }

    /**
     * Returns the API endpoint path that, together with the {@link #baseUrl()},
     * forms the complete address for LLM requests.
     *
     * @return the endpoint string, or {@code null} if not set
     */
    @Override
    public String endpoint() {
        return this.endpoint;
    }

    /**
     * Returns the set of profile names assigned to this provider. The profiles
     * can be used to conditionally activate or select the provider based on the
     * runtime environment.
     *
     * @return the set of profile names (non‑null)
     */
    @Override
    public Set<String> profiles() {
        return this.profiles;
    }

    /**
     * Indicates whether this provider is designated as the default among multiple
     * configured providers.
     *
     * @return {@code true} if this is the default provider, {@code false} otherwise
     */
    @Override
    public boolean isDefault() {
        return this.isDefault;
    }

    /**
     * Returns a comprehensive string representation of this provider's metadata,
     * including its name, profiles, supported models, default flag, base URL,
     * and endpoint. Useful for debugging and logging.
     *
     * @return a formatted string containing all provider properties
     */
    @Override
    public String toString() {
        return "LlmProviderInfo{name=" + name() + ", profiles=" + profiles + ", supportedModels=" + supportedModels + ", isDefault=" + isDefault + ", baseUrl=" + this.baseUrl + ", endpoint=" + endpoint + "}";
    }
}