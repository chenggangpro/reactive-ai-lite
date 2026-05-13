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
 * <p>
 * This execution type is designed for scenarios where the response is delivered
 * as a continuous stream of events (e.g., Server-Sent Events), wrapped in a Project Reactor
 * {@link Flux}. It is ideal for handling real-time or chunked text generation, allowing
 * applications to display responses to users before the entire completion is finished.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface StreamExecution extends LlmClientExecution {

    /**
     * Executes the streaming LLM request and returns a standardized flux of parsed response chunks.
     * <p>
     * Each item in the stream represents a categorized part of the overall response
     * (e.g., a text token, a tool call snippet, or final usage metrics) abstracted
     * away from the provider's specific JSON schema.
     * </p>
     *
     * @return a {@link Flux} emitting structured {@link StreamResponse} objects
     */
    Flux<StreamResponse> execute();

    /**
     * Executes the streaming LLM request and returns the raw, unprocessed stream of JSON events.
     * <p>
     * This is useful for accessing provider-specific, non-standard fields or for
     * performing custom parsing logic outside the framework's standard abstractions.
     * </p>
     *
     * @return a {@link Flux} emitting raw JSON chunks as {@link RawStreamResponse} objects
     */
    Flux<RawStreamResponse> executeRaw();

    /**
     * Executes the streaming LLM request and applies a custom converter to each raw chunk.
     * <p>
     * This is a convenience method that automatically maps the output of {@link #executeRaw()}
     * using the provided {@link RawStreamResponseConverter}.
     * </p>
     *
     * @param converter the converter to transform each {@link RawStreamResponse}
     * @param <R>       the target type of the conversion
     * @return a {@link Flux} emitting the converted chunks
     */
    default <R> Flux<R> execute(RawStreamResponseConverter<R> converter) {
        return executeRaw().map(converter::convert);
    }

}
