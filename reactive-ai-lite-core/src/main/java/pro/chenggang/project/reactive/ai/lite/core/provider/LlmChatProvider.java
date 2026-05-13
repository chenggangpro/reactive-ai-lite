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
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.option.Capability;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Provider interface specifically tailored for LLM chat capabilities.
 * <p>
 * This interface extends {@link LlmProvider} to provide methods for executing various
 * types of chat requests against an underlying AI service. It supports general single-turn
 * requests, streaming responses, and structured output generation. Implementations of this
 * interface handle the actual HTTP communication and protocol specifics for different AI providers.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface LlmChatProvider extends LlmProvider {

    /**
     * Returns the capability type of this provider.
     * <p>
     * For chat providers, this always returns {@link Capability#CHAT}.
     * </p>
     *
     * @return the {@link Capability#CHAT} capability
     */
    @Override
    default Capability capability() {
        return Capability.CHAT;
    }

    /**
     * Executes a general chat request and returns a processed, unified response.
     * <p>
     * The response is parsed and converted into a standard {@link GeneralResponse}
     * format, abstracting away provider-specific JSON structures.
     * </p>
     *
     * @param executionInfo the execution information containing the request details
     * @return a {@link Mono} emitting the unified general response
     */
    Mono<GeneralResponse> executeGeneral(@NonNull ExecutionInfo executionInfo);

    /**
     * Executes a general chat request and returns the raw, unprocessed provider response.
     * <p>
     * This is useful when the calling code needs access to the exact JSON structure
     * returned by the specific AI API.
     * </p>
     *
     * @param executionInfo the execution information containing the request details
     * @return a {@link Mono} emitting the raw response
     */
    Mono<RawResponse> executeGeneralRaw(@NonNull ExecutionInfo executionInfo);

    /**
     * Executes a streaming chat request and returns a flux of processed stream responses.
     * <p>
     * The response stream is parsed, and individual chunks (like text fragments or tool calls)
     * are emitted as standard {@link StreamResponse} objects.
     * </p>
     *
     * @param executionInfo the execution information containing the request details
     * @return a {@link Flux} emitting parsed stream responses as they arrive
     */
    Flux<StreamResponse> executeStream(@NonNull ExecutionInfo executionInfo);

    /**
     * Executes a streaming chat request and returns a flux of raw, unprocessed stream chunks.
     * <p>
     * This is useful for intercepting or logging the exact SSE stream data sent by the provider.
     * </p>
     *
     * @param executionInfo the execution information containing the request details
     * @return a {@link Flux} emitting raw stream responses as they arrive
     */
    Flux<RawStreamResponse> executeStreamRaw(@NonNull ExecutionInfo executionInfo);

}
