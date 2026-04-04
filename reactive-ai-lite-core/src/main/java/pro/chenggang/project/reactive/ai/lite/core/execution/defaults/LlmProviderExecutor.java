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
package pro.chenggang.project.reactive.ai.lite.core.execution.defaults;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContextView;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.option.Capability;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmChatProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Orchestrates the execution of LLM tasks by resolving the appropriate provider
 * and preparing the runtime execution context.
 * <p>
 * This class uses an {@link ExecutionSpec} to determine how to dynamically look up
 * a provider from the {@link LlmProviderRegistry}. Once the provider and context
 * are resolved, it executes the given functional logic (e.g., executing a chat request).
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class LlmProviderExecutor {

    /**
     * The registry used to look up and manage available LLM providers.
     */
    @NonNull
    private final LlmProviderRegistry llmProviderRegistry;

    /**
     * The specification containing all configurations and dynamic functions for this execution.
     */
    @Getter
    @NonNull
    private final ExecutionSpec executionSpec;

    /**
     * Executes a chat-based operation.
     * <p>
     * This method resolves the runtime {@link ExecutionContext}, loads the appropriate
     * {@link LlmChatProvider} based on the {@link ExecutionSpec}'s filtering rules,
     * creates the finalized {@link ExecutionInfo}, and then applies the provided execution logic.
     * </p>
     *
     * @param specifiedExecution a {@link BiFunction} defining the specific chat operation to perform.
     *                           It receives the resolved provider and execution info.
     * @param <T>                the type of the result returned by the execution (e.g., Mono or Flux)
     * @return the result of the specified execution
     */
    public <T> T executeChat(@NonNull BiFunction<LlmChatProvider, ExecutionInfo, T> specifiedExecution) {
        ExecutionContext executionContext = this.executionSpec.newExecutionContext();
        LlmChatProvider llmProvider = this.loadLlmProvider(executionContext, LlmProviderRegistry::getChatProvider);
        ExecutionInfo executionInfo = executionSpec.newExecutionInfo(executionContext);
        return specifiedExecution.apply(llmProvider, executionInfo);
    }

    /**
     * Dynamically loads an LLM provider from the registry based on the current execution specification.
     * <p>
     * It evaluates whether to use the default provider for the required capability, or whether
     * to apply a custom filter predicate against the available providers' metadata and the current context.
     * </p>
     *
     * @param executionContext the current runtime execution context
     * @param providerLoader   a function that interacts with the registry to find a provider matching a predicate
     * @param <P>              the type of the {@link LlmProvider} to load
     * @return the loaded LLM provider
     */
    @SuppressWarnings("unchecked")
    public <P extends LlmProvider> P loadLlmProvider(@NonNull ExecutionContext executionContext, @NonNull BiFunction<LlmProviderRegistry, Predicate<LlmProviderInfo>, P> providerLoader) {
        Capability capability = executionSpec.getLlmClientType().getCapability();
        if (executionSpec.isDefaultProvider()) {
            return (P) llmProviderRegistry.getDefaultProvider(capability);
        }
        ExecutionContextView executionContextView = executionContext.getContextView();
        return providerLoader.apply(llmProviderRegistry, llmProviderInfo -> {
                    if (Objects.isNull(executionSpec.getProviderFilter())) {
                        return true;
                    }
                    return executionSpec.getProviderFilter().test(llmProviderInfo, executionContextView);
                }
        );
    }
}
