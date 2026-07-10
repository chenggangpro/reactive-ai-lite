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
import lombok.extern.slf4j.Slf4j;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.execution.StreamExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.defaults.LlmProviderExecutor;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.StreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ChatExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ChatExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmChatProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import reactor.core.publisher.Flux;

/**
 * The default {@link StreamExecution} implementation for LLM chat operations.
 * <p>
 * This class orchestrates a streaming chat request by dynamically resolving the appropriate
 * {@link LlmChatProvider} via the provided {@link LlmProviderRegistry} and delegating the
 * actual streaming call to it. The provider resolution and reactive execution plumbing are
 * encapsulated by an {@link LlmProviderExecutor}, which also manages the Reactor
 * {@link reactor.util.context.Context} to propagate execution‑specific attributes and
 * any configured context adjustments defined in the {@link ChatExecutionSpec}.
 * </p>
 * <p>
 * Two variants of streaming execution are provided:
 * <ul>
 *   <li><strong>Structured responses</strong> – returned as {@link StreamResponse} objects,
 *   where the raw JSON is parsed into a higher‑level abstraction by the provider.</li>
 *   <li><strong>Raw JSON responses</strong> – returned as {@link RawStreamResponse} objects,
 *   allowing the caller to handle the raw LLM output directly (e.g., for custom parsing,
 *   logging, or inspecting the original payload).</li>
 * </ul>
 * Both methods ensure that the {@link ExecutionContext} is initialized and available within
 * the reactive pipeline, making parent attributes and context configurators accessible to
 * downstream components.
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Slf4j
public class ChatStreamExecution implements StreamExecution {

    /**
     * The dedicated executor that manages provider resolution and reactive execution
     * for chat requests. It holds a reference to the {@link LlmProviderRegistry} and
     * the {@link ExecutionSpec} (specifically a {@link ChatExecutionSpec}), using generics
     * to handle {@link ChatExecutionInfo}-typed configuration.
     */
    private final LlmProviderExecutor<ChatExecutionInfo> llmProviderExecutor;

    /**
     * Private constructor to enforce creation through the static factory method.
     * Builds the internal {@link LlmProviderExecutor} from the supplied registry
     * and execution specification.
     *
     * @param llmProviderRegistry the registry used to look up the appropriate chat provider
     * @param executionSpec       the chat execution specification containing provider criteria,
     *                            prompt data, and execution parameters
     */
    private ChatStreamExecution(@NonNull LlmProviderRegistry llmProviderRegistry, @NonNull ChatExecutionSpec executionSpec) {
        this.llmProviderExecutor = LlmProviderExecutor.<ChatExecutionInfo>builder()
                .llmProviderRegistry(llmProviderRegistry)
                .executionSpec(executionSpec)
                .build();
    }

    /**
     * Creates a new {@link ChatStreamExecution} instance for streaming chat interactions.
     * <p>
     * This factory method encapsulates the instantiation logic, keeping the constructor
     * private and ensuring that all required dependencies are provided. The returned
     * execution object is ready to be called with {@link #execute()} or {@link #executeRaw()}.
     * </p>
     *
     * @param llmProviderRegistry the registry used to look up the appropriate chat provider
     * @param executionSpec       the chat execution specification containing provider criteria,
     *                            prompt data, and execution parameters
     * @return a new {@code ChatStreamExecution} instance, ready to start streaming
     */
    public static StreamExecution of(@NonNull LlmProviderRegistry llmProviderRegistry, @NonNull ChatExecutionSpec executionSpec) {
        return new ChatStreamExecution(llmProviderRegistry, executionSpec);
    }

    /**
     * Executes the streaming chat request and returns parsed, structured responses.
     * <p>
     * Internally, this method delegates to the {@link LlmProviderExecutor} which resolves
     * the target {@link LlmChatProvider} based on the configured criteria and then invokes
     * its {@link LlmChatProvider#executeStream} method. The resulting {@link Flux} of
     * {@link StreamResponse} is backed by a Reactor context that includes all attributes
     * defined in the parent attributes map of the execution spec, plus any custom context
     * modifications specified via {@link ChatExecutionSpec#getContextConfigure()}.
     * This ensures that downstream operations can access execution‑scoped metadata
     * (e.g., correlation IDs, user information) without manual propagation.
     * </p>
     *
     * @return a {@link Flux} of {@link StreamResponse} objects, each representing a
     *         parsed portion of the LLM response
     */
    @Override
    public Flux<StreamResponse> execute() {
        return llmProviderExecutor.executeFlux(LlmChatProvider.class, LlmChatProvider::executeStream)
                .contextWrite(context -> {
                    ExecutionSpec<ChatExecutionInfo> executionSpec = llmProviderExecutor.getExecutionSpec();
                    return ExecutionContext.initializeExecutionContext(context, executionSpec.getParentAttributes(), executionSpec.getContextConfigure());
                });
    }

    /**
     * Executes the streaming chat request and returns the raw JSON responses as they
     * were received from the LLM backend.
     * <p>
     * Similar to {@link #execute()}, this method uses the {@link LlmProviderExecutor} to
     * resolve the appropriate {@link LlmChatProvider}, but delegates to
     * {@link LlmChatProvider#executeStreamRaw} instead. The resulting {@link Flux} of
     * {@link RawStreamResponse} objects provides the original payload without any
     * post‑processing, enabling advanced use cases such as custom parsing, inspection,
     * or logging of the provider’s native streaming output.
     * The Reactor context is enriched in the same way as in the structured variant,
     * ensuring consistent access to execution metadata.
     * </p>
     *
     * @return a {@link Flux} of {@link RawStreamResponse} objects, each containing
     *         the raw JSON string from the LLM
     */
    @Override
    public Flux<RawStreamResponse> executeRaw() {
        return llmProviderExecutor.executeFlux(LlmChatProvider.class, LlmChatProvider::executeStreamRaw)
                .contextWrite(context -> {
                    ExecutionSpec<ChatExecutionInfo> executionSpec = llmProviderExecutor.getExecutionSpec();
                    return ExecutionContext.initializeExecutionContext(context, executionSpec.getParentAttributes(), executionSpec.getContextConfigure());
                });
    }

}