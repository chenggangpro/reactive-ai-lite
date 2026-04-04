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
 * This interface follows the standard Intercepting Filter or Chain of Responsibility pattern.
 * Each interceptor in the chain receives the current {@link LlmProviderRequestExchange} and
 * must invoke the {@code next()} method to pass control to the next interceptor, eventually
 * reaching the terminal operation.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface LlmProviderRequestInterceptorChain {

    /**
     * Proceeds to the next interceptor in the chain, or completes the interception process
     * if there are no more interceptors.
     *
     * @param exchange the current request exchange containing request data and context
     * @return a {@link Mono} representing the asynchronous completion of the chain execution
     */
    Mono<Void> next(LlmProviderRequestExchange exchange);

}
