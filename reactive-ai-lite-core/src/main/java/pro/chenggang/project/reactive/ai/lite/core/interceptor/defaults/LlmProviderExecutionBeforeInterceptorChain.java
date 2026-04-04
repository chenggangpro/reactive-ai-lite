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
package pro.chenggang.project.reactive.ai.lite.core.interceptor.defaults;

import lombok.extern.slf4j.Slf4j;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderExecutionBeforeInterceptor;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderRequestInterceptorChain;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderRequestExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

/**
 * The Llm provider execution before interceptor chain.
 *
 * @author Cheng Gang
 * @version 0.1.0
 * @since 0.1.0
 */
@Slf4j
public class LlmProviderExecutionBeforeInterceptorChain implements LlmProviderRequestInterceptorChain {

    private final LlmProviderExecutionBeforeInterceptor currentInterceptor;
    private final LlmProviderExecutionBeforeInterceptorChain next;

    public LlmProviderExecutionBeforeInterceptorChain(List<LlmProviderExecutionBeforeInterceptor> interceptors) {
        LlmProviderExecutionBeforeInterceptorChain interceptor = init(interceptors);
        this.currentInterceptor = interceptor.currentInterceptor;
        this.next = interceptor.next;
    }

    private LlmProviderExecutionBeforeInterceptorChain init(List<LlmProviderExecutionBeforeInterceptor> interceptors) {
        LlmProviderExecutionBeforeInterceptorChain interceptor = new LlmProviderExecutionBeforeInterceptorChain(null, null);
        if (Objects.nonNull(interceptors) && !interceptors.isEmpty()) {
            ListIterator<? extends LlmProviderExecutionBeforeInterceptor> iterator = interceptors.listIterator(interceptors.size());
            while (iterator.hasPrevious()) {
                interceptor = new LlmProviderExecutionBeforeInterceptorChain(iterator.previous(), interceptor);
            }
        } else {
            log.debug("Llm provider execution before interceptors is empty");
        }
        return interceptor;
    }

    private LlmProviderExecutionBeforeInterceptorChain(LlmProviderExecutionBeforeInterceptor currentInterceptor,
                                                       LlmProviderExecutionBeforeInterceptorChain next) {
        this.currentInterceptor = currentInterceptor;
        this.next = next;
    }

    @Override
    public Mono<Void> next(LlmProviderRequestExchange exchange) {
        return Mono.defer(() -> {
            if (shouldIntercept()) {
                return this.currentInterceptor.interceptBefore(exchange, this.next);
            }
            return Mono.empty();
        });
    }

    private boolean shouldIntercept() {
        return this.currentInterceptor != null && this.next != null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[currentInterceptor=" + this.currentInterceptor + "]";
    }
}
