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
package pro.chenggang.project.reactive.ai.lite.core.api;

import lombok.RequiredArgsConstructor;
import pro.chenggang.project.reactive.ai.lite.core.api.chat.ChatModule;
import pro.chenggang.project.reactive.ai.lite.core.api.chat.DefaultChatModule;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;

/**
 * The default implementation of the {@link ReactiveLlmClient} interface.
 * <p>
 * This class provides the standard, out-of-the-box implementation for
 * interacting with various LLM providers in a reactive manner. It relies
 * on an {@link LlmProviderRegistry} to manage and access different
 * provider implementations.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@RequiredArgsConstructor
public class DefaultReactiveLlmClient implements ReactiveLlmClient {

    /**
     * The registry used to look up and manage available LLM providers.
     */
    private final LlmProviderRegistry llmProviderRegistry;

    /**
     * Retrieves a new instance of the default chat module.
     * <p>
     * This method instantiates and returns a {@link DefaultChatModule},
     * configuring it with the underlying {@link LlmProviderRegistry} to
     * enable chat interactions.
     * </p>
     *
     * @return a new {@link ChatModule} instance for chat operations
     */
    @Override
    public ChatModule chat() {
        return new DefaultChatModule(llmProviderRegistry);
    }
}
