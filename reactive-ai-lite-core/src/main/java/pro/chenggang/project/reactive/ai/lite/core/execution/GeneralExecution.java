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
 * This execution type returns a single response object wrapped in a {@link Mono}.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface GeneralExecution extends LlmClientExecution {

    /**
     * Executes the LLM request and returns a structured {@link GeneralResponse}.
     *
     * @return A {@link Mono} emitting the structured response.
     */
    Mono<GeneralResponse> execute();

    /**
     * Executes the LLM request and returns the raw, unprocessed response from the provider.
     *
     * @return A {@link Mono} emitting the raw response.
     */
    Mono<RawResponse> executeRaw();

    /**
     * Executes the LLM request and converts the raw response to a custom type using the provided converter.
     *
     * @param converter The converter to transform the {@link RawResponse} into the desired type.
     * @param <R>       The target type of the response.
     * @return A {@link Mono} emitting the converted response.
     */
    default <R> Mono<R> execute(RawResponseConverter<R> converter) {
        return executeRaw().map(converter::convert);
    }

}
