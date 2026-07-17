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

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * A central registry for managing and applying interceptors to LLM requests and responses.
 * <p>
 * This interface defines the contract for a component that wraps the core execution of an LLM
 * request with a chain of interceptors. Interceptors can modify the request before it is sent
 * (e.g., logging, adding headers, mutating the JSON body) and can inspect or modify the
 * response after it is received (e.g., logging the response, extracting metadata).
 * </p>
 * <p>
 * Implementations typically maintain an ordered list of {@link LlmProviderInterceptor} instances
 * and apply them sequentially: first all "before" callbacks, then the actual provider execution,
 * then all "after" callbacks. This pattern centralizes cross-cutting concerns like logging,
 * authentication, monitoring, and request/response transformation.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface LlmProviderInterceptorRegistry {

    /**
     * Intercepts a general (non-streaming) execution.
     * <p>
     * Applies the registered 'before' interceptors to the request data, executes the provided
     * {@code generalExecution} Mono, and then applies the 'after' interceptors to the response data.
     * The {@code interceptedDataInfo} contains all necessary context for interceptors to make decisions.
     * The core execution logic is supplied as a {@link Mono} because it is a single asynchronous
     * operation.
     * </p>
     *
     * @param interceptedDataInfo the metadata and payload of the request, never null
     * @param generalExecution    the core execution logic that returns the raw response, never null
     * @param <T>                 the type of the raw response payload (e.g., ObjectNode or DataBuffer)
     * @return a {@link Mono} emitting the intercepted and potentially modified response
     */
    <T> Mono<T> interceptGeneral(@NonNull InterceptedDataInfo interceptedDataInfo, @NonNull Mono<T> generalExecution);

    /**
     * Intercepts a streaming execution.
     * <p>
     * Applies the registered 'before' interceptors to the request data, executes the provided
     * {@code streamExecution} Flux, and then applies the 'after' interceptors to each chunk
     * emitted by the stream. The stream is wrapped in a {@link Flux} to allow per‑item
     * post‑processing (e.g., logging each chunk, aggregating stream‑wide metrics).
     * </p>
     *
     * @param interceptedDataInfo the metadata and payload of the request, never null
     * @param streamExecution     the core execution logic that returns a stream of raw responses, never null
     * @param <T>                 the type of the raw stream payload (e.g., RawStreamResponse or DataBuffer)
     * @return a {@link Flux} emitting the intercepted and potentially modified stream chunks
     */
    <T> Flux<T> interceptStream(@NonNull InterceptedDataInfo interceptedDataInfo, @NonNull Flux<T> streamExecution);

    /**
     * A data container holding all necessary context and payload information for interceptors
     * to inspect or modify during an execution cycle.
     * <p>
     * This immutable object bundles the request raw data, execution context, client type,
     * and provider information so that interceptors can make informed decisions without external
     * lookups. It also provides convenience methods to trigger the registry directly from the
     * data object, improving code readability.
     * </p>
     *
     * @author Gang Cheng
     */
    @Getter
    @Builder
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class InterceptedDataInfo {

        /**
         * The type of LLM client making the request (e.g., CHAT, IMAGE).
         * Allows interceptors to differentiate behavior based on the interaction category.
         */
        @NonNull
        private final LlmClientType clientType;

        /**
         * The metadata of the LLM provider handling the request.
         * Contains information such as the provider name, model, and endpoint,
         * which interceptors may use for routing, logging, or header injection.
         */
        @NonNull
        private final LlmProviderInfo llmProviderInfo;

        /**
         * The execution context, useful for passing correlation IDs, tracing data,
         * or user‑specific attributes across the interceptor chain and the actual provider call.
         */
        @NonNull
        private final ExecutionContext executionContext;

        /**
         * The raw JSON request body that will be sent to the provider.
         * Interceptors may read or mutate this payload before the actual execution.
         */
        @NonNull
        private final ObjectNode rawRequestBody;

        /**
         * A convenience method to trigger the interception of a general execution using this data info.
         * <p>
         * Delegates to {@link LlmProviderInterceptorRegistry#interceptGeneral(InterceptedDataInfo, Mono)}
         * so that callers can invoke the interceptor chain directly from the data object,
         * reducing boilerplate.
         * </p>
         *
         * @param lLmProviderInterceptorRegistry the registry to apply, never null
         * @param generalExecution               the core execution mono, never null
         * @param <T>                            the type of the raw response payload
         * @return the intercepted Mono
         */
        public <T> Mono<T> interceptGeneral(@NonNull LlmProviderInterceptorRegistry lLmProviderInterceptorRegistry, @NonNull Mono<T> generalExecution) {
            return lLmProviderInterceptorRegistry.interceptGeneral(this, generalExecution);
        }

        /**
         * A convenience method to trigger the interception of a stream execution using this data info.
         * <p>
         * Delegates to {@link LlmProviderInterceptorRegistry#interceptStream(InterceptedDataInfo, Flux)}
         * so that callers can invoke the interceptor chain directly from the data object,
         * reducing boilerplate.
         * </p>
         *
         * @param lLmProviderInterceptorRegistry the registry to apply, never null
         * @param streamExecution                the core execution flux, never null
         * @param <T>                            the type of the raw stream payload
         * @return the intercepted Flux
         */
        public <T> Flux<T> interceptStream(@NonNull LlmProviderInterceptorRegistry lLmProviderInterceptorRegistry, @NonNull Flux<T> streamExecution) {
            return lLmProviderInterceptorRegistry.interceptStream(this, streamExecution);
        }
    }

}