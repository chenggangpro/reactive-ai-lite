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

import pro.chenggang.project.reactive.ai.lite.core.spec.ConfigurableChatSpec;
import pro.chenggang.project.reactive.ai.lite.core.spec.ConfigurableEmbeddingSpec;
import pro.chenggang.project.reactive.ai.lite.core.spec.ConfigurableSpeechSpec;

/**
 * Reactive LLM (Large Language Model) client interface for interacting with AI services.
 * <p>
 * This interface is the central entry point for building and executing requests against
 * language model providers in a reactive, non-blocking manner. It encapsulates the pattern
 * of first selecting a provider, then a profile (which governs the model and its parameters),
 * and finally specifying the desired operation (chat, embedding, etc.). This layered design
 * separates configuration from invocation, simplifies multi-provider support, and allows
 * default settings to be applied transparently.
 * </p>
 * <p>
 * The typical interaction flow begins with {@link #newRequest()}, which returns a
 * {@link ClientRequest} instance. From there, the caller can explicitly set a provider and
 * profile before choosing an operation, or rely on the convenience methods
 * {@link #chat()} and {@link #embedding()} that automatically use the
 * application‑configured defaults.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see ClientRequest
 * @see ConfigurableChatSpec
 * @see ConfigurableEmbeddingSpec
 */
public interface ReactiveLlmClient {

    /**
     * Initiates a new request specification.
     * <p>
     * This method creates a fresh {@link ClientRequest} object that acts as a fluent
     * builder for configuring the LLM interaction. The returned instance is stateful and
     * thread‑safe only when used within a single thread; each invocation produces a new,
     * independent spec. The typical builder chain follows the pattern:
     * <pre>{@code
     * client.newRequest()
     *       .provider("openai")
     *       .profile("gpt-4-turbo")
     *       .chat()
     *       .messages(...)
     *       ...
     * }</pre>
     * </p>
     * <p>
     * Because {@code newRequest()} always returns a fresh spec, it is safe to use in
     * concurrent scenarios where multiple requests are being constructed simultaneously.
     * </p>
     *
     * @return the {@link ClientRequest} instance used to continue building the request
     */
    ClientRequest newRequest();

    /**
     * A convenience method to start a chat request using the default provider and default
     * profile immediately.
     * <p>
     * This is equivalent to calling
     * <code>newRequest().defaultProvider().defaultProfile().chat()</code> and is designed
     * for scenarios where a single LLM backend is configured and the application does not
     * need to dynamically switch providers or profiles. The returned
     * {@link ConfigurableChatSpec} allows further fine‑tuning of the chat prompt,
     * temperature, and other parameters before execution.
     * </p>
     *
     * @return a {@link ConfigurableChatSpec} instance ready to be configured and executed
     */
    default ConfigurableChatSpec chat() {
        return newRequest().defaultProvider().defaultProfile().chat();
    }

    /**
     * A convenience method to start an embedding request using the default provider and
     * default profile immediately.
     * <p>
     * This is equivalent to calling
     * <code>newRequest().defaultProvider().defaultProfile().embedding()</code> and is
     * useful when the application only uses the default embedding model. The returned
     * {@link ConfigurableEmbeddingSpec} provides methods to set the input texts and
     * adjust parameters such as the embedding dimension.
     * </p>
     *
     * @return a {@link ConfigurableEmbeddingSpec} instance ready to be configured and executed
     */
    default ConfigurableEmbeddingSpec embedding() {
        return newRequest().defaultProvider().defaultProfile().embedding();
    }

    /**
     * A convenience method to start a speech request using the default provider and
     * default profile immediately.
     * <p>
     * This is equivalent to calling
     * <code>newRequest().defaultProvider().defaultProfile().speech()</code> and is
     * useful when the application only uses the default speech model. The returned
     * {@link ConfigurableSpeechSpec} provides methods to set the input texts and
     * adjust parameters such as the voice.
     * </p>
     *
     * @return a {@link ConfigurableSpeechSpec} instance ready to be configured and executed
     */
    default ConfigurableSpeechSpec speech() {
        return newRequest().defaultProvider().defaultProfile().speech();
    }

}