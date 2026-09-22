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
package pro.chenggang.project.reactive.ai.lite.core.execution.defaults.systemone;

import lombok.NonNull;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.execution.SystemOneExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.defaults.LlmProviderExecutor;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.SystemOneResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.SystemOneExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.SystemOneExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmSystemOneProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import reactor.core.publisher.Mono;

/**
 * Default implementation of {@link SystemOneExecution} for standard SystemOne requests.
 * <p>
 * Orchestrates provider resolution and request dispatching via {@link LlmProviderExecutor}.
 * Context propagation is automatically maintained in the reactor context with parent attributes
 * and context customization.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 * @see SystemOneExecution
 * @see LlmProviderExecutor
 * @see LlmSystemOneProvider
 */
public class SystemOneGeneralExecution implements SystemOneExecution {

    /**
     * Executor that orchestrates provider resolution and request execution.
     */
    private final LlmProviderExecutor<SystemOneExecutionInfo> llmProviderExecutor;

    /**
     * Private constructor enforcing creation through {@link #of(LlmProviderRegistry, SystemOneExecutionSpec)}.
     *
     * @param llmProviderRegistry the registry for looking up providers; must not be null
     * @param executionSpec       the SystemOne execution specification; must not be null
     */
    private SystemOneGeneralExecution(@NonNull LlmProviderRegistry llmProviderRegistry, @NonNull SystemOneExecutionSpec executionSpec) {
        this.llmProviderExecutor = LlmProviderExecutor.<SystemOneExecutionInfo>builder()
                .llmProviderRegistry(llmProviderRegistry)
                .executionSpec(executionSpec)
                .build();
    }

    /**
     * Static factory method to instantiate {@link SystemOneGeneralExecution}.
     *
     * @param llmProviderRegistry the provider registry; must not be null
     * @param executionSpec       the execution specification; must not be null
     * @return a new {@link SystemOneExecution} ready for execution
     */
    public static SystemOneExecution of(@NonNull LlmProviderRegistry llmProviderRegistry, @NonNull SystemOneExecutionSpec executionSpec) {
        return new SystemOneGeneralExecution(llmProviderRegistry, executionSpec);
    }

    /**
     * Executes the SystemOne operation and returns the domain {@link SystemOneResponse}.
     *
     * @return a {@link Mono} emitting the parsed {@link SystemOneResponse}
     */
    @Override
    public Mono<SystemOneResponse> execute() {
        return llmProviderExecutor.execute(LlmSystemOneProvider.class, LlmSystemOneProvider::executeSystemOne)
                .contextWrite(context -> {
                    ExecutionSpec<SystemOneExecutionInfo> executionSpec = llmProviderExecutor.getExecutionSpec();
                    return ExecutionContext.initializeExecutionContext(context, executionSpec.getParentAttributes(), executionSpec.getContextConfigure());
                });
    }

    /**
     * Executes the SystemOne operation and returns the raw JSON response.
     *
     * @return a {@link Mono} emitting the {@link RawResponse}
     */
    @Override
    public Mono<RawResponse> executeRaw() {
        return llmProviderExecutor.execute(LlmSystemOneProvider.class, LlmSystemOneProvider::executeSystemOneRaw)
                .contextWrite(context -> {
                    ExecutionSpec<SystemOneExecutionInfo> executionSpec = llmProviderExecutor.getExecutionSpec();
                    return ExecutionContext.initializeExecutionContext(context, executionSpec.getParentAttributes(), executionSpec.getContextConfigure());
                });
    }
}
