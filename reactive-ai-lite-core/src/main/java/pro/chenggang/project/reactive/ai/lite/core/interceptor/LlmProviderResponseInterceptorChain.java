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
 * Defines a chain of responsibility for intercepting and processing LLM provider responses
 * after the actual response is obtained.
 * <p>
 * Implementations of this interface are typically constructed by the framework and passed
 * to each {@link LlmProviderExecutionInterceptor} during the response interception phase.
 * The chain allows multiple interceptors to be applied sequentially in a pluggable manner,
 * following the standard Intercepting Filter pattern. Each interceptor in the chain receives
 * the current response exchange (either general/non-streaming or streaming) and must
 * invoke the corresponding {@code next()} method to delegate to the next interceptor,
 * eventually reaching a terminal operation (e.g., the final response handler or a no-op).
 * <p>
 * This asynchronous design, using {@link Mono}, ensures non-blocking execution and
 * full back‑pressure support with Project Reactor, making it suitable for reactive AI
 * pipelines. Interceptors are expected not to block the reactive stream and to gracefully
 * handle any errors within the chain.
 * <p>
 * The two overloads of {@code next()} allow the same chain to uniformly handle both
 * streaming and non-streaming responses, avoiding code duplication and enabling shared
 * processing logic (e.g., logging, metrics, security) across different response modes.
 * <p>
 * Interceptors must ensure they call the appropriate {@code next()} method; otherwise the
 * chain is broken and downstream processing (including the final conversation result) will
 * never be triggered.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see LlmProviderExecutionInterceptor
 */
public interface LlmProviderResponseInterceptorChain {

    /**
     * Invokes the next interceptor in the chain (or the terminal handler) for a general,
     * non-streaming response.
     * <p>
     * The provided {@link LlmProviderGeneralResponseExchange} carries the complete
     * response payload along with contextual information (e.g., conversation mode,
     * model details) that may have been enriched by preceding interceptors. The chain
     * guarantees that each interceptor receives the same exchange instance, allowing
     * mutable modifications that are visible to downstream handlers.
     * <p>
     * This method is called by the current interceptor when it has finished its processing
     * and wants to pass control further. If this is the last interceptor in the chain,
     * the terminal operation (e.g., returning the final response to the caller) is performed.
     *
     * @param exchange the current non-streaming response exchange, never {@code null}
     * @return a {@link Mono} that completes when the rest of the chain (or the terminal
     * operation) has finished processing; errors are propagated as an error signal
     */
    Mono<Void> next(LlmProviderGeneralResponseExchange exchange);

    /**
     * Invokes the next interceptor in the chain (or the terminal handler) for a streaming
     * response.
     * <p>
     * The {@link LlmProviderStreamResponseExchange} provides access to the stream of
     * response chunks and the context built so far. Since streaming responses are emitted
     * incrementally, the exchange may carry a reference to the original {@link reactor.core.publisher.Flux}
     * that can be transformed by interceptors (e.g., for rate‑limiting or content filtering).
     * <p>
     * As with the general response variant, this method is called by each interceptor
     * after it has completed its own processing. It ensures that the chain remains reactive
     * and non‑blocking, with the returned {@link Mono} signaling completion only when the
     * entire downstream processing (including consumption of the stream) is finished.
     *
     * @param exchange the current streaming response exchange, never {@code null}
     * @return a {@link Mono} that completes when the rest of the chain (or the terminal
     * operation) has finished processing all stream elements; errors are propagated
     * as an error signal
     */
    Mono<Void> next(LlmProviderStreamResponseExchange exchange);

}