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
 * An interceptor that executes before the LLM request is sent to the provider.
 * <p>
 * Implementing this interface allows developers to inspect, log, or mutate the
 * outbound request data (such as the JSON body or headers) before it leaves the application.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface LlmProviderExecutionBeforeInterceptor extends LLmProviderExecutionInterceptor {

    /**
     * Intercepts the outbound request to the LLM provider.
     * <p>
     * Implementations must call {@code chain.next(exchange)} to continue the interception chain.
     * </p>
     *
     * @param exchange the request exchange containing the outbound data and context
     * @param chain    the request interceptor chain
     * @return a {@link Mono} representing the asynchronous completion of this interceptor's work
     */
    Mono<Void> interceptBefore(LlmProviderRequestExchange exchange, LlmProviderRequestInterceptorChain chain);

}
