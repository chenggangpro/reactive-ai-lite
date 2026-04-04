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
import pro.chenggang.project.reactive.ai.lite.core.execution.StreamExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.defaults.LlmProviderExecutor;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.StreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmChatProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import reactor.core.publisher.Flux;

/**
 * The standard implementation of {@link StreamExecution} for LLM chat operations.
 * <p>
 * This class orchestrates a streaming chat request by delegating the dynamic
 * provider resolution to an {@link LlmProviderExecutor} and invoking the appropriate
 * streaming methods on the resolved {@link LlmChatProvider}.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public class ChatStreamExecution implements StreamExecution {

    /**
     * The executor responsible for resolving the provider and executing the request.
     */
    private final LlmProviderExecutor llmProviderExecutor;

    /**
     * Constructs a new {@link ChatStreamExecution}.
     *
     * @param llmProviderRegistry the registry for looking up providers
     * @param executionSpec       the execution specification
     */
    private ChatStreamExecution(@NonNull LlmProviderRegistry llmProviderRegistry, @NonNull ExecutionSpec executionSpec) {
        this.llmProviderExecutor = LlmProviderExecutor.builder()
                .llmProviderRegistry(llmProviderRegistry)
                .executionSpec(executionSpec)
                .build();
    }

    /**
     * Factory method for creating a new {@link ChatStreamExecution}.
     *
     * @param llmProviderRegistry the registry for looking up providers
     * @param executionSpec       the execution specification
     * @return a new {@link ChatStreamExecution} instance
     */
    public static ChatStreamExecution of(@NonNull LlmProviderRegistry llmProviderRegistry, @NonNull ExecutionSpec executionSpec) {
        return new ChatStreamExecution(llmProviderRegistry, executionSpec);
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
     * Executes the streaming chat request and returns parsed responses.
     *
     * @return a {@link Flux} of {@link StreamResponse}s
     */
    @Override
    public Flux<StreamResponse> execute() {
        return llmProviderExecutor.executeChat(LlmChatProvider::executeStream);
    }

    /**
     * Executes the streaming chat request and returns raw JSON responses.
     *
     * @return a {@link Flux} of {@link RawStreamResponse}s
     */
    @Override
    public Flux<RawStreamResponse> executeRaw() {
        return llmProviderExecutor.executeChat(LlmChatProvider::executeStreamRaw);
    }

}
