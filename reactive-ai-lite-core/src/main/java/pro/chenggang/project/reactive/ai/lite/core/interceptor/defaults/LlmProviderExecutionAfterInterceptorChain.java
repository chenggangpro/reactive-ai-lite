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
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderExchange;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderExecutionAfterInterceptor;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderInterceptorChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

/**
 * The Llm provider execution after interceptor chain.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Slf4j
public class LlmProviderExecutionAfterInterceptorChain implements LlmProviderInterceptorChain {

    private final LlmProviderExecutionAfterInterceptor currentInterceptor;
    private final LlmProviderExecutionAfterInterceptorChain next;

    public LlmProviderExecutionAfterInterceptorChain(List<LlmProviderExecutionAfterInterceptor> interceptors) {
        LlmProviderExecutionAfterInterceptorChain interceptor = init(interceptors);
        this.currentInterceptor = interceptor.currentInterceptor;
        this.next = interceptor.next;
    }

    private LlmProviderExecutionAfterInterceptorChain init(List<LlmProviderExecutionAfterInterceptor> interceptors) {
        LlmProviderExecutionAfterInterceptorChain interceptor = new LlmProviderExecutionAfterInterceptorChain(null, null);
        if (Objects.nonNull(interceptors) && !interceptors.isEmpty()) {
            ListIterator<? extends LlmProviderExecutionAfterInterceptor> iterator = interceptors.listIterator(0);
            while (iterator.hasNext()) {
                interceptor = new LlmProviderExecutionAfterInterceptorChain(iterator.next(), interceptor);
            }
        } else {
            log.debug("Llm provider execution after interceptors is empty");
        }
        return interceptor;
    }

    private LlmProviderExecutionAfterInterceptorChain(LlmProviderExecutionAfterInterceptor currentInterceptor,
                                                      LlmProviderExecutionAfterInterceptorChain next) {
        this.currentInterceptor = currentInterceptor;
        this.next = next;
    }

    @Override
    public Mono<Void> next(LlmProviderExchange exchange) {
        return Mono.defer(() -> {
            if (shouldIntercept()) {
                return this.currentInterceptor.interceptAfter(exchange, this.next);
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
