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
 * An abstract base class for implementing {@link LlmProviderInfo}.
 * <p>
 * This class provides common state and logic for managing provider metadata.
 * It handles the storage of base URLs, endpoints, profiles, and a predefined
 * list of supported models. It implements the default logic for checking model
 * support and default provider status.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Slf4j
@SuperBuilder
public abstract class AbstractLlmProviderInfo implements LlmProviderInfo {

    /**
     * The base URL for the provider's API.
     */
    protected final String baseUrl;

    /**
     * The specific endpoint path for the provider's capability.
     */
    protected final String endpoint;

    /**
     * Flag indicating if this is the default provider.
     */
    protected final boolean isDefault;

    /**
     * The set of configured profiles for this provider.
     */
    protected final Set<String> profiles;

    /**
     * A set of explicitly supported model names. If empty, all models are assumed to be supported.
     */
    protected final Set<String> supportedModels;

    /**
     * Constructs a new {@link AbstractLlmProviderInfo}.
     *
     * @param baseUrl         the API base URL
     * @param endpoint        the API endpoint
     * @param isDefault       whether this is the default provider
     * @param profiles        the set of available profiles
     * @param supportedModels the set of supported models, or empty/null if all are supported
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
     * Checks if a given model is supported by this provider.
     * <p>
     * If the {@code supportedModels} set is empty, it assumes all models are supported.
     * Otherwise, it checks if the model name is contained within the set.
     * </p>
     *
     * @param modelName the name of the model to check
     * @return true if supported, false otherwise
     */
    @Override
    public boolean supportModel(@NonNull String modelName) {
        if (supportedModels.isEmpty()) {
            return true;
        }
        return supportedModels.contains(modelName);
    }

    /**
     * Returns the base URL.
     *
     * @return the base URL string
     */
    @Override
    public String baseUrl() {
        return this.baseUrl;
    }

    /**
     * Returns the endpoint.
     *
     * @return the endpoint string
     */
    @Override
    public String endpoint() {
        return this.endpoint;
    }

    /**
     * Returns the set of profiles.
     *
     * @return the set of profile names
     */
    @Override
    public Set<String> profiles() {
        return this.profiles;
    }

    /**
     * Returns whether this is the default provider.
     *
     * @return true if default, false otherwise
     */
    @Override
    public boolean isDefault() {
        return this.isDefault;
    }

    /**
     * Returns a string representation of the provider info.
     *
     * @return a formatted string containing the provider's metadata
     */
    @Override
    public String toString() {
        return "LlmProviderInfo{name=" + name() + ", profiles=" + profiles + ", supportedModels=" + supportedModels + ", isDefault=" + isDefault + ", baseUrl=" + this.baseUrl + ", endpoint=" + endpoint + "}";
    }
}
