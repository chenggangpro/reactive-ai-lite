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
package pro.chenggang.project.reactive.ai.lite.core.execution;

import lombok.NonNull;
import org.springframework.core.ParameterizedTypeReference;
import pro.chenggang.project.reactive.ai.lite.core.execution.converter.RawResponseConverter;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.StructuredResponse;
import reactor.core.publisher.Mono;

/**
 * Defines the contract for an LLM execution that returns a structured, typed response.
 * This interface is used when the LLM is expected to generate output that conforms to a specific schema,
 * such as a JSON object that can be deserialized into a Plain Old Java Object (POJO).
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface StructuredExecution extends LlmClientExecution {

    /**
     * Executes the request and deserializes the LLM's response into an object of the specified class.
     *
     * @param resultType The class to which the response should be mapped.
     * @param <R>        The target type.
     * @return A {@link Mono} emitting a {@link StructuredResponse} containing the deserialized object.
     */
    <R> Mono<StructuredResponse<R>> execute(@NonNull Class<R> resultType);

    /**
     * Executes the request and deserializes the LLM's response into a generic type, such as {@code List<String>}.
     *
     * @param resultType A {@link ParameterizedTypeReference} representing the target generic type.
     * @param <R>        The target generic type.
     * @return A {@link Mono} emitting a {@link StructuredResponse} containing the deserialized object.
     */
    <R> Mono<StructuredResponse<R>> execute(@NonNull ParameterizedTypeReference<R> resultType);

    /**
     * Executes the request, instructing the LLM to generate a response that conforms to the provided JSON schema.
     *
     * @param responseJsonSchema A string containing the JSON schema for the expected response.
     * @return A {@link Mono} emitting the raw, unprocessed response from the provider.
     */
    Mono<RawResponse> executeRaw(@NonNull String responseJsonSchema);

    /**
     * Executes the request, instructing the LLM to generate a response that can be mapped to the specified class.
     * The implementation will typically generate a JSON schema from the class to guide the LLM.
     *
     * @param resultType The class representing the desired structure.
     * @param <R>        The target type.
     * @return A {@link Mono} emitting the raw, unprocessed response from the provider.
     */
    <R> Mono<RawResponse> executeRaw(@NonNull Class<R> resultType);

    /**
     * Executes the request, instructing the LLM to generate a response that can be mapped to the specified generic type.
     *
     * @param resultType A {@link ParameterizedTypeReference} representing the desired structure.
     * @param <R>        The target generic type.
     * @return A {@link Mono} emitting the raw, unprocessed response from the provider.
     */
    <R> Mono<RawResponse> executeRaw(@NonNull ParameterizedTypeReference<R> resultType);

    /**
     * Executes the request with a JSON schema and converts the raw response to a custom type.
     *
     * @param responseJsonSchema The JSON schema for the expected response.
     * @param converter          The converter to transform the {@link RawResponse}.
     * @param <R>                The target type of the converted response.
     * @return A {@link Mono} emitting the converted response.
     */
    default <R> Mono<R> execute(@NonNull String responseJsonSchema, @NonNull RawResponseConverter<R> converter) {
        return executeRaw(responseJsonSchema).map(converter::convert);
    }

    /**
     * Executes the request with a target class and converts the raw response to a custom type.
     *
     * @param resultType The class representing the desired structure.
     * @param converter  The converter to transform the {@link RawResponse}.
     * @param <R>        The target type of the converted response.
     * @return A {@link Mono} emitting the converted response.
     */
    default <R> Mono<R> execute(@NonNull Class<R> resultType, @NonNull RawResponseConverter<R> converter) {
        return executeRaw(resultType).map(converter::convert);
    }

    /**
     * Executes the request with a target generic type and converts the raw response to a custom type.
     *
     * @param resultType A {@link ParameterizedTypeReference} for the desired structure.
     * @param converter  The converter to transform the {@link RawResponse}.
     * @param <R>        The target type of the converted response.
     * @return A {@link Mono} emitting the converted response.
     */
    default <R> Mono<R> execute(@NonNull ParameterizedTypeReference<R> resultType, @NonNull RawResponseConverter<R> converter) {
        return executeRaw(resultType).map(converter::convert);
    }

}
