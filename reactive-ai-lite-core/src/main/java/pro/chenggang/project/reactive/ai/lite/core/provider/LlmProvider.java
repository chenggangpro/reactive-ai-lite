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

import pro.chenggang.project.reactive.ai.lite.core.option.Capability;


/**
 * Base interface for all Large Language Model (LLM) providers in the framework.
 * <p>
 * This interface defines the core contract that all provider implementations must fulfill.
 * It ensures that every provider can self-report its specific capabilities (e.g., chat, audio)
 * and its configuration metadata, allowing the registry to manage and route requests appropriately.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface LlmProvider {

    /**
     * Retrieves the primary capability of this LLM provider.
     * <p>
     * The capability defines what type of operations (e.g., CHAT, EMBEDDING) this specific
     * provider implementation is designed to handle. This is used for routing requests to
     * the correct provider type.
     * </p>
     *
     * @return the {@link Capability} representing the provider's primary function
     */
    Capability capability();

    /**
     * Retrieves metadata and configuration information about this LLM provider.
     * <p>
     * The provider information includes details such as the provider's unique name,
     * available configuration profiles, and other identifying parsingAttributes necessary for
     * dynamic provider selection.
     * </p>
     *
     * @return the {@link LlmProviderInfo} object containing provider metadata
     */
    LlmProviderInfo info();
}
