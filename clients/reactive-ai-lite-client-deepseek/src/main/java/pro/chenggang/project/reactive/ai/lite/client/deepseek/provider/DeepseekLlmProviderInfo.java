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
package pro.chenggang.project.reactive.ai.lite.client.deepseek.provider;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import pro.chenggang.project.reactive.ai.lite.core.provider.defaults.AbstractLlmProviderInfo;

/**
 * Deepseek LLM provider information model.
 * <p>
 * Extends {@link AbstractLlmProviderInfo} to encapsulate the identity of a
 * Deepseek-based language model provider. The provider's name serves as a
 * unique key for routing requests and resolving configurations within the
 * reactive AI client framework. Instances are typically constructed via the
 * builder pattern (enabled by Lombok's {@link SuperBuilder}) and must supply a
 * non-null name. The constant {@link #DEFAULT_NAME} provides a standard default
 * value, simplifying initial setup for common use cases.
 * </p>
 * <p>
 * This class may be subclassed in the future to include additional
 * Deepseek-specific metadata such as endpoint overrides or capability flags.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@SuperBuilder
public class DeepseekLlmProviderInfo extends AbstractLlmProviderInfo {

    /**
     * The default name identifier for the Deepseek provider.
     * <p>
     * This constant represents the canonical name used to identify the Deepseek
     * language model in typical application configurations. It can be referenced
     * when building a {@link DeepseekLlmProviderInfo} instance, or when
     * programmatically selecting the provider from a collection.
     * </p>
     */
    public static final String DEFAULT_NAME = "deepseek";

    /**
     * The unique name of this Deepseek provider instance.
     * <p>
     * This name distinguishes this provider from others when multiple LLM
     * providers are active. It is normally set to {@link #DEFAULT_NAME}, but
     * may be customized (e.g., "deepseek-china" or "deepseek-eu") to support
     * multiple deployments with different endpoints or credentials. The field
     * is required (non-null) and must be supplied during builder construction.
     * </p>
     */
    @NonNull
    private final String name;

    /**
     * Returns the name of this Deepseek provider.
     * <p>
     * Overrides the abstract {@link AbstractLlmProviderInfo#name()} to provide
     * the concrete identifier. The returned value is guaranteed non-null and
     * should be used as the provider key in routing, logging, and configuration
     * lookups.
     * </p>
     *
     * @return the provider name, never {@code null}
     */
    @Override
    public String name() {
        return this.name;
    }
}