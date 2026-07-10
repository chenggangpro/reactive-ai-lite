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
package pro.chenggang.project.reactive.ai.lite.core.provider;

import lombok.NonNull;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.GeneralResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.StreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ChatExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.option.Capability;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Provider interface specifically tailored for LLM chat capabilities.
 * <p>
 * This interface extends {@link LlmProvider} to define the contract for executing
 * various types of chat requests against an AI backend. It provides both unified, processed
 * response paths and raw, provider‑specific access, enabling flexible integration for
 * different client needs. Each method accepts a {@link ChatExecutionInfo} that describes
 * the full chat context (messages, tools, options, etc.) and returns a reactive type,
 * allowing non‑blocking execution.
 * </p>
 * <p>
 * Implementations are expected to handle protocol details (HTTP, WebSocket, SSE),
 * authentication, and payload conversion. The processed methods
 * ({@link #executeGeneral(ChatExecutionInfo)}, {@link #executeStream(ChatExecutionInfo)})
 * normalize responses into the framework's common model, while the raw methods
 * ({@link #executeGeneralRaw(ChatExecutionInfo)}, {@link #executeStreamRaw(ChatExecutionInfo)})
 * give direct access to the provider's native data for debugging, logging, or advanced
 * use cases.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see LlmProvider
 * @see Capability#CHAT
 */
public interface LlmChatProvider extends LlmProvider {

    /**
     * Returns the capability type of this provider.
     * <p>
     * For any chat provider, this is unconditionally {@link Capability#CHAT}.
     * This method allows the framework to introspect the provider's role without
     * requiring explicit type checks, enabling capability‑based routing and validation.
     * </p>
     *
     * @return always {@link Capability#CHAT}
     */
    @Override
    default Capability capability() {
        return Capability.CHAT;
    }

    /**
     * Executes a general (non‑streaming) chat request and returns a fully processed,
     * unified response.
     * <p>
     * The implementation sends the request to the AI service, waits for the complete
     * response, and converts the provider‑specific JSON into a standardized
     * {@link GeneralResponse} object. This encompasses all relevant data such as
     * generated text, tool call requests, finish reasons, usage statistics, etc.
     * This method is suitable for single‑turn or conversational interactions where
     * the entire response is needed at once.
     * </p>
     *
     * @param executionInfo the chat execution context containing messages, model,
     *                      temperature, tools, and other options; must not be {@code null}
     *                      (enforced by {@link lombok.NonNull})
     * @return a {@link Mono} that completes with the unified response once the
     *         entire chat answer is ready
     * @see GeneralResponse
     */
    Mono<GeneralResponse> executeGeneral(@NonNull ChatExecutionInfo executionInfo);

    /**
     * Executes a general (non‑streaming) chat request and returns the raw, unprocessed
     * provider response.
     * <p>
     * Unlike {@link #executeGeneral(ChatExecutionInfo)}, this method does not apply
     * any post‑processing or conversion; it passes through the exact HTTP response body
     * (or equivalent) as received from the AI service. This makes it ideal for scenarios
     * that require direct inspection of the provider's native format, such as verbose
     * logging, response validation, or integration with custom parsing logic.
     * </p>
     *
     * @param executionInfo the chat execution context containing messages, model,
     *                      temperature, tools, and other options; must not be {@code null}
     *                      (enforced by {@link lombok.NonNull})
     * @return a {@link Mono} that completes with the raw response as a
     *         {@link RawResponse} instance (which typically wraps the original JSON
     *         string)
     * @see RawResponse
     */
    Mono<RawResponse> executeGeneralRaw(@NonNull ChatExecutionInfo executionInfo);

    /**
     * Initiates a streaming chat request and emits partially‑processed response chunks
     * in a unified format as they arrive.
     * <p>
     * The implementation opens a streaming connection (e.g., SSE) and parses each incoming
     * server‑sent event into a {@link StreamResponse} object. These objects represent
     * incremental updates—such as text deltas, tool call arguments, or status
     * notifications—in a provider‑independent way, allowing reactive clients to consume
     * real‑time output without being tied to a specific AI protocol.
     * </p>
     *
     * @param executionInfo the chat execution context containing messages, model,
     *                      temperature, tools, and other options; must not be {@code null}
     *                      (enforced by {@link lombok.NonNull})
     * @return a {@link Flux} that emits sequential {@link StreamResponse} instances until
     *         the stream completes or is cancelled
     * @see StreamResponse
     */
    Flux<StreamResponse> executeStream(@NonNull ChatExecutionInfo executionInfo);

    /**
     * Initiates a streaming chat request and emits the raw, unprocessed chunks as they
     * arrive.
     * <p>
     * This method provides direct access to each server‑sent event (or equivalent
     * streaming protocol element) without any parsing or transformation. Each chunk is
     * bundled as a {@link RawStreamResponse}, typically containing the raw event string.
     * This is especially useful for low‑level debugging, implementing custom parsers, or
     * archiving the exact stream data for later replay.
     * </p>
     *
     * @param executionInfo the chat execution context containing messages, model,
     *                      temperature, tools, and other options; must not be {@code null}
     *                      (enforced by {@link lombok.NonNull})
     * @return a {@link Flux} that emits sequential {@link RawStreamResponse} instances
     *         until the stream ends or is cancelled
     * @see RawStreamResponse
     */
    Flux<RawStreamResponse> executeStreamRaw(@NonNull ChatExecutionInfo executionInfo);

}