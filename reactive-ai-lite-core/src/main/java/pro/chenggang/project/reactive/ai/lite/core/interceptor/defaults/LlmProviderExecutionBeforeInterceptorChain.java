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
 * A concrete implementation of {@link LlmProviderRequestInterceptorChain}.
 * <p>
 * This class builds an immutable, linked-list-style execution chain of
 * {@link LlmProviderExecutionBeforeInterceptor} instances. When the {@code next()}
 * method is called, it triggers the current interceptor and passes a reference
 * to the remainder of the chain. This allows interceptors to execute sequentially
 * before the LLM request is dispatched.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Slf4j
public class LlmProviderExecutionBeforeInterceptorChain implements LlmProviderRequestInterceptorChain {

    /**
     * The interceptor to execute at this point in the chain.
     */
    private final LlmProviderExecutionBeforeInterceptor currentInterceptor;

    /**
     * The remainder of the interceptor chain.
     */
    private final LlmProviderExecutionBeforeInterceptorChain next;

    /**
     * Constructs a new chain from a list of interceptors.
     * <p>
     * The list should be ordered according to the desired execution sequence.
     * The constructor builds the linked structure iteratively from the end
     * of the list backwards to maintain the correct forward-execution order.
     * </p>
     *
     * @param interceptors the list of 'before' interceptors to form the chain
     */
    public LlmProviderExecutionBeforeInterceptorChain(List<LlmProviderExecutionBeforeInterceptor> interceptors) {
        LlmProviderExecutionBeforeInterceptorChain interceptor = init(interceptors);
        this.currentInterceptor = interceptor.currentInterceptor;
        this.next = interceptor.next;
    }

    /**
     * Initializes the linked chain structure by iterating backwards over the provided list.
     *
     * @param interceptors the list of interceptors
     * @return the head of the initialized chain
     */
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

    /**
     * Private constructor for linking nodes in the chain.
     *
     * @param currentInterceptor the current node's interceptor
     * @param next               the next node in the chain
     */
    private LlmProviderExecutionBeforeInterceptorChain(LlmProviderExecutionBeforeInterceptor currentInterceptor,
                                                       LlmProviderExecutionBeforeInterceptorChain next) {
        this.currentInterceptor = currentInterceptor;
        this.next = next;
    }

    /**
     * Executes the next interceptor in the chain for an outbound request.
     *
     * @param exchange the request exchange
     * @return a Mono representing completion of the chain execution
     */
    @Override
    public Mono<Void> next(LlmProviderRequestExchange exchange) {
        return Mono.defer(() -> {
            if (shouldIntercept()) {
                return this.currentInterceptor.interceptBefore(exchange, this.next);
            }
            return Mono.empty();
        });
    }

    /**
     * Determines if there is a valid interceptor to execute at this node.
     *
     * @return true if an interceptor and a next node exist, false otherwise (end of chain)
     */
    private boolean shouldIntercept() {
        return this.currentInterceptor != null && this.next != null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[currentInterceptor=" + this.currentInterceptor + "]";
    }
}
