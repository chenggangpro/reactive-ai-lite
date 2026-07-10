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
package pro.chenggang.project.reactive.ai.lite.core.option;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmChatProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmEmbeddingProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProvider;

/**
 * Enumerates the distinct capabilities that an AI provider or model can support within the
 * reactive-ai-lite framework.
 * <p>
 * Each capability is associated with a specific {@link LlmProvider} sub‑interface that represents
 * the contract any concrete provider must fulfill to serve that capability. This design allows
 * the system to dynamically discover, validate, and route requests to the appropriate provider
 * implementation without tightly coupling to concrete classes.
 * <p>
 * For example, when a chat request is received, the framework can look up the registered provider
 * whose class is assignable to {@link #CHAT}'s {@link #getProviderClass() provider class} and
 * delegate the call. This ensures that capabilities are decoupled from provider implementations,
 * supporting extensibility and multi‑model scenarios.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see LlmProvider
 * @see LlmChatProvider
 * @see LlmEmbeddingProvider
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum Capability {

    /**
     * Represents conversational AI capabilities: the ability to engage in text‑based, multi‑turn
     * dialogues. A provider advertising this capability must implement the
     * {@link LlmChatProvider} interface, which defines the contract for building and processing
     * chat requests and streaming responses.
     */
    CHAT(LlmChatProvider.class),

    /**
     * Represents the capability to produce vector embeddings (numerical representations) of
     * textual or other input data. Providers supporting this capability must implement
     * {@link LlmEmbeddingProvider} and are typically used for semantic search, clustering,
     * and other machine learning tasks that rely on vector similarity.
     */
    EMBEDDING(LlmEmbeddingProvider.class),

//    /**
//     * Represents audio processing capabilities, such as speech-to-text or text-to-speech.
//     */
//    AUDIO(null),
//
//    /**
//     * Represents image generation or analysis capabilities.
//     */
//    IMAGE(null),

    ;

    /**
     * The base interface class that any concrete provider must extend or implement in order to
     * be considered eligible for this capability. This field serves as a marker for dynamic
     * registration and lookup, ensuring that only providers implementing the correct contract
     * can be used to serve requests for the associated capability.
     */
    private final Class<? extends LlmProvider> providerClass;
}