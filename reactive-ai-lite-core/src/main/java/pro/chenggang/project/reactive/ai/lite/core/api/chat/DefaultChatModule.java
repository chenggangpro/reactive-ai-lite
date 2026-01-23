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
package pro.chenggang.project.reactive.ai.lite.core.api.chat;

import lombok.RequiredArgsConstructor;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import pro.chenggang.project.reactive.ai.lite.core.spec.ExecutionContextSpec;
import pro.chenggang.project.reactive.ai.lite.core.spec.defaults.DefaultExecutionContextSpec;

/**
 * Default implementation of the ChatModule interface.
 * <p>
 * This class provides the standard chat module functionality for the reactive AI lite framework.
 * It manages the creation of execution contexts for chat-based LLM operations and maintains
 * a reference to the LLM provider registry for provider lookups.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@RequiredArgsConstructor
public class DefaultChatModule implements ChatModule {

    private final LlmProviderRegistry llmProviderRegistry;

    @Override
    public LlmClientType type() {
        return LlmClientType.CHAT;
    }

    @Override
    public ExecutionContextSpec newCompletionContext() {
        return DefaultExecutionContextSpec.of(type(), llmProviderRegistry);
    }
}
