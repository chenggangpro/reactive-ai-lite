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
import org.springframework.core.ParameterizedTypeReference;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.GeneralResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.StreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.StructuredResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.option.Capability;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Provider interface for LLM chat capabilities, supporting various execution modes including
 * general responses, streaming, and structured outputs.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface LlmChatProvider extends LlmProvider {

    /**
     * Returns the capability type of this provider.
     *
     * @return the {@link Capability#CHAT} capability
     */
    @Override
    default Capability capability() {
        return Capability.CHAT;
    }

    /**
     * Executes a general chat request and returns a processed response.
     *
     * @param executionInfo the execution information containing the request details
     * @return a {@link Mono} emitting the general response
     */
    Mono<GeneralResponse> executeGeneral(@NonNull ExecutionInfo executionInfo);

    /**
     * Executes a general chat request and returns the raw, unprocessed response.
     *
     * @param executionInfo the execution information containing the request details
     * @return a {@link Mono} emitting the raw response
     */
    Mono<RawResponse> executeGeneralRaw(@NonNull ExecutionInfo executionInfo);

    /**
     * Executes a streaming chat request and returns processed stream responses.
     *
     * @param executionInfo the execution information containing the request details
     * @return a {@link Flux} emitting stream responses as they arrive
     */
    Flux<StreamResponse> executeStream(@NonNull ExecutionInfo executionInfo);

    /**
     * Executes a streaming chat request and returns raw, unprocessed stream responses.
     *
     * @param executionInfo the execution information containing the request details
     * @return a {@link Flux} emitting raw stream responses as they arrive
     */
    Flux<RawStreamResponse> executeStreamRaw(@NonNull ExecutionInfo executionInfo);

    /**
     * Executes a structured chat request and returns a response deserialized to the specified type.
     *
     * @param <R> the type of the result object
     * @param executionInfo the execution information containing the request details
     * @param resultType the class type to deserialize the response into
     * @return a {@link Mono} emitting the structured response containing the deserialized result
     */
    <R> Mono<StructuredResponse<R>> executeStructured(@NonNull ExecutionInfo executionInfo, @NonNull Class<R> resultType);

    /**
     * Executes a structured chat request and returns a response deserialized to the specified parameterized type.
     *
     * @param <R> the type of the result object
     * @param executionInfo the execution information containing the request details
     * @param resultType the parameterized type reference to deserialize the response into
     * @return a {@link Mono} emitting the structured response containing the deserialized result
     */
    <R> Mono<StructuredResponse<R>> executeStructured(@NonNull ExecutionInfo executionInfo, @NonNull ParameterizedTypeReference<R> resultType);

    /**
     * Executes a structured chat request with a custom JSON schema and returns the raw response.
     *
     * @param executionInfo the execution information containing the request details
     * @param responseJsonSchema the JSON schema defining the expected response structure
     * @return a {@link Mono} emitting the raw response
     */
    Mono<RawResponse> executeStructuredRaw(@NonNull ExecutionInfo executionInfo, @NonNull String responseJsonSchema);

    /**
     * Executes a structured chat request for the specified type and returns the raw response.
     *
     * @param <R> the type of the result object
     * @param executionInfo the execution information containing the request details
     * @param resultType the class type used to generate the response schema
     * @return a {@link Mono} emitting the raw response
     */
    <R> Mono<RawResponse> executeStructuredRaw(@NonNull ExecutionInfo executionInfo, @NonNull Class<R> resultType);

    /**
     * Executes a structured chat request for the specified parameterized type and returns the raw response.
     *
     * @param <R> the type of the result object
     * @param executionInfo the execution information containing the request details
     * @param resultType the parameterized type reference used to generate the response schema
     * @return a {@link Mono} emitting the raw response
     */
    <R> Mono<RawResponse> executeStructuredRaw(@NonNull ExecutionInfo executionInfo, @NonNull ParameterizedTypeReference<R> resultType);

}
