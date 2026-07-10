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
package pro.chenggang.project.reactive.ai.lite.core.provider;

import lombok.NonNull;

import java.util.Set;

/**
 * Provides static metadata and configuration details about an LLM provider.
 * <p>
 * In a reactive AI framework, multiple providers can be configured, each offering
 * different capabilities (e.g., chat, embedding, image generation). To route requests
 * to the correct provider without instantiating full client objects, the framework
 * needs a lightweight, read‑only description of each provider. This interface exposes
 * that description: the provider’s identity, its API endpoint structure, the named
 * configuration profiles it supports, and its model availability. Implementations
 * are typically backed by configuration properties and are used in dynamic provider
 * selection, default provider fallback, and model‑compatibility checks.
 * </p>
 * <p>
 * All methods must return consistent, immutable data for the lifetime of the
 * implementing object. The default methods provide sensible defaults that fit
 * common simple setups (supports all models, not a default provider).
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface LlmProviderInfo {

    /**
     * Returns the unique name or identifier of the LLM provider.
     * <p>
     * The name is used as a discriminator in configuration files, API endpoints,
     * and when explicitly selecting a provider in application code. It should be
     * short, lowercase, and without spaces (e.g., "openai", "ollama"). The
     * framework may use this name to look up additional provider‑specific settings
     * or to log the active provider.
     * </p>
     *
     * @return the provider name as a String, never null
     */
    String name();

    /**
     * Returns the base URL for the LLM provider's API.
     * <p>
     * All requests to this provider are constructed by appending the {@link #endpoint()}
     * to this base URL. For cloud providers this is usually a fixed address (e.g.,
     * "https://api.openai.com/v1"), while for self‑hosted instances (like Ollama) it
     * may point to a local or custom domain. The framework uses this URL to build
     * the complete request URI when executing a model interaction.
     * </p>
     *
     * @return the base URL as a String, never null
     */
    String baseUrl();

    /**
     * Returns the specific API endpoint used for requests related to this provider's capability.
     * <p>
     * This path is appended to the {@link #baseUrl()} to form the full request URI.
     * For example, a chat capability might return "/chat/completions", an embedding
     * capability "/embeddings". The separation of base URL and endpoint allows the
     * same provider to support multiple capabilities (each backed by a different
     * {@code LlmProviderInfo} instance) while reusing the same base URL.
     * </p>
     *
     * @return the endpoint path as a String, never null
     */
    String endpoint();

    /**
     * Returns the set of configuration profiles supported by this LLM provider.
     * <p>
     * Profiles represent different sets of credentials, environment settings, or
     * organizational tenants that can be used with the same underlying API. A single
     * provider can have, for example, "default", "production", and "development"
     * profiles. The framework selects a profile based on the current execution context
     * to obtain the correct API key, timeout, or other runtime parameters. This
     * method must return at least one entry (typically "default") for the provider
     * to be considered usable.
     * </p>
     *
     * @return a Set of profile names, never null, containing at least one element
     */
    Set<String> profiles();

    /**
     * Checks whether this provider instance supports the specified AI model.
     * <p>
     * Some providers restrict access to certain models based on subscription or
     * deployment. In self‑hosted scenarios (e.g., Ollama), the set of available
     * models is dynamic and depends on what is loaded. This method allows the
     * framework to verify compatibility before sending a request, avoiding
     * unnecessary network calls and providing clearer error messages. The default
     * implementation returns {@code true} to indicate that the provider is model‑agnostic
     * and accepts any model name sent to it.
     * </p>
     *
     * @param modelName the name of the model to check, must not be null
     * @return {@code true} if the model is supported, {@code false} otherwise
     */
    default boolean supportModel(@NonNull String modelName) {
        return true;
    }

    /**
     * Indicates whether this provider is the primary or default choice within its capability category.
     * <p>
     * When multiple providers are registered for the same capability (e.g., CHAT),
     * the framework needs a fallback provider to use when no explicit provider name
     * is specified in a request. This method designates exactly one provider per
     * capability as the default. Override this method and return {@code true} in the
     * implementation that should be used as the automatic fallback. The default
     * implementation returns {@code false}, meaning the provider is only used when
     * explicitly selected.
     * </p>
     *
     * @return {@code true} if this is the default provider, {@code false} otherwise
     */
    default boolean isDefault() {
        return false;
    }
}