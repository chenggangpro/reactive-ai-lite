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
 * Interface representing information about a Large Language Model (LLM) provider.
 * This interface defines the contract for LLM provider implementations, including
 * provider identification, profile management, and model support capabilities.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface LlmProviderInfo {

    /**
     * Returns the name of the LLM provider.
     *
     * @return the provider name as a String
     */
    String name();

    /**
     * Returns the base URL for the LLM provider.
     *
     * @return the base URL as a String
     */
    String baseUrl();

    /**
     * Returns the endpoint for the LLM provider.
     *
     * @return the endpoint as a String
     */
    String endpoint();

    /**
     * Returns the set of profiles supported by this LLM provider.
     * Profiles typically represent different configurations or environments
     * (e.g., development, production, testing).
     *
     * @return a Set of profile names supported by this provider
     */
    Set<String> profiles();

    /**
     * Checks whether this provider supports the specified model.
     * The default implementation returns true, indicating that all models
     * are supported unless overridden by the implementing class.
     *
     * @param modelName the name of the model to check for support, must not be null
     * @return true if the model is supported, false otherwise
     */
    default boolean supportModel(@NonNull String modelName) {
        return true;
    }

    /**
     * Indicates whether this provider is the default provider.
     * The default implementation returns false, meaning the provider
     * is not the default unless explicitly overridden.
     *
     * @return true if this is the default provider, false otherwise
     */
    default boolean isDefault() {
        return false;
    }

}