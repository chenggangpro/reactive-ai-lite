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

import pro.chenggang.project.reactive.ai.lite.core.api.chat.ChatModule;

/**
 * Reactive LLM (Large Language Model) client interface for interacting with AI services.
 * <p>
 * This interface provides a reactive, non-blocking API for communicating with LLM providers.
 * It serves as the main entry point for accessing various LLM functionalities through
 * specialized modules.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface ReactiveLlmClient {

    /**
     * Retrieves the chat module for performing chat-based interactions with the LLM.
     * <p>
     * The chat module provides methods for sending messages and receiving responses
     * from the language model in a conversational context.
     * </p>
     *
     * @return the {@link ChatModule} instance for chat operations
     */
    ChatModule chat();

}
