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

import pro.chenggang.project.reactive.ai.lite.core.execution.converter.RawResponseConverter;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.GeneralResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import reactor.core.publisher.Mono;

/**
 * Defines the contract for a general, non-streaming execution of an LLM request.
 * <p>
 * This execution type is designed for standard request-response scenarios where the
 * entire generated output is received at once. It returns a single response object
 * wrapped in a Project Reactor {@link Mono}.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface GeneralExecution extends LlmClientExecution {

    /**
     * Executes the non-streaming LLM request and returns a standardized response object.
     * <p>
     * The returned {@link GeneralResponse} abstracts away provider-specific details,
     * providing a unified interface to access messages, reasoning content, usage stats,
     * and tool calls.
     * </p>
     *
     * @return a {@link Mono} emitting the parsed {@link GeneralResponse}
     */
    Mono<GeneralResponse> execute();

    /**
     * Executes the non-streaming LLM request and returns the raw, unprocessed provider response.
     * <p>
     * This is useful when the calling code needs access to the exact JSON structure
     * returned by the specific AI API (e.g., to extract non-standard fields).
     * </p>
     *
     * @return a {@link Mono} emitting the raw JSON response as a {@link RawResponse}
     */
    Mono<RawResponse> executeRaw();

    /**
     * Executes the non-streaming LLM request and converts the raw response to a custom type
     * using the provided converter.
     * <p>
     * This is a convenience method that automatically maps the output of {@link #executeRaw()}
     * using the provided {@link RawResponseConverter}.
     * </p>
     *
     * @param converter the converter to transform the {@link RawResponse} into the desired type
     * @param <R>       the target type of the conversion
     * @return a {@link Mono} emitting the converted response
     */
    default <R> Mono<R> execute(RawResponseConverter<R> converter) {
        return executeRaw().map(converter::convert);
    }

}
