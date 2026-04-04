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
package pro.chenggang.project.reactive.ai.lite.core.execution.defaults.chat;

import lombok.NonNull;
import org.springframework.core.ParameterizedTypeReference;
import pro.chenggang.project.reactive.ai.lite.core.execution.StructuredExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.defaults.LlmProviderExecutor;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.StructuredResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import reactor.core.publisher.Mono;

/**
 * The standard implementation of {@link StructuredExecution} for LLM chat operations.
 * <p>
 * This class orchestrates a request that mandates a structured, typed JSON response
 * from the AI model. It uses the {@link LlmProviderExecutor} to resolve the appropriate
 * provider and delegates the schema generation, request execution, and JSON deserialization
 * to the provider implementation.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public class ChatStructuredExecution implements StructuredExecution {

    /**
     * The executor responsible for resolving the provider and executing the request.
     */
    private final LlmProviderExecutor llmProviderExecutor;

    /**
     * Constructs a new {@link ChatStructuredExecution}.
     *
     * @param llmProviderRegistry the registry for looking up providers
     * @param executionSpec       the execution specification
     */
    private ChatStructuredExecution(@NonNull LlmProviderRegistry llmProviderRegistry, @NonNull ExecutionSpec executionSpec) {
        this.llmProviderExecutor = LlmProviderExecutor.builder()
                .llmProviderRegistry(llmProviderRegistry)
                .executionSpec(executionSpec)
                .build();
    }

    /**
     * Factory method for creating a new {@link ChatStructuredExecution}.
     *
     * @param llmProviderRegistry the registry for looking up providers
     * @param executionSpec       the execution specification
     * @return a new {@link ChatStructuredExecution} instance
     */
    public static ChatStructuredExecution of(@NonNull LlmProviderRegistry llmProviderRegistry, @NonNull ExecutionSpec executionSpec) {
        return new ChatStructuredExecution(llmProviderRegistry, executionSpec);
    }

    /**
     * Retrieves the underlying execution specification.
     *
     * @return the execution spec
     */
    @Override
    public ExecutionSpec executionSpec() {
        return this.llmProviderExecutor.getExecutionSpec();
    }

    /**
     * Executes the structured request using a class type for deserialization.
     */
    @Override
    public <R> Mono<StructuredResponse<R>> execute(@NonNull Class<R> resultType) {
        return llmProviderExecutor.executeChat((llmChatProvider, executionInfo) -> llmChatProvider.executeStructured(executionInfo, resultType));
    }

    /**
     * Executes the structured request using a parameterized type for deserialization.
     */
    @Override
    public <R> Mono<StructuredResponse<R>> execute(@NonNull ParameterizedTypeReference<R> resultType) {
        return llmProviderExecutor.executeChat((llmChatProvider, executionInfo) -> llmChatProvider.executeStructured(executionInfo, resultType));
    }

    /**
     * Executes the structured request using a raw JSON schema string.
     */
    @Override
    public Mono<RawResponse> executeRaw(@NonNull String responseJsonSchema) {
        return llmProviderExecutor.executeChat((llmChatProvider, executionInfo) -> llmChatProvider.executeStructuredRaw(executionInfo, responseJsonSchema));
    }

    /**
     * Executes the structured request using a class type to generate the schema, returning raw JSON.
     */
    @Override
    public <R> Mono<RawResponse> executeRaw(@NonNull Class<R> resultType) {
        return llmProviderExecutor.executeChat((llmChatProvider, executionInfo) -> llmChatProvider.executeStructuredRaw(executionInfo, resultType));
    }

    /**
     * Executes the structured request using a parameterized type to generate the schema, returning raw JSON.
     */
    @Override
    public <R> Mono<RawResponse> executeRaw(@NonNull ParameterizedTypeReference<R> resultType) {
        return llmProviderExecutor.executeChat((llmChatProvider, executionInfo) -> llmChatProvider.executeStructuredRaw(executionInfo, resultType));
    }

}
