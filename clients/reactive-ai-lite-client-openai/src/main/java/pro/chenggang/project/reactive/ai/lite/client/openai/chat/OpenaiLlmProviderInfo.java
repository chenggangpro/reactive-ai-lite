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
package pro.chenggang.project.reactive.ai.lite.client.openai.chat;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import pro.chenggang.project.reactive.ai.lite.core.provider.defaults.AbstractLlmProviderInfo;


/**
 * OpenAI LLM provider information implementation.
 * <p>
 * This class extends {@link AbstractLlmProviderInfo} to provide specific configuration
 * and metadata for the OpenAI language model provider. It encapsulates the provider's
 * name and other relevant information needed for OpenAI integration.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@SuperBuilder
public class OpenaiLlmProviderInfo extends AbstractLlmProviderInfo {

    /**
     * The default name identifier for the OpenAI provider.
     */
    public static final String DEFAULT_NAME = "openai";

    /**
     * The name of the OpenAI provider.
     * This field is required and cannot be null.
     */
    @NonNull
    private final String name;

    /**
     * Returns the name of the OpenAI LLM provider.
     * <p>
     * This method provides the identifier used to distinguish this provider
     * from other LLM providers in the system.
     * </p>
     *
     * @return the name of the provider, never {@code null}
     */
    @Override
    public String name() {
        return this.name;
    }

}
