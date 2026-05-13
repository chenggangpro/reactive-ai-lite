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
 * An interceptor that executes after the LLM provider has responded, but before
 * the response is fully returned to the calling application.
 * <p>
 * Implementing this interface allows developers to inspect, log, or mutate the
 * response data (either a single general response or individual stream chunks)
 * returned by the LLM provider.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface LlmProviderExecutionAfterInterceptor extends LLmProviderExecutionInterceptor {

    /**
     * Intercepts a general, non-streaming response from the LLM provider.
     * <p>
     * Implementations must call {@code chain.next(exchange)} to continue the interception chain.
     * </p>
     *
     * @param exchange the general response exchange containing response data and context
     * @param chain    the response interceptor chain
     * @return a {@link Mono} representing the asynchronous completion of this interceptor's work
     */
    Mono<Void> interceptAfter(LlmProviderGeneralResponseExchange exchange, LlmProviderResponseInterceptorChain chain);

    /**
     * Intercepts a single chunk of a streaming response from the LLM provider.
     * <p>
     * This method is invoked for every chunk emitted in the SSE stream.
     * Implementations must call {@code chain.next(exchange)} to continue the interception chain.
     * </p>
     *
     * @param exchange the stream response exchange containing the chunk data and context
     * @param chain    the response interceptor chain
     * @return a {@link Mono} representing the asynchronous completion of this interceptor's work
     */
    Mono<Void> interceptAfterEach(LlmProviderStreamResponseExchange exchange, LlmProviderResponseInterceptorChain chain);
}
