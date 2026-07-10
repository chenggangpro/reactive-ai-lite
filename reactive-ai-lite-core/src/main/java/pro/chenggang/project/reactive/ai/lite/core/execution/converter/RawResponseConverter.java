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
package pro.chenggang.project.reactive.ai.lite.core.execution.converter;

import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;

/**
 * A functional interface that allows custom, application‑specific conversion
 * of a raw, unparsed AI provider response into a typed domain object.
 * <p>
 * Most of the time the framework’s built‑in response extraction (e.g., via
 * {@code .contentAsJson()} or {@code .contentAsText()}) is sufficient.
 * However, when a provider introduces new, non‑standard response structures,
 * or when the application needs to map raw JSON into its own data model,
 * a custom converter can be supplied directly to the fluent execution API.
 * The execution engine invokes {@link #convert(RawResponse)} once the provider
 * has returned the raw HTTP body and metadata, and before returning control
 * to the caller.
 * </p>
 * <p>
 * Typical usage:
 * <pre>{@code
 * GeneralExecution<MyResponse> execution = ...;
 * MyResponse result = execution.execute(rawResponse -> {
 *     // parse rawResponse.rawBody() into MyResponse
 *     return objectMapper.readValue(rawResponse.rawBody(), MyResponse.class);
 * });
 * }</pre>
 * Because the interface is a {@link FunctionalInterface}, lambda expressions
 * or method references can implement it concisely.
 * </p>
 * <p>
 * Implementations should be stateless and side‑effect free, as the same
 * converter may be invoked concurrently by different execution threads.
 * Any parsing errors should be propagated as unchecked exceptions; the
 * framework will wrap them into appropriate runtime exceptions.
 * </p>
 *
 * @param <RESPONSE> the target type of the converted response; this is
 *                   typically a domain DTO, a JSON node, or a string
 *                   representation chosen by the application
 * @author Gang Cheng
 * @version 0.1.0
 * @see RawResponse
 * @see pro.chenggang.project.reactive.ai.lite.core.execution.GeneralExecution#execute(RawResponseConverter)
 */
@FunctionalInterface
public interface RawResponseConverter<RESPONSE> {

    /**
     * Transforms the raw, provider‑agnostic response into the application’s
     * own representation.
     * <p>
     * This method is invoked by the framework after the AI provider has
     * returned a response and before it is handed back to the client code.
     * The supplied {@link RawResponse} contains the complete HTTP body
     * (as a {@link String}) as well as any headers, status code, and
     * provider metadata. This gives full flexibility to parse vendor‑specific
     * JSON shapes, handle streaming responses, or enrich the result with
     * additional information.
     * </p>
     *
     * @param rawResponse non‑null raw response object wrapping the provider’s
     *                    JSON body and associated metadata
     * @return the converted instance of type {@code RESPONSE}; must not be
     *         {@code null}
     * @throws RuntimeException if parsing fails; the framework will wrap any
     *                          exception in a {@code ResponseConversionException}
     */
    RESPONSE convert(RawResponse rawResponse);

}