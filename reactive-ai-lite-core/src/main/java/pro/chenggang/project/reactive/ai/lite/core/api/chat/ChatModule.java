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

import pro.chenggang.project.reactive.ai.lite.core.api.LlmModule;
import pro.chenggang.project.reactive.ai.lite.core.spec.ExecutionContextSpec;

/**
 * Chat module interface that extends the base LLM module functionality.
 * <p>
 * This interface provides chat-specific operations for interacting with language models,
 * including the ability to create new completion contexts for chat interactions.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface ChatModule extends LlmModule {

    /**
     * Creates a new execution context for chat completion operations.
     * <p>
     * This method initializes and returns a new execution context specification that can be used
     * to configure and execute chat completion requests. The context encapsulates the necessary
     * configuration and state for processing chat interactions with the language model.
     * </p>
     *
     * @return a new {@link ExecutionContextSpec} instance configured for chat completion operations
     */
    ExecutionContextSpec newCompletionContext();

}
