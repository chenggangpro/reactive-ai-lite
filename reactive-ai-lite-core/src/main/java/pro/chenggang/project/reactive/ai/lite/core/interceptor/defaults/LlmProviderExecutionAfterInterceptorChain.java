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
 * A concrete implementation of {@link LlmProviderResponseInterceptorChain} that forms a
 * linked-list execution chain for post-processing LLM responses.
 * <p>
 * This chain is built from a list of {@link LlmProviderExecutionAfterInterceptor} instances,
 * which are designed to intercept after a general or streaming LLM response has been obtained.
 * The chain implements the classic chain-of-responsibility pattern in a reactive manner:
 * each interceptor receives a reference to the next node in the chain, allowing it to either
 * process the response and delegate to the rest of the chain, or to short‑circuit the execution.
 * <p>
 * Construction prepends each interceptor from the provided list, so the resulting execution
 * order is the reverse of the original list order. In other words, the last element in the
 * list becomes the first interceptor to execute. This design is intentional and ensures that
 * the chain always terminates with a sentinel node (where both {@link #currentInterceptor}
 * and {@link #next} are {@code null}).
 * <p>
 * The chain supports two kinds of response exchanges:
 * <ul>
 *     <li>{@link LlmProviderGeneralResponseExchange} – for complete, non‑streaming responses</li>
 *     <li>{@link LlmProviderStreamResponseExchange} – for individual streaming chunks</li>
 * </ul>
 * Each is handled by a separate {@link #next} overload, which uses {@link Mono#defer}
 * to lazily trigger the chain, respecting the reactive contract.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Slf4j
public class LlmProviderExecutionAfterInterceptorChain implements LlmProviderResponseInterceptorChain {

    /**
     * The interceptor to invoke at this position in the chain.
     * <p>
     * If this node is the sentinel (end of chain), this field is {@code null}.
     */
    private final LlmProviderExecutionAfterInterceptor currentInterceptor;

    /**
     * The remainder of the chain, i.e., the next node to be invoked after this one.
     * <p>
     * At the sentinel node, this field is also {@code null}, representing the end of the chain.
     */
    private final LlmProviderExecutionAfterInterceptorChain next;

    /**
     * Constructs an interceptor chain from the given list of after‑execution interceptors.
     * <p>
     * The chain is built by prepending each interceptor in iteration order, resulting in an
     * execution sequence that is the reverse of the input list. This constructor does not
     * perform any additional validation beyond what {@link #init(List)} provides.
     *
     * @param interceptors the list of interceptors; may be {@code null} or empty, in which case
     *                     the chain consists solely of the sentinel node.
     */
    public LlmProviderExecutionAfterInterceptorChain(List<LlmProviderExecutionAfterInterceptor> interceptors) {
        LlmProviderExecutionAfterInterceptorChain interceptor = init(interceptors);
        this.currentInterceptor = interceptor.currentInterceptor;
        this.next = interceptor.next;
    }

    /**
     * Prepares the chain by iterating through the list of interceptors and constructing
     * the linked nodes.
     * <p>
     * It starts with a sentinel node (both fields null). For each interceptor in the list,
     * a new node is created with the interceptor as the current interceptor and the previous
     * node as the next node, thereby prepending elements and reversing the order.
     * If the list is {@code null} or empty, the sentinel node is returned as‑is.
     *
     * @param interceptors the list of interceptors to build from
     * @return the head of the constructed chain (which may be the sentinel)
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
     * Private constructor for creating a single node within the chain.
     * <p>
     * This constructor is only invoked from {@link #init(List)} during chain construction.
     *
     * @param currentInterceptor the interceptor for this node; may be {@code null} for the sentinel.
     * @param next               the next node in the chain; may be {@code null} for the sentinel.
     */
    private LlmProviderExecutionAfterInterceptorChain(LlmProviderExecutionAfterInterceptor currentInterceptor,
                                                      LlmProviderExecutionAfterInterceptorChain next) {
        this.currentInterceptor = currentInterceptor;
        this.next = next;
    }

    /**
     * Checks whether this node represents a valid interceptor position that should be invoked.
     * <p>
     * A node is considered interceptable when both {@link #currentInterceptor} and {@link #next}
     * are non‑{@code null}. The sentinel node has both fields set to {@code null} and will return
     * {@code false}, signaling the end of the chain.
     *
     * @return {@code true} if this node should perform an interception; {@code false} otherwise.
     */
    private boolean shouldIntercept() {
        return this.currentInterceptor != null && this.next != null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[currentInterceptor=" + this.currentInterceptor + "]";
    }

    /**
     * Initiates the chain for a general (non‑streaming) LLM response.
     * <p>
     * The chain is executed lazily via {@link Mono#defer}. If this node is a valid intercept‑point
     * ({@link #shouldIntercept()} returns {@code true}), the current interceptor's
     * {@link LlmProviderExecutionAfterInterceptor#interceptAfter} method is called, passing
     * the exchange and the next chain node. If this is the sentinel, an empty {@link Mono} is
     * returned, effectively terminating the reactive sequence.
     *
     * @param exchange the general response exchange containing the LLM response data
     * @return a {@link Mono} that completes when the chain processing finishes
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
     * Initiates the chain for a streaming LLM response chunk.
     * <p>
     * The chain is executed lazily via {@link Mono#defer}. If this node is a valid intercept‑point
     * ({@link #shouldIntercept()} returns {@code true}), the current interceptor's
     * {@link LlmProviderExecutionAfterInterceptor#interceptAfterEach} method is called, passing
     * the exchange and the next chain node. If this is the sentinel, an empty {@link Mono} is
     * returned, terminating the reactive sequence.
     *
     * @param exchange the stream response exchange containing a single streaming chunk
     * @return a {@link Mono} that completes when the chain processing for this chunk finishes
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