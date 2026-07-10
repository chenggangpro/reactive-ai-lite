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
 * Contract for executing a Large Language Model (LLM) request in a reactive,
 * streaming fashion. Implementations typically wrap a reactive HTTP client that
 * connects to a provider's streaming endpoint (e.g., OpenAI chat completions with
 * {@code stream=true}) and process Server-Sent Events (SSE) or similar chunked
 * transfer encodings.
 * <p>
 * The key advantage of a streaming execution is that partial results become
 * available to the application as soon as they are produced by the model, without
 * waiting for the entire completion. This enables lower time-to-first-byte
 * (TTFB) and a more responsive user experience, especially for longer responses.
 * </p>
 * <p>
 * The interface offers two levels of abstraction:
 * <ul>
 *   <li>{@link #execute()}: parses the raw JSON events into a unified
 *       {@link StreamResponse} model (text, tool calls, usage metrics), hiding
 *       provider-specific schemas.</li>
 *   <li>{@link #executeRaw()}: delivers the raw JSON chunks as they arrive,
 *       providing full access to the provider's response format, useful for
 *       debugging, logging, or custom processing.</li>
 * </ul>
 * Additionally, a convenience {@link #execute(RawStreamResponseConverter)} method
 * allows direct transformation of each raw chunk to any domain type.
 * <p>
 * The resulting {@link Flux} may emit multiple items until the stream is
 * complete, after which an onComplete signal is sent. In case of errors (network
 * issues, authentication failures, rate limits), an onError signal is emitted.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface StreamExecution {

    /**
     * Initiates the streaming LLM call and returns a {@link Flux} of
     * structured, provider-agnostic {@link StreamResponse} objects.
     * <p>
     * Under the hood, this method typically delegates to {@link #executeRaw()}
     * and translates each raw chunk into a {@link StreamResponse} using
     * internal converters. The mapping handles the variability of different
     * LLM providers (e.g., OpenAI's chunk format vs. Anthropic's streaming
     * messages) behind a consistent API.
     * </p>
     * <p>
     * The emitted items can include:
     * <ul>
     *   <li>Content – tokens of the generated text (delta).</li>
     *   <li>Tool calls – incremental snippets of function-calling requests.</li>
     *   <li>Metadata – finish reasons, usage statistics, or any provider
     *       meta-information.</li>
     * </ul>
     * The final element may carry the aggregated usage data (total token
     * counts) once the stream has ended.
     * </p>
     *
     * @return a {@link Flux} emitting structured streaming responses;
     *         never {@code null}, may be empty if no chunks are received
     *         (unlikely in a streaming scenario)
     */
    Flux<StreamResponse> execute();

    /**
     * Initiates the streaming LLM call and returns the raw, unprocessed
     * JSON chunks as a {@link Flux} of {@link RawStreamResponse}.
     * <p>
     * This method is designed for advanced use cases where the standard
     * abstraction is insufficient:
     * <ul>
     *   <li>Accessing provider-specific fields not covered by the common model.</li>
     *   <li>Implementing custom error handling or logging that requires the
     *       exact wire format.</li>
     *   <li>Chaining custom reactive operators before any transformation.</li>
     * </ul>
     * Each emitted {@link RawStreamResponse} typically contains the raw bytes
     * or string representation of a single SSE data line (after the "data:"
     * prefix) or an equivalent chunk from the streaming connection.
     * </p>
     * <p>
     * Note that the stream will include control messages (e.g., "[DONE]" in
     * OpenAI) which can be identified and handled downstream.
     * </p>
     *
     * @return a {@link Flux} of raw streaming responses; never {@code null}
     */
    Flux<RawStreamResponse> executeRaw();

    /**
     * Convenience method that executes the raw streaming request and applies
     * a custom {@link RawStreamResponseConverter} to each emitted raw chunk,
     * yielding a {@link Flux} of the converter's target type.
     * <p>
     * This is equivalent to calling {@code executeRaw().map(converter::convert)}.
     * It is particularly useful when you need to transform the raw chunks into
     * a domain-specific object or into a different intermediate representation
     * without explicitly subscribing to the raw flux.
     * </p>
     *
     * @param <R>       the output type after conversion, usually a
     *                  domain POJO or an aggregated result
     * @param converter a stateless transformation from
     *                  {@link RawStreamResponse} to type {@code R}
     * @return a {@link Flux} emitting the converted objects; never {@code null}
     */
    default <R> Flux<R> execute(RawStreamResponseConverter<R> converter) {
        return executeRaw().map(converter::convert);
    }

}