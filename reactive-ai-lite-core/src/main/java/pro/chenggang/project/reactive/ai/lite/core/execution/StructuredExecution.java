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
 * Defines the contract for an LLM execution that mandates a structured, typed JSON response.
 * <p>
 * This interface is used when the AI model is expected to generate output that strictly
 * conforms to a specific schema, allowing the framework to automatically deserialize
 * the response into a Plain Old Java Object (POJO) or a specific collection type.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface StructuredExecution extends LlmClientExecution {

    /**
     * Executes the request and deserializes the LLM's JSON response into an object of the specified class.
     * <p>
     * The framework will typically generate a JSON schema from the provided class to guide
     * the model's output generation.
     * </p>
     *
     * @param resultType the class to which the JSON response should be mapped
     * @param <R>        the target object type
     * @return a {@link Mono} emitting a {@link StructuredResponse} containing the deserialized object
     */
    <R> Mono<StructuredResponse<R>> execute(@NonNull Class<R> resultType);

    /**
     * Executes the request and deserializes the LLM's JSON response into a parameterized generic type.
     * <p>
     * This is useful when the expected output is a collection, such as {@code List<String>}
     * or {@code Map<String, MyObject>}.
     * </p>
     *
     * @param resultType a {@link ParameterizedTypeReference} representing the target generic type
     * @param <R>        the target generic type
     * @return a {@link Mono} emitting a {@link StructuredResponse} containing the deserialized object
     */
    <R> Mono<StructuredResponse<R>> execute(@NonNull ParameterizedTypeReference<R> resultType);

    /**
     * Executes the request by passing an explicit JSON schema to the model, returning the raw response.
     * <p>
     * This instructs the LLM to generate a response that conforms to the provided schema,
     * but bypasses automatic deserialization, giving you access to the raw JSON string.
     * </p>
     *
     * @param responseJsonSchema a string containing the JSON schema for the expected response
     * @return a {@link Mono} emitting the raw, unprocessed {@link RawResponse}
     */
    Mono<RawResponse> executeRaw(@NonNull String responseJsonSchema);

    /**
     * Executes the request using a schema generated from the specified class, returning the raw response.
     *
     * @param resultType the class representing the desired schema structure
     * @param <R>        the target type
     * @return a {@link Mono} emitting the raw, unprocessed {@link RawResponse}
     */
    <R> Mono<RawResponse> executeRaw(@NonNull Class<R> resultType);

    /**
     * Executes the request using a schema generated from the specified parameterized type, returning the raw response.
     *
     * @param resultType a {@link ParameterizedTypeReference} representing the desired schema structure
     * @param <R>        the target generic type
     * @return a {@link Mono} emitting the raw, unprocessed {@link RawResponse}
     */
    <R> Mono<RawResponse> executeRaw(@NonNull ParameterizedTypeReference<R> resultType);

    /**
     * Executes the request with an explicit JSON schema and converts the raw response using a custom converter.
     *
     * @param responseJsonSchema the JSON schema for the expected response
     * @param converter          the converter to transform the {@link RawResponse}
     * @param <R>                the target type of the converted response
     * @return a {@link Mono} emitting the manually converted response
     */
    default <R> Mono<R> execute(@NonNull String responseJsonSchema, @NonNull RawResponseConverter<R> converter) {
        return executeRaw(responseJsonSchema).map(converter::convert);
    }

    /**
     * Executes the request with a class-based schema and converts the raw response using a custom converter.
     *
     * @param resultType the class representing the desired schema structure
     * @param converter  the converter to transform the {@link RawResponse}
     * @param <R>        the target type of the converted response
     * @return a {@link Mono} emitting the manually converted response
     */
    default <R> Mono<R> execute(@NonNull Class<R> resultType, @NonNull RawResponseConverter<R> converter) {
        return executeRaw(resultType).map(converter::convert);
    }

    /**
     * Executes the request with a parameterized type schema and converts the raw response using a custom converter.
     *
     * @param resultType a {@link ParameterizedTypeReference} for the desired schema structure
     * @param converter  the converter to transform the {@link RawResponse}
     * @param <R>        the target type of the converted response
     * @return a {@link Mono} emitting the manually converted response
     */
    default <R> Mono<R> execute(@NonNull ParameterizedTypeReference<R> resultType, @NonNull RawResponseConverter<R> converter) {
        return executeRaw(resultType).map(converter::convert);
    }

}
