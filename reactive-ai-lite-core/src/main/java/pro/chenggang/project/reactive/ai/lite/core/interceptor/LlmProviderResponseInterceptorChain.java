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
 * Defines a chain of response interceptors that are executed sequentially after the actual
 * LLM response is received.
 * <p>
 * This interface follows the standard Intercepting Filter or Chain of Responsibility pattern.
 * Each interceptor in the chain receives the current response exchange (either general or stream)
 * and must invoke the corresponding {@code next()} method to pass control to the next interceptor,
 * eventually reaching the terminal operation.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface LlmProviderResponseInterceptorChain {

    /**
     * Proceeds to the next interceptor in the chain for a general (non-streaming) response,
     * or completes the interception process if there are no more interceptors.
     *
     * @param exchange the current general response exchange containing data and context
     * @return a {@link Mono} representing the asynchronous completion of the chain execution
     */
    Mono<Void> next(LlmProviderGeneralResponseExchange exchange);

    /**
     * Proceeds to the next interceptor in the chain for a streaming response,
     * or completes the interception process if there are no more interceptors.
     *
     * @param exchange the current stream response exchange containing data and context
     * @return a {@link Mono} representing the asynchronous completion of the chain execution
     */
    Mono<Void> next(LlmProviderStreamResponseExchange exchange);

}
