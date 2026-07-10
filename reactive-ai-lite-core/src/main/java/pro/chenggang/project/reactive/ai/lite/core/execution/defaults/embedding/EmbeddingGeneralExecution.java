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
package pro.chenggang.project.reactive.ai.lite.core.execution.defaults.embedding;

import lombok.NonNull;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.execution.EmbeddingExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.defaults.LlmProviderExecutor;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.EmbeddingResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.EmbeddingExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.EmbeddingExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmEmbeddingProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import reactor.core.publisher.Mono;

/**
 * Default implementation of {@link EmbeddingExecution} for standard (non-streaming) embedding requests.
 * <p>
 * This class is the execution entry point for synchronous-style embedding invocations. It leverages
 * the {@link LlmProviderExecutor} to dynamically resolve the most appropriate {@link LlmEmbeddingProvider}
 * from the {@link LlmProviderRegistry} based on the configuration defined in the given
 * {@link EmbeddingExecutionSpec}. The resolved provider then handles the actual embedding operation,
 * returning either a parsed {@link EmbeddingResponse} or a raw JSON {@link RawResponse}.
 * </p>
 * <p>
 * The reactor context is automatically initialized with parent attributes and any context customization
 * provided by the specification, ensuring consistent context propagation throughout the execution chain.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see EmbeddingExecution
 * @see LlmProviderExecutor
 */
public class EmbeddingGeneralExecution implements EmbeddingExecution {

    /**
     * Executor that orchestrates provider resolution and request execution.
     * <p>
     * This field holds a pre-configured {@link LlmProviderExecutor} instance that combines
     * the provider registry and execution specification. It acts as the execution engine,
     * dynamically selecting the appropriate embedding provider at runtime and invoking
     * the correct method.
     * </p>
     */
    private final LlmProviderExecutor<EmbeddingExecutionInfo> llmProviderExecutor;

    /**
     * Private constructor, enforcing object creation via the static factory method
     * {@link #of(LlmProviderRegistry, EmbeddingExecutionSpec)}.
     * <p>
     * The constructor builds the {@link LlmProviderExecutor} with the provided registry
     * and specification, locking in the configuration needed for all subsequent executions.
     * This design centralizes the executor configuration and ensures immutability.
     * </p>
     *
     * @param llmProviderRegistry the registry for looking up providers, must not be null
     * @param executionSpec       the embedding execution specification, must not be null
     */
    private EmbeddingGeneralExecution(@NonNull LlmProviderRegistry llmProviderRegistry, @NonNull EmbeddingExecutionSpec executionSpec) {
        this.llmProviderExecutor = LlmProviderExecutor.<EmbeddingExecutionInfo>builder()
                .llmProviderRegistry(llmProviderRegistry)
                .executionSpec(executionSpec)
                .build();
    }

    /**
     * Static factory method that creates a ready-to-use {@link EmbeddingGeneralExecution} instance.
     * <p>
     * This method serves as the canonical way to instantiate the execution. It hides the
     * private constructor and provides a fluent API for callers. The returned instance
     * encapsulates the provider registry and execution spec, immediately available for
     * executing embedding requests.
     * </p>
     *
     * @param llmProviderRegistry the registry for looking up providers, must not be null
     * @param executionSpec       the embedding execution specification, must not be null
     * @return a new {@link EmbeddingExecution} instance ready for use
     */
    public static EmbeddingExecution of(@NonNull LlmProviderRegistry llmProviderRegistry, @NonNull EmbeddingExecutionSpec executionSpec) {
        return new EmbeddingGeneralExecution(llmProviderRegistry, executionSpec);
    }

    /**
     * Executes the embedding operation and returns a domain {@link EmbeddingResponse}.
     * <p>
     * The execution follows these steps:
     * <ol>
     *   <li>The executor resolves the concrete {@link LlmEmbeddingProvider} from the registry
     *       based on the embedded execution specification.</li>
     *   <li>The provider's {@code executeEmbedding} method is invoked to perform the actual
     *       embedding and parse the result.</li>
     *   <li>The reactor context is augmented with parent attributes and custom context
     *       configuration from the specification, ensuring downstream subscribers have
     *       the necessary contextual data.</li>
     * </ol>
     * This method returns a cold {@link Mono} that, when subscribed to, triggers the entire
     * reactive embedding pipeline.
     * </p>
     *
     * @return a {@link Mono} emitting an {@link EmbeddingResponse} upon successful execution
     */
    @Override
    public Mono<EmbeddingResponse> execute() {
        return llmProviderExecutor.execute(LlmEmbeddingProvider.class, LlmEmbeddingProvider::executeEmbedding)
                .contextWrite(context -> {
                    ExecutionSpec<EmbeddingExecutionInfo> executionSpec = llmProviderExecutor.getExecutionSpec();
                    return ExecutionContext.initializeExecutionContext(context, executionSpec.getParentAttributes(), executionSpec.getContextConfigure());
                });
    }

    /**
     * Executes the embedding operation and returns the raw JSON response without parsing.
     * <p>
     * Similar to {@link #execute()}, this method resolves the provider via the registry
     * and invokes the {@code executeEmbeddingRaw} method. The returned {@link RawResponse}
     * contains the unprocessed JSON payload from the LLM provider, allowing callers to
     * perform custom deserialization or inspection. The reactor context is initialized
     * identically to the parsed execution path, maintaining consistent attribute propagation.
     * </p>
     *
     * @return a {@link Mono} emitting a {@link RawResponse} with the raw provider output
     */
    @Override
    public Mono<RawResponse> executeRaw() {
        return llmProviderExecutor.execute(LlmEmbeddingProvider.class, LlmEmbeddingProvider::executeEmbeddingRaw)
                .contextWrite(context -> {
                    ExecutionSpec<EmbeddingExecutionInfo> executionSpec = llmProviderExecutor.getExecutionSpec();
                    return ExecutionContext.initializeExecutionContext(context, executionSpec.getParentAttributes(), executionSpec.getContextConfigure());
                });
    }

}