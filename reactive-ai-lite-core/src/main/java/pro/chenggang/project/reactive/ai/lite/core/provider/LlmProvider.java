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
 * The foundational contract for all Large Language Model (LLM) providers within the framework.
 * <p>
 * This interface establishes a provider-agnostic abstraction layer, allowing the surrounding
 * infrastructure (such as routing, registry, and auto-configuration) to interact with any
 * LLM backend uniformly. Every implementation must be self-describing: it must declare its
 * primary {@link Capability} and expose its identity and configuration metadata via
 * {@link LlmProviderInfo}. This design decouples application logic from provider specifics,
 * enabling dynamic selection, health checking, and multi-model orchestration without
 * hardcoded dependencies.
 * </p>
 * <p>
 * Implementations typically wrap a specific AI vendor's SDK or API, translating the common
 * framework model into vendor-specific requests. The provider does not itself execute AI
 * operations; it simply describes how it can be used. The actual invocation is performed
 * by a separate operation handler that retrieves the provider from the registry using its
 * capability and metadata.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface LlmProvider {

    /**
     * Returns the primary operational capability of this LLM provider.
     * <p>
     * The capability acts as a contract label that declares the kind of AI tasks this
     * provider is designed to support (e.g., {@code CHAT}, {@code EMBEDDING}). This
     * self-classification is crucial for the provider registry to categorize and index
     * implementations. When a request dispatcher needs a provider for a specific operation,
     * it queries the registry by capability, and only providers matching that capability
     * are considered. This mechanism ensures that chat providers are never invoked for
     * embedding tasks, and vice versa, preserving type safety and pipeline integrity.
     * </p>
     * <p>
     * Although a single provider <em>could</em> support multiple capabilities, this
     * interface encourages declaring a primary capability to simplify routing. Complex
     * multi-capability providers should be registered as separate instances, each
     * advertising a distinct capability.
     * </p>
     *
     * @return the primary {@link Capability} of this provider; never {@code null}
     */
    Capability capability();

    /**
     * Returns the metadata and configuration information for this LLM provider.
     * <p>
     * The returned {@link LlmProviderInfo} object contains the provider's unique
     * identifier (often a logical name), one or more named configuration profiles, and
     * additional parsing attributes (such as vendor-specific API endpoints, model IDs,
     * or authentication details). This information is used by the framework during
     * provider initialization, health verification, and dynamic selection within
     * multi-tenant or multi-model environments. For example, the gateway may use the
     * provider's name to select a specific configuration from an external property
     * source, or to build a composite key for caching inference clients.
     * </p>
     * <p>
     * Because the provider does not communicate with the external AI service directly,
     * the configuration profiles serve as the bridge between the provider definition
     * and the actual connection factory. They enable hot-reloading of credentials,
     * seamless fallback across providers, and clear observability of which backend
     * is serving each request.
     * </p>
     *
     * @return the {@link LlmProviderInfo} containing this provider's identity and
     *         configuration; never {@code null}
     */
    LlmProviderInfo info();
}