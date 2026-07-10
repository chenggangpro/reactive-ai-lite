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
 * Implementations of this interface handle the generation of prompts and the specification of a JSON schema
 * to an AI language model, ensuring the response can be reliably deserialized into Java objects. This is
 * essential when the client expects a well-defined data structure rather than free-form text.
 * </p>
 * <p>
 * The interface provides multiple execution methods:
 * <ul>
 *   <li><b>Typed execution:</b> The framework generates a JSON schema from the provided {@code Class} or
 *       {@link ParameterizedTypeReference}, instructs the model to produce compliant JSON, and automatically
 *       deserializes it into a {@link StructuredResponse} containing the target object.</li>
 *   <li><b>Raw execution:</b> Returns the model's response as a raw string, optionally guided by a schema
 *       (explicit or generated), bypassing automatic deserialization. This is useful for inspection,
 *       logging, or custom processing.</li>
 *   <li><b>Converted execution:</b> Combines raw execution with a {@link RawResponseConverter} to perform
 *       bespoke transformation logic.</li>
 * </ul>
 * </p>
 * <p>
 * All methods are reactive and return {@link Mono}, reflecting the non-blocking nature of the underlying
 * AI service calls.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 * @see RawResponseConverter
 * @see RawResponse
 * @see StructuredResponse
 */
public interface StructuredExecution {

    /**
     * Executes the request and deserializes the model's JSON response into an instance of the given class.
     * <p>
     * The framework will introspect the class to generate a JSON schema that is passed to the model,
     * constraining its output. The raw JSON string returned by the model is then parsed into a
     * {@link StructuredResponse} containing the object of type {@code R}.
     * </p>
     *
     * @param resultType the target class (must have a no-arg constructor and be compatible with Jackson)
     * @param <R>        the type of the expected response object
     * @return a {@link Mono} emitting a {@link StructuredResponse} that holds the deserialized object,
     *         or an error signal if the response cannot be parsed
     */
    <R> Mono<StructuredResponse<R>> execute(@NonNull Class<R> resultType);

    /**
     * Executes the request and deserializes the model's JSON response into an object described by a
     * parameterized type reference.
     * <p>
     * This variant is necessary when the target type includes generic parameters, such as
     * {@code List<String>} or {@code Map<String, MyPojo>}, because Java's type erasure prevents
     * conveying full generic information via a plain {@code Class} object. The framework
     * will generate a schema that reflects the complete generic structure.
     * </p>
     *
     * @param resultType a {@link ParameterizedTypeReference} encoding the full generic type
     * @param <R>        the complete target type, including generic arguments
     * @return a {@link Mono} emitting a {@link StructuredResponse} holding the deserialized object
     */
    <R> Mono<StructuredResponse<R>> execute(@NonNull ParameterizedTypeReference<R> resultType);

    /**
     * Executes the request and returns the raw response text, bypassing any automatic deserialization.
     * <p>
     * The provided JSON schema string is sent directly to the model to shape its output. This is
     * the most flexible raw method, allowing you to supply a schema that may not be directly
     * representable as a Java class (e.g., schemas with {@code oneOf} constructs, conditional fields, etc.).
     * The response is wrapped in a {@link RawResponse} for further inspection or processing.
     * </p>
     *
     * @param responseJsonSchema a JSON string representing the expected schema (must be a valid JSON Schema)
     * @return a {@link Mono} emitting the raw {@link RawResponse} object containing the model's output
     */
    Mono<RawResponse> executeRaw(@NonNull String responseJsonSchema);

    /**
     * Executes the request, auto-generating a JSON schema from the given class, and returns the raw response.
     * <p>
     * This is a convenience method that bridges the typed schema generation with the raw execution path.
     * The schema is derived from the class structure, but the JSON is not deserialized. Useful for
     * debugging or for cases where you later want to deserialize with different settings.
     * </p>
     *
     * @param resultType the class from which a JSON schema will be generated
     * @param <R>        the type that informs schema generation
     * @return a {@link Mono} emitting the raw {@link RawResponse}
     */
    <R> Mono<RawResponse> executeRaw(@NonNull Class<R> resultType);

    /**
     * Executes the request, generating a JSON schema from the given parameterized type reference,
     * and returns the raw response without deserialization.
     * <p>
     * Similar to {@link #executeRaw(Class)} but preserves generic type information when building
     * the schema, ensuring that complex containers are accurately represented.
     * </p>
     *
     * @param resultType a {@link ParameterizedTypeReference} describing the full target type (including generics)
     * @param <R>        the target generic type used for schema generation
     * @return a {@link Mono} emitting the raw {@link RawResponse}
     */
    <R> Mono<RawResponse> executeRaw(@NonNull ParameterizedTypeReference<R> resultType);

    /**
     * Executes the request with an explicit schema and transforms the raw response using a custom converter.
     * <p>
     * This default method calls {@link #executeRaw(String)} and then applies the provided
     * {@code converter} to map the raw output into the desired type. It is a convenient way to
     * implement non-standard deserialization logic (e.g., using a different JSON library,
     * applying transformations, or aggregating multiple outputs).
     * </p>
     *
     * @param responseJsonSchema the JSON schema to constrain the model's output
     * @param converter          a function that converts the raw response into the desired type
     * @param <R>                the final type produced by the converter
     * @return a {@link Mono} emitting the converted object
     */
    default <R> Mono<R> execute(@NonNull String responseJsonSchema, @NonNull RawResponseConverter<R> converter) {
        return executeRaw(responseJsonSchema).map(converter::convert);
    }

    /**
     * Executes the request with a class-based schema and transforms the raw response using a custom converter.
     *
     * @param resultType the class from which a JSON schema is generated
     * @param converter  a function that converts the raw response into the desired type
     * @param <R>        the final type produced by the converter
     * @return a {@link Mono} emitting the converted object
     */
    default <R> Mono<R> execute(@NonNull Class<R> resultType, @NonNull RawResponseConverter<R> converter) {
        return executeRaw(resultType).map(converter::convert);
    }

    /**
     * Executes the request with a parameterized-type-based schema and transforms the raw response
     * using a custom converter.
     *
     * @param resultType a {@link ParameterizedTypeReference} describing the full target type
     * @param converter  a function that converts the raw response into the desired type
     * @param <R>        the final type produced by the converter
     * @return a {@link Mono} emitting the converted object
     */
    default <R> Mono<R> execute(@NonNull ParameterizedTypeReference<R> resultType, @NonNull RawResponseConverter<R> converter) {
        return executeRaw(resultType).map(converter::convert);
    }

}