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
package pro.chenggang.project.reactive.ai.lite.core.interceptor;

import pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderGeneralResponseExchange;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderStreamResponseExchange;
import reactor.core.publisher.Mono;

/**
 * Interceptor that acts on a response after the underlying LLM provider has produced it,
 * but before the response is fully delivered to the calling application.
 * <p>
 * This is the "after" phase of the interception chain designed for response post-processing.
 * It enables cross-cutting concerns such as auditing, logging, metrics collection, or
 * response transformation without polluting core business logic.
 * </p>
 * <p>
 * Two distinct interception points are offered to handle the asynchronous nature of
 * LLM responses:
 * <ul>
 *   <li>{@link #interceptAfter(LlmProviderGeneralResponseExchange, LlmProviderResponseInterceptorChain)} — for
 *       non-streaming, complete responses; invoked once per request.</li>
 *   <li>{@link #interceptAfterEach(LlmProviderStreamResponseExchange, LlmProviderResponseInterceptorChain)} — for
 *       each chunk emitted in a streaming (SSE) response; invoked repeatedly as data arrives.</li>
 * </ul>
 * In both cases the interceptor must explicitly call {@code chain.next(exchange)} to
 * continue the chain, ensuring subsequent interceptors and the eventual response consumer
 * receive the processed exchange. Failure to call {@code next} will stall the response pipeline.
 * </p>
 * <p>
 * Implementations are expected to be thread-safe and non-blocking, returning a
 * {@link Mono} that completes when the interceptor's work is done. The reactive
 * pattern allows chaining of multiple "after" interceptors in a predictable order.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface LlmProviderExecutionAfterInterceptor extends LlmProviderExecutionInterceptor {

    /**
     * Processes a complete, non-streaming response after the LLM provider has returned it.
     * <p>
     * This method is called exactly once for each non-streaming request, after the provider
     * has generated the full response but before it is returned to the caller. It can inspect
     * or modify the exchange's response data and/or the associated context (e.g., timestamps,
     * proprietary metadata). For example, it may enrich the response with additional headers,
     * log the dialogue turn, or record performance metrics.
     * </p>
     * <p>
     * The provided {@code chain} must be used to propagate the exchange down the interceptor
     * chain. Call {@code chain.next(exchange)} when the interceptor's logic is complete;
     * this returns a {@link Mono} that represents the continuation of processing. The interceptor
     * can also decide to short‑circuit the chain by not invoking {@code next}, but this is
     * rarely desirable in a response post‑processing phase.
     * </p>
     *
     * @param exchange the general response exchange containing the full response and contextual information
     * @param chain    the response interceptor chain; call {@code chain.next(exchange)} to proceed
     * @return a {@link Mono} that signals completion of this interceptor's work; errors are propagated downstream
     */
    Mono<Void> interceptAfter(LlmProviderGeneralResponseExchange exchange, LlmProviderResponseInterceptorChain chain);

    /**
     * Processes an individual chunk of a streaming response after the LLM provider has emitted it.
     * <p>
     * When the LLM provider is configured for streaming (e.g., Server‑Sent Events), this
     * method is invoked for every data chunk as it becomes available. This allows per‑chunk
     * interventions such as token‑by‑token logging, content filtering, or real‑time rate
     * limiting. Each chunk is delivered via the {@code exchange} and can be altered before
     * the chain forwards it to the next interceptor or to the client.
     * </p>
     * <p>
     * As with the non‑streaming variant, the interceptor must call {@code chain.next(exchange)}
     * to continue the chain. Forgetting to do so will cause the stream to stall. Because this
     * method is called multiple times per request, implementations should avoid heavy
     * synchronization or blocking operations to maintain stream throughput.
     * </p>
     *
     * @param exchange the stream response exchange holding the current chunk and its metadata
     * @param chain    the response interceptor chain; call {@code chain.next(exchange)} to proceed
     * @return a {@link Mono} that completes when the interceptor has finished handling the chunk
     */
    Mono<Void> interceptAfterEach(LlmProviderStreamResponseExchange exchange, LlmProviderResponseInterceptorChain chain);
}