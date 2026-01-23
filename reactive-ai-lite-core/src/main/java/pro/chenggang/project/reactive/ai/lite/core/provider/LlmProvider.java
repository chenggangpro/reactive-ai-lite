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
 * Interface for Large Language Model (LLM) providers.
 * <p>
 * This interface defines the contract for LLM provider implementations,
 * providing access to provider capabilities and information.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface LlmProvider {

    /**
     * Retrieves the capability of this LLM provider.
     * <p>
     * The capability defines what features and operations are supported
     * by this provider implementation.
     * </p>
     *
     * @return the {@link Capability} object representing the provider's capabilities
     */
    Capability capability();

    /**
     * Retrieves the information about this LLM provider.
     * <p>
     * The provider information includes metadata such as provider name,
     * version, and other identifying details.
     * </p>
     *
     * @return the {@link LlmProviderInfo} object containing provider information
     */
    LlmProviderInfo info();
}
