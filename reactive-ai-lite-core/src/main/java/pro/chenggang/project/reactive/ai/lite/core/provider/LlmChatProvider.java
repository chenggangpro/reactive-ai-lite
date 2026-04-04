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
 * Provider interface specifically tailored for LLM chat capabilities.
 * <p>
 * This interface extends {@link LlmProvider} to provide methods for executing various
 * types of chat requests against an underlying AI service. It supports general single-turn
 * requests, streaming responses, and structured output generation. Implementations of this
 * interface handle the actual HTTP communication and protocol specifics for different AI providers.
 * </p>
 *
 * @author Cheng Gang
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

    /**
     * Executes a structured chat request and returns a response deserialized into the specified class type.
     * <p>
     * The provider implementation is responsible for instructing the model to output JSON
     * matching the schema of the provided class, and then parsing that JSON into an instance.
     * </p>
     *
     * @param <R>           the type of the result object
     * @param executionInfo the execution information containing the request details
     * @param resultType    the class type to deserialize the response into
     * @return a {@link Mono} emitting the structured response containing the deserialized result
     */
    <R> Mono<StructuredResponse<R>> executeStructured(@NonNull ExecutionInfo executionInfo, @NonNull Class<R> resultType);

    /**
     * Executes a structured chat request and returns a response deserialized into a parameterized type.
     * <p>
     * This method is used when the desired output type involves generics (e.g., {@code List<MyObject>}).
     * </p>
     *
     * @param <R>           the type of the result object
     * @param executionInfo the execution information containing the request details
     * @param resultType    the parameterized type reference to deserialize the response into
     * @return a {@link Mono} emitting the structured response containing the deserialized result
     */
    <R> Mono<StructuredResponse<R>> executeStructured(@NonNull ExecutionInfo executionInfo, @NonNull ParameterizedTypeReference<R> resultType);

    /**
     * Executes a structured chat request using a custom JSON schema string and returns the raw response.
     * <p>
     * The provider will force the model's output to conform to the provided JSON schema.
     * </p>
     *
     * @param executionInfo      the execution information containing the request details
     * @param responseJsonSchema the raw JSON schema string defining the expected response structure
     * @return a {@link Mono} emitting the raw response
     */
    Mono<RawResponse> executeStructuredRaw(@NonNull ExecutionInfo executionInfo, @NonNull String responseJsonSchema);

    /**
     * Executes a structured chat request based on the schema of a class type and returns the raw response.
     * <p>
     * The framework will generate a JSON schema from the class, send it to the provider,
     * and return the unparsed JSON string response.
     * </p>
     *
     * @param <R>           the type of the result object
     * @param executionInfo the execution information containing the request details
     * @param resultType    the class type used to generate the response schema
     * @return a {@link Mono} emitting the raw response containing the JSON string
     */
    <R> Mono<RawResponse> executeStructuredRaw(@NonNull ExecutionInfo executionInfo, @NonNull Class<R> resultType);

    /**
     * Executes a structured chat request based on the schema of a parameterized type and returns the raw response.
     * <p>
     * The framework will generate a JSON schema from the parameterized type, send it to the provider,
     * and return the unparsed JSON string response.
     * </p>
     *
     * @param <R>           the type of the result object
     * @param executionInfo the execution information containing the request details
     * @param resultType    the parameterized type reference used to generate the response schema
     * @return a {@link Mono} emitting the raw response containing the JSON string
     */
    <R> Mono<RawResponse> executeStructuredRaw(@NonNull ExecutionInfo executionInfo, @NonNull ParameterizedTypeReference<R> resultType);

}
