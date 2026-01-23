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

import pro.chenggang.project.reactive.ai.lite.core.execution.converter.RawStreamResponseConverter;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.StreamResponse;
import reactor.core.publisher.Flux;

/**
 * Defines the contract for a streaming execution of an LLM request.
 * This execution type is designed for scenarios where the response is delivered as a stream of events,
 * wrapped in a {@link Flux}. It is ideal for handling real-time or chunked data from the LLM provider.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface StreamExecution extends LlmClientExecution {

    /**
     * Executes the streaming LLM request and returns a structured stream of {@link StreamResponse} objects.
     * Each item in the stream represents a part of the overall response.
     *
     * @return A {@link Flux} that emits structured response chunks as they are received.
     */
    Flux<StreamResponse> execute();

    /**
     * Executes the streaming LLM request and returns the raw, unprocessed stream of events from the provider.
     * This is useful for accessing provider-specific data or for custom processing.
     *
     * @return A {@link Flux} that emits raw response chunks as {@link RawStreamResponse} objects.
     */
    Flux<RawStreamResponse> executeRaw();

    /**
     * Executes the streaming LLM request and converts each raw response chunk to a custom type using the provided converter.
     * This default method simplifies the process of transforming the raw data stream into a desired format.
     *
     * @param converter The converter to apply to each {@link RawStreamResponse} in the stream.
     * @param <R>       The target type of the converted response chunks.
     * @return A {@link Flux} that emits the converted response chunks.
     */
    default <R> Flux<R> execute(RawStreamResponseConverter<R> converter) {
        return executeRaw().map(converter::convert);
    }

}
