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
 * Defines a chain of request interceptors that are executed sequentially before the actual
 * LLM request is dispatched.
 * <p>
 * This interface follows the standard Intercepting Filter or Chain of Responsibility pattern,
 * adapted for reactive programming. Each interceptor in the chain receives the current
 * {@link LlmProviderRequestExchange} and must explicitly invoke the {@link #next(LlmProviderRequestExchange)}
 * method to pass control to the next interceptor. This design allows interceptors to:
 * <ul>
 *   <li><strong>Inspect or modify</strong> the context before and after the chain proceeds.</li>
 *   <li><strong>Short-circuit</strong> the chain by not calling {@code next()}, for example to
 *       reject a request early due to authentication failure or to serve a cached response.</li>
 *   <li><strong>Perform non-blocking I/O</strong> (Mono-based) such as logging, metrics collection,
 *       or calling external services, without blocking the reactive pipeline.</li>
 * </ul>
 * The chain is typically assembled with a terminal operation (the actual LLM invocation) as its
 * final handler, ensuring that every interceptor runs before the request reaches the provider.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see LlmProviderRequestExchange
 */
public interface LlmProviderRequestInterceptorChain {

    /**
     * Proceeds to the next interceptor in the chain, or completes the interception process
     * if there are no more interceptors (i.e., reaching the terminal handler).
     * <p>
     * This method returns a {@link Mono}{@code <Void>} that signals the completion of the
     * entire chain execution. Implementors of interceptors should call this and typically
     * return the resulting Mono after applying any pre‑ or post‑processing logic. If an
     * interceptor does <strong>not</strong> call {@code next()}, subsequent interceptors and
     * the final LLM call are never executed—this is the mechanism for short-circuiting the
     * pipeline (e.g., returning a cached response or an error immediately).
     * </p>
     *
     * @param exchange the current request exchange containing request data, metadata, and
     *        mutable context that can be enriched or inspected by interceptors
     * @return a {@link Mono}{@code <Void>} that completes when the chain (and thus all
     *         downstream logic) has finished, or signals an error if any step fails
     */
    Mono<Void> next(LlmProviderRequestExchange exchange);

}