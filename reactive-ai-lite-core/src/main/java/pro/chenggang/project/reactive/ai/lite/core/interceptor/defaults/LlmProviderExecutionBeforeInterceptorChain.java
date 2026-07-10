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
 * A concrete implementation of {@link LlmProviderRequestInterceptorChain} that chains multiple
 * {@link LlmProviderExecutionBeforeInterceptor} instances into a sequential execution pipeline.
 * <p>
 * The chain is constructed as an immutable, linked-list data structure. Each node in the chain
 * holds a reference to a single interceptor and the remainder of the chain. When {@link #next(LlmProviderRequestExchange)}
 * is invoked, the current interceptor's {@code interceptBefore} method is called and passed the
 * continuation chain (i.e., the remainder), enabling the interceptor to decide whether to proceed
 * with the next interceptor or short‑circuit the execution. This design follows the chain‑of‑responsibility
 * pattern tailored for reactive, non‑blocking LLM request processing.
 * </p>
 * <p>
 * The chain is built from a supplied list of interceptors. To maintain the correct forward‑execution
 * order, the list is traversed in reverse. The algorithm starts with a sentinel node (both fields
 * {@code null}) and prepends nodes for each list element, resulting in the first list element becoming
 * the head of the chain. An empty or {@code null} list yields a chain whose sentinel node is the head;
 * calls to {@link #next(LlmProviderRequestExchange)} on such a chain immediately return
 * {@link Mono#empty()}, effectively doing nothing.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Slf4j
public class LlmProviderExecutionBeforeInterceptorChain implements LlmProviderRequestInterceptorChain {

    /**
     * The interceptor responsible for processing the current node of the chain.
     * <p>
     * This field may be {@code null} only in the special sentinel node that marks the terminus of
     * the chain. In all other nodes it holds a non‑{@code null} instance of a
     * {@link LlmProviderExecutionBeforeInterceptor}.
     * </p>
     */
    private final LlmProviderExecutionBeforeInterceptor currentInterceptor;

    /**
     * A reference to the remainder of the interceptor chain.
     * <p>
     * For a regular node, this field points to the next node in the linked list (which may itself
     * be a regular node or the sentinel). For the sentinel node, it is {@code null}. The requirement
     * that {@code next} be non‑{@code null} for a node to be considered "executable" (see
     * {@link #shouldIntercept()}) ensures that the continuation is always valid when an interceptor
     * decides to invoke {@link LlmProviderRequestInterceptorChain#next(LlmProviderRequestExchange)}.
     * </p>
     */
    private final LlmProviderExecutionBeforeInterceptorChain next;

    /**
     * Constructs a new interceptor chain from the provided list.
     * <p>
     * The list order dictates the execution sequence: the first element is executed first.
     * Internally, the chain is built in reverse order to guarantee this behavior. If the
     * list is {@code null} or empty, the resulting chain will be a sentinel that performs
     * no operation.
     * </p>
     *
     * @param interceptors ordered list of "before" interceptors to form the chain; may be {@code null} or empty
     */
    public LlmProviderExecutionBeforeInterceptorChain(List<LlmProviderExecutionBeforeInterceptor> interceptors) {
        LlmProviderExecutionBeforeInterceptorChain interceptor = init(interceptors);
        this.currentInterceptor = interceptor.currentInterceptor;
        this.next = interceptor.next;
    }

    /**
     * Initializes the linked chain structure from a list of interceptors.
     * <p>
     * The construction proceeds backwards to preserve the logical order of the list.
     * A sentinel node with both fields set to {@code null} is first created; then the list
     * is iterated from the last element to the first. For each element a new chain node is
     * prepended, with the element as its {@code currentInterceptor} and the previously
     * created node as its {@code next}. The result is the head node of the chain. If the
     * list is {@code null} or empty, the sentinel itself is returned.
     * </p>
     *
     * @param interceptors the list of interceptors, possibly {@code null} or empty
     * @return the head of the interceptor chain
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
     * Private constructor used exclusively for assembling the linked chain nodes.
     * <p>
     * Each instance holds one interceptor and a reference to the next node. The
     * chain is immutable once constructed.
     * </p>
     *
     * @param currentInterceptor the interceptor for this node, or {@code null} for the sentinel
     * @param next               the next node in the chain, or {@code null} for the sentinel
     */
    private LlmProviderExecutionBeforeInterceptorChain(LlmProviderExecutionBeforeInterceptor currentInterceptor,
                                                       LlmProviderExecutionBeforeInterceptorChain next) {
        this.currentInterceptor = currentInterceptor;
        this.next = next;
    }

    /**
     * Executes the next step in the interceptor chain for an outbound request.
     * <p>
     * Delegates to {@link LlmProviderExecutionBeforeInterceptor#interceptBefore(LlmProviderRequestExchange, LlmProviderRequestInterceptorChain)}
     * if this node is executable (i.e., both {@code currentInterceptor} and {@code next} are non‑{@code null}).
     * The implementation uses {@link Mono#defer} to guarantee that the decision to invoke the interceptor
     * is made lazily, avoiding eager subscription management. If this node is not executable (e.g., the
     * chain's sentinel), the method returns {@link Mono#empty()} immediately, signaling that the
     * chain has been fully processed.
     * </p>
     *
     * @param exchange the request exchange carrying context and the outgoing request details
     * @return a {@code Mono} that completes when this portion of the chain has been handled; may be
     *         empty if the chain is exhausted
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
     * Determines whether this node is a valid, executable part of the chain.
     * <p>
     * A node is considered executable only if it possesses a non‑{@code null} interceptor
     * <strong>and</strong> a non‑{@code null} reference to the next node. This dual check
     * ensures that when the interceptor decides to call
     * {@link LlmProviderRequestInterceptorChain#next(LlmProviderRequestExchange)} on the
     * continuation, that continuation is also a valid chain instance. The sentinel node
     * (with both fields {@code null}) fails this check and therefore terminates the chain.
     * </p>
     *
     * @return {@code true} if this node can be invoked; {@code false} otherwise
     */
    private boolean shouldIntercept() {
        return this.currentInterceptor != null && this.next != null;
    }

    /**
     * Returns a string representation of this chain node, showing the class simple name
     * and the contained interceptor.
     *
     * @return a string describing this node
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[currentInterceptor=" + this.currentInterceptor + "]";
    }
}