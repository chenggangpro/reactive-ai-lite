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
package pro.chenggang.project.reactive.ai.lite.client.anthropic.provider;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import pro.chenggang.project.reactive.ai.lite.core.provider.defaults.AbstractLlmProviderInfo;


/**
 * Concrete implementation of {@link AbstractLlmProviderInfo} that encapsulates the identity and metadata
 * required to integrate the Anthropic language model provider within the reactive AI lite framework.
 * <p>
 * In the multi‑provider architecture, each provider is identified by a unique name string. This class
 * enforces that a non‑null name is supplied at construction time—typically through the generated builder
 * (enabled by {@link SuperBuilder})—and exposes it via the parent class contract. The static constant
 * {@link #DEFAULT_NAME} defines the conventional identifier when no custom name is needed, promoting
 * consistency across application configurations.
 * <p>
 * Because the Anthropic provider may require additional configuration that extends the common
 * {@link AbstractLlmProviderInfo} supertype, this subclass is designed to be further extended via
 * {@link SuperBuilder}, preserving the builder pattern and ensuring that all mandatory fields are
 * consistently populated in a type‑safe manner.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see AbstractLlmProviderInfo
 */
@SuperBuilder
public class AnthropicLlmProviderInfo extends AbstractLlmProviderInfo {

    /**
     * The default name string used to reference the Anthropic provider when no explicit custom name is given.
     * <p>
     * This constant serves as a well‑known, stable identifier that simplifies configuration by allowing
     * the framework and client code to fall back to a standard name. Using the constant rather than a
     * hard‑coded literal reduces duplication and potential typographical errors across the codebase.
     */
    public static final String DEFAULT_NAME = "anthropic";

    /**
     * The non‑null, immutable identifier for this provider instance.
     * <p>
     * The name is the primary discriminator used by the framework to route requests to the correct
     * LLM provider. It is marked with {@link NonNull} to guarantee that a name is always provided,
     * preventing {@code null}‑related failures during provider lookup.
     */
    @NonNull
    private final String name;

    /**
     * Returns the name that uniquely identifies this Anthropic LLM provider.
     * <p>
     * This method satisfies the abstract contract defined in {@link AbstractLlmProviderInfo}, enabling
     * the framework to resolve the provider by its name. Because the {@code name} field is declared
     * as {@link NonNull}, the return value is guaranteed to be non‑null, allowing callers to safely
     * use it in maps or conditional logic without additional null‑checks.
     *
     * @return the provider identifier, never {@code null}
     */
    @Override
    public String name() {
        return this.name;
    }

}