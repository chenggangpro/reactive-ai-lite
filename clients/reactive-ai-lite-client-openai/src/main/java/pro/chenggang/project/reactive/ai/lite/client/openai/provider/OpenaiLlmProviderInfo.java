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
package pro.chenggang.project.reactive.ai.lite.client.openai.provider;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import pro.chenggang.project.reactive.ai.lite.core.provider.defaults.AbstractLlmProviderInfo;

/**
 * OpenAI provider information that extends the common base with a concrete
 * provider name and builder support. It serves as the canonical representation
 * of the OpenAI LLM provider within the system.
 * <p>
 * The class uses {@code @SuperBuilder} to generate a builder that can also
 * set properties inherited from {@link AbstractLlmProviderInfo} (if any).
 * The {@code name} field is mandatory and annotated with {@code @NonNull},
 * ensuring that every instance carries a valid identifier. This identifier
 * is typically used in configuration lookups, logging, or routing decisions
 * that depend on which provider is currently active.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@SuperBuilder
public class OpenaiLlmProviderInfo extends AbstractLlmProviderInfo {

    /**
     * Default provider name constant used as a well‑known identifier for the
     * OpenAI provider. This value is often used when no custom name is required,
     * for example in default configurations or as a fallback in provider
     * registration.
     */
    public static final String DEFAULT_NAME = "openai";

    /**
     * The actual name of this OpenAI provider instance. It must be supplied
     * during construction (via the builder) and is guaranteed to be non‑{@code null}
     * because of the {@code @NonNull} annotation.
     */
    @NonNull
    private final String name;

    /**
     * Returns the unique name that identifies this OpenAI provider.
     * <p>
     * This implementation satisfies the abstract contract from
     * {@link AbstractLlmProviderInfo#name()}. The returned value is exactly
     * the {@code name} field that was set through the builder. Callers can
     * use this name to distinguish between different provider instances
     * (e.g., multiple OpenAI accounts) and to select the appropriate
     * connection parameters.
     * </p>
     *
     * @return the provider name, never {@code null}
     */
    @Override
    public String name() {
        return this.name;
    }

}