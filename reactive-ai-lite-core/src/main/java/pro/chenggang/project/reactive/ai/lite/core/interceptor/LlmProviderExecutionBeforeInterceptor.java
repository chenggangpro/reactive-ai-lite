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

import pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderRequestExchange;
import reactor.core.publisher.Mono;

/**
 * An {@link LlmProviderExecutionInterceptor} that runs <strong>before</strong> the outbound
 * request is dispatched to the LLM provider.
 * <p>
 * Interceptors of this type are executed in the reactive pipeline after the full request
 * context (including the resolved provider configuration, model, and request body) has
 * been assembled but before the actual HTTP (or equivalent) call to the underlying
 * language model service is made.
 * </p>
 * <p><strong>Why use this interceptor?</strong></p>
 * <ul>
 *   <li><em>Authentication and Header Injection:</em> Dynamically attach API keys,
 *       tenant identifiers, or corporate authorization tokens to the provider request.</li>
 *   <li><em>Request Logging/Auditing:</em> Record the outgoing payload (masked if needed)
 *       for compliance or debugging purposes without altering the response flow.</li>
 *   <li><em>Payload Transformation:</em> Modify the raw JSON body (e.g., inject system
 *       prompts, adjust temperature/sampling parameters, or reroute to a different model)
 *       based on runtime conditions.</li>
 *   <li><em>Rate‑Limiting or Circuit Breaking:</em> Inject observability mechanisms that
 *       consume the exchange and decide whether the request should proceed, without
 *       affecting post‑processing interceptors.</li>
 * </ul>
 * <p>
 * Implementations <strong>must</strong> call {@link
 * LlmProviderRequestInterceptorChain#next(LlmProviderRequestExchange)} inside their
 * {@link #interceptBefore} method to pass control to the next interceptor (or to the actual
 * invocation). Failing to invoke the chain will short‑circuit the pipeline and prevent the
 * request from being sent.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see LlmProviderExecutionInterceptor
 * @see LlmProviderRequestInterceptorChain
 * @see LlmProviderRequestExchange
 * @since 0.1.0
 */
public interface LlmProviderExecutionBeforeInterceptor extends LlmProviderExecutionInterceptor {

    /**
     * Called when the outbound request is ready to be sent to the LLM provider. This is the
     * interception point where the request can be inspected, logged, or mutated before being
     * dispatched.
     * <p>
     * The {@code exchange} object provides access to the raw request body (typically a JSON
     * string), the resolved provider configuration, and any request‑level attributes.
     * Modifications made to the exchange (e.g., setting a new body or adding headers) are
     * visible to downstream interceptors and the final provider invocation.
     * </p>
     * <p>
     * The interception chain must be continued by calling
     * {@code chain.next(exchange)} . This call returns a
     * {@link Mono} that completes when all subsequent interceptors and the actual provider
     * call have finished (or failed). The returned {@code Mono} from this method should be
     * derived from that chain, so that any reactive signals (success, error, cancellation)
     * propagate correctly.
     * </p>
     * <p>
     * <strong>Reactive semantics:</strong> Since this is a {@code Mono<Void>}, the
     * interceptor itself does <em>not</em> produce a value. It can, however, signal an
     * error to abort the request pipeline. If the interceptor needs to perform
     * non‑blocking work (e.g., an asynchronous lookup of credentials), it should chain the
     * operation before calling {@code chain.next}.
     * </p>
     *
     * @param exchange the exchange that carries the outbound request data and context,
     *                 never {@code null}
     * @param chain    the chain of request interceptors; use {@code chain.next(exchange)} to
     *                 proceed, never {@code null}
     * @return a {@link Mono} that signals completion (or failure) of the entire
     *         request‑response lifecycle, as seen from this interceptor's perspective
     */
    Mono<Void> interceptBefore(LlmProviderRequestExchange exchange, LlmProviderRequestInterceptorChain chain);

}