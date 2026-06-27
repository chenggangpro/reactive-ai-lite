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
import pro.chenggang.project.reactive.ai.lite.core.interceptor.LlmProviderInterceptorRegistry.InterceptedDataInfo.InterceptedDataInfoBuilder;
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
     * </p>
     *
     * @param interceptedDataInfo the metadata and payload of the request
     * @param generalExecution    the core execution logic that returns the raw JSON response
     * @return a {@link Mono} emitting the intercepted and potentially modified JSON response
     */
    Mono<ObjectNode> interceptGeneral(@NonNull InterceptedDataInfo interceptedDataInfo, @NonNull Mono<ObjectNode> generalExecution);

    /**
     * Intercepts a streaming execution.
     * <p>
     * Applies the registered 'before' interceptors to the request data, executes the provided
     * {@code streamExecution} Flux, and then applies the 'after' interceptors to each chunk
     * emitted by the stream.
     * </p>
     *
     * @param interceptedDataInfo the metadata and payload of the request
     * @param streamExecution     the core execution logic that returns a stream of raw responses
     * @return a {@link Flux} emitting the intercepted and potentially modified stream chunks
     */
    Flux<RawStreamResponse> interceptStream(@NonNull InterceptedDataInfo interceptedDataInfo, @NonNull Flux<RawStreamResponse> streamExecution);

    /**
     * Creates a new builder for constructing an {@link InterceptedDataInfo} instance.
     *
     * @return a new {@link InterceptedDataInfoBuilder}
     */
    static InterceptedDataInfoBuilder newInterceptedDataInfoBuilder() {
        return InterceptedDataInfo.builder();
    }

    /**
     * A data container holding all necessary context and payload information for interceptors
     * to inspect or modify during an execution cycle.
     *
     * @author Gang Cheng
     */
    @Getter
    @Builder
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    class InterceptedDataInfo {

        /**
         * The type of LLM client making the request (e.g., CHAT, IMAGE).
         */
        @NonNull
        private final LlmClientType clientType;

        /**
         * The metadata of the LLM provider handling the request.
         */
        @NonNull
        private final LlmProviderInfo llmProviderInfo;

        /**
         * The execution context, useful for passing correlation IDs or tracing data.
         */
        @NonNull
        private final ExecutionContext executionContext;

        /**
         * The raw JSON request body that will be sent to the provider.
         */
        @NonNull
        private final ObjectNode rawRequestBody;

        /**
         * A convenience method to trigger the interception of a general execution using this data info.
         *
         * @param lLmProviderInterceptorRegistry the registry to apply
         * @param generalExecution               the core execution mono
         * @return the intercepted Mono
         */
        public Mono<ObjectNode> interceptGeneral(@NonNull LlmProviderInterceptorRegistry lLmProviderInterceptorRegistry, @NonNull Mono<ObjectNode> generalExecution) {
            return lLmProviderInterceptorRegistry.interceptGeneral(this, generalExecution);
        }

        /**
         * A convenience method to trigger the interception of a stream execution using this data info.
         *
         * @param lLmProviderInterceptorRegistry the registry to apply
         * @param streamExecution                the core execution flux
         * @return the intercepted Flux
         */
        public Flux<RawStreamResponse> interceptStream(@NonNull LlmProviderInterceptorRegistry lLmProviderInterceptorRegistry, @NonNull Flux<RawStreamResponse> streamExecution) {
            return lLmProviderInterceptorRegistry.interceptStream(this, streamExecution);
        }
    }

}
