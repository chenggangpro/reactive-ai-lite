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
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.execution.GeneralExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.defaults.LlmProviderExecutor;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.GeneralResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ChatExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ChatExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmChatProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import reactor.core.publisher.Mono;

/**
 * Default implementation of {@link GeneralExecution} specialized for LLM chat interactions.
 * <p>
 * This execution variant is designed for non-streaming (general) chat requests. It delegates
 * the actual invocation to an {@link LlmChatProvider} that is dynamically resolved from the
 * {@link LlmProviderRegistry} based on the provided {@link ChatExecutionSpec}. The resolution
 * logic is encapsulated in {@link LlmProviderExecutor}, ensuring that the appropriate provider
 * (e.g., OpenAI, Anthropic) is selected at runtime depending on configuration and context.
 * </p>
 * <p>
 * The execution flow is orchestrated reactively using Project Reactor. Both {@link #execute()}
 * and {@link #executeRaw()} methods enrich the reactive context with execution-scoped attributes
 * via {@link Mono#contextWrite}, using {@link ExecutionContext#initializeExecutionContext} to
 * propagate parent attributes and any custom context configuration defined in the
 * {@link ExecutionSpec}. This ensures that all downstream operations (including provider calls)
 * have access to essential metadata such as user IDs, session keys, or tracing information.
 * </p>
 * <p>
 * The class is instantiated only through the static factory method {@link #of(LlmProviderRegistry, ChatExecutionSpec)},
 * keeping the constructor private to enforce a controlled creation path and prevent misuse.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
public class ChatGeneralExecution implements GeneralExecution {

    /**
     * The executor component that handles provider lookup and request delegation.
     * <p>
     * Built once during construction, it caches the registry and execution spec to avoid
     * repetitive introspection. The executor is parameterized with {@link ChatExecutionInfo}
     * because this concrete execution type works with chat-specific configuration and state.
     * </p>
     */
    private final LlmProviderExecutor<ChatExecutionInfo> llmProviderExecutor;

    /**
     * Private constructor to enforce creation through the static factory method
     * {@link #of(LlmProviderRegistry, ChatExecutionSpec)}.
     * <p>
     * This constructor immediately constructs the {@link LlmProviderExecutor} using the provided
     * registry and specification. The executor is then reused for all subsequent
     * {@link #execute()} and {@link #executeRaw()} calls, ensuring consistent provider resolution
     * and configuration throughout the lifetime of this execution.
     * </p>
     *
     * @param llmProviderRegistry the registry that holds all available {@link LlmChatProvider} instances
     * @param executionSpec       the specification containing provider selection criteria and
     *                            execution parameters for chat operations
     */
    private ChatGeneralExecution(@NonNull LlmProviderRegistry llmProviderRegistry, @NonNull ChatExecutionSpec executionSpec) {
        this.llmProviderExecutor = LlmProviderExecutor.<ChatExecutionInfo>builder()
                .llmProviderRegistry(llmProviderRegistry)
                .executionSpec(executionSpec)
                .build();
    }

    /**
     * Static factory method to create a new {@link ChatGeneralExecution} instance.
     * <p>
     * This method encapsulates the construction logic and returns the instance as a
     * {@link GeneralExecution}, hiding the concrete implementation type from callers.
     * It ensures that the required registry and specification are not null (via Lombok's
     * {@link NonNull}) and initializes the internal executor in a ready-to-use state.
     * </p>
     *
     * @param llmProviderRegistry the registry used to locate the chat provider
     * @param executionSpec       the specification that defines which provider to use and how
     * @return a fully initialized {@link GeneralExecution} for chat operations
     */
    public static GeneralExecution of(@NonNull LlmProviderRegistry llmProviderRegistry, @NonNull ChatExecutionSpec executionSpec) {
        return new ChatGeneralExecution(llmProviderRegistry, executionSpec);
    }

    /**
     * Executes the general chat request and returns a structured {@link GeneralResponse}.
     * <p>
     * This method triggers the synchronous (from the caller's perspective) chat operation
     * through the resolved {@link LlmChatProvider}. Behind the scenes, the provider's
     * {@link LlmChatProvider#executeGeneral(ChatExecutionInfo)} is invoked with the
     * {@link ChatExecutionInfo} instance derived from the spec.
     * </p>
     * <p>
     * Before execution, the reactive context is enriched with execution attributes from
     * the spec's parent attributes and any custom context configuration. This enrichment
     * uses {@link ExecutionContext#initializeExecutionContext}, which sets up a
     * per-execution context map (e.g., for logging, tracing, or user data) that is
     * accessible within downstream Reactor operators.
     * </p>
     *
     * @return a {@link Mono} that emits the parsed {@link GeneralResponse} upon completion
     */
    @Override
    public Mono<GeneralResponse> execute() {
        return llmProviderExecutor.execute(LlmChatProvider.class, LlmChatProvider::executeGeneral)
                .contextWrite(context -> {
                    ExecutionSpec<ChatExecutionInfo> executionSpec = llmProviderExecutor.getExecutionSpec();
                    return ExecutionContext.initializeExecutionContext(context, executionSpec.getParentAttributes(), executionSpec.getContextConfigure());
                });
    }

    /**
     * Executes the general chat request and returns the raw JSON response as a {@link RawResponse}.
     * <p>
     * Similar to {@link #execute()}, this method delegates to the resolved chat provider,
     * but calls {@link LlmChatProvider#executeGeneralRaw(ChatExecutionInfo)} instead, which
     * bypasses any parsing/transformation and returns the provider's raw output. This is
     * useful when the caller needs access to the original response envelope, e.g., for
     * logging, auditing, or custom post-processing.
     * </p>
     * <p>
     * As with the standard execution, the reactive context is initialized with the same
     * execution attributes to ensure consistency across calls.
     * </p>
     *
     * @return a {@link Mono} that emits the raw {@link RawResponse} containing the full
     *         provider response in its original form
     */
    @Override
    public Mono<RawResponse> executeRaw() {
        return llmProviderExecutor.execute(LlmChatProvider.class, LlmChatProvider::executeGeneralRaw)
                .contextWrite(context -> {
                    ExecutionSpec<ChatExecutionInfo> executionSpec = llmProviderExecutor.getExecutionSpec();
                    return ExecutionContext.initializeExecutionContext(context, executionSpec.getParentAttributes(), executionSpec.getContextConfigure());
                });
    }

}