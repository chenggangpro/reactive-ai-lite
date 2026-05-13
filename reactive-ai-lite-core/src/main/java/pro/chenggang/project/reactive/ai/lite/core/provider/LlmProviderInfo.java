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
 * This interface allows the framework to interrogate a provider about its identity,
 * its connection endpoints, and the profiles and models it supports without having
 * to invoke an actual request. This is crucial for dynamic provider selection and
 * routing.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface LlmProviderInfo {

    /**
     * Returns the unique name or identifier of the LLM provider.
     * <p>
     * E.g., "openai", "anthropic", "ollama". This is often used in configuration
     * files or when explicitly selecting a provider by name.
     * </p>
     *
     * @return the provider name as a String
     */
    String name();

    /**
     * Returns the base URL for the LLM provider's API.
     * <p>
     * E.g., "https://api.openai.com/v1".
     * </p>
     *
     * @return the base URL as a String
     */
    String baseUrl();

    /**
     * Returns the specific API endpoint used for requests related to this provider's capability.
     * <p>
     * E.g., "/chat/completions". This is appended to the base URL to form the full
     * request URI.
     * </p>
     *
     * @return the endpoint path as a String
     */
    String endpoint();

    /**
     * Returns the set of configuration profiles supported by this LLM provider.
     * <p>
     * Profiles represent different sets of credentials or environments configured for
     * the same provider (e.g., "default", "production", "testing"). A provider must
     * have at least one profile (usually "default") to be usable.
     * </p>
     *
     * @return a Set of profile names supported by this provider
     */
    Set<String> profiles();

    /**
     * Checks whether this provider instance supports the specified AI model.
     * <p>
     * Some providers restrict access to certain models, or different instances of the
     * same provider (e.g., different self-hosted Ollama instances) might have different
     * models loaded. The default implementation assumes the provider supports all requested models.
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
     * When multiple providers offer the same capability (e.g., CHAT), the framework uses
     * this flag to select the default one when no specific provider is requested.
     * </p>
     *
     * @return {@code true} if this is the default provider, {@code false} otherwise
     */
    default boolean isDefault() {
        return false;
    }

}
