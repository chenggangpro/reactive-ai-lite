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
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderExecutionAfterInterceptor;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderResponseInterceptorChain;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderGeneralResponseExchange;
import pro.chenggang.project.reactive.ai.lite.core.interceptor.exchange.LlmProviderStreamResponseExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

/**
 * A concrete implementation of {@link LlmProviderResponseInterceptorChain}.
 * <p>
 * This class builds an immutable, linked-list-style execution chain of
 * {@link LlmProviderExecutionAfterInterceptor} instances. When the {@code next()}
 * method is called, it triggers the current interceptor and passes a reference
 * to the remainder of the chain. This allows interceptors to execute sequentially
 * after the LLM response is received.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Slf4j
public class LlmProviderExecutionAfterInterceptorChain implements LlmProviderResponseInterceptorChain {

    /**
     * The interceptor to execute at this point in the chain.
     */
    private final LlmProviderExecutionAfterInterceptor currentInterceptor;

    /**
     * The remainder of the interceptor chain.
     */
    private final LlmProviderExecutionAfterInterceptorChain next;

    /**
     * Constructs a new chain from a list of interceptors.
     * <p>
     * The list should be ordered according to the desired execution sequence.
     * The constructor builds the linked structure iteratively.
     * </p>
     *
     * @param interceptors the list of 'after' interceptors to form the chain
     */
    public LlmProviderExecutionAfterInterceptorChain(List<LlmProviderExecutionAfterInterceptor> interceptors) {
        LlmProviderExecutionAfterInterceptorChain interceptor = init(interceptors);
        this.currentInterceptor = interceptor.currentInterceptor;
        this.next = interceptor.next;
    }

    /**
     * Initializes the linked chain structure.
     *
     * @param interceptors the list of interceptors
     * @return the head of the initialized chain
     */
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

    /**
     * Private constructor for linking nodes in the chain.
     *
     * @param currentInterceptor the current node's interceptor
     * @param next               the next node in the chain
     */
    private LlmProviderExecutionAfterInterceptorChain(LlmProviderExecutionAfterInterceptor currentInterceptor,
                                                      LlmProviderExecutionAfterInterceptorChain next) {
        this.currentInterceptor = currentInterceptor;
        this.next = next;
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

    /**
     * Executes the next interceptor in the chain for a general response.
     *
     * @param exchange the general response exchange
     * @return a Mono representing completion of the chain execution
     */
    @Override
    public Mono<Void> next(LlmProviderGeneralResponseExchange exchange) {
        return Mono.defer(() -> {
            if (shouldIntercept()) {
                return this.currentInterceptor.interceptAfter(exchange, this.next);
            }
            return Mono.empty();
        });
    }

    /**
     * Executes the next interceptor in the chain for a stream response chunk.
     *
     * @param exchange the stream response exchange
     * @return a Mono representing completion of the chain execution
     */
    @Override
    public Mono<Void> next(LlmProviderStreamResponseExchange exchange) {
        return Mono.defer(() -> {
            if (shouldIntercept()) {
                return this.currentInterceptor.interceptAfterEach(exchange, this.next);
            }
            return Mono.empty();
        });
    }
}
