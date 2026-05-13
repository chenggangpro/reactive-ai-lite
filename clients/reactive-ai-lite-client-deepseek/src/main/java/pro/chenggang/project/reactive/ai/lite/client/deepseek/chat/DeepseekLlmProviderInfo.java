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
package pro.chenggang.project.reactive.ai.lite.client.deepseek.chat;

import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import pro.chenggang.project.reactive.ai.lite.core.provider.defaults.AbstractLlmProviderInfo;


/**
 * Deepseek LLM provider information implementation.
 * <p>
 * This class extends {@link AbstractLlmProviderInfo} to provide specific configuration
 * and metadata for the Deepseek language model provider. It encapsulates the provider's
 * name and other relevant information needed for Deepseek integration.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@SuperBuilder
public class DeepseekLlmProviderInfo extends AbstractLlmProviderInfo {

    /**
     * The default name identifier for the Deepseek provider.
     */
    public static final String DEFAULT_NAME = "deepseek";

    /**
     * The name of the Deepseek provider.
     * This field is required and cannot be null.
     */
    @NonNull
    private final String name;

    /**
     * Returns the name of the Deepseek LLM provider.
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
