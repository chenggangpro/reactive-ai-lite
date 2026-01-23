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
 * Orchestrates the execution of LLM tasks by selecting an appropriate provider and preparing the execution context.
 * This class uses an {@link ExecutionSpec} to determine how to load a provider from the {@link LlmProviderRegistry}
 * and then executes a given function with the selected provider and necessary execution information.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class LlmProviderExecutor {

    @NonNull
    private final LlmProviderRegistry llmProviderRegistry;
    @Getter
    @NonNull
    private final ExecutionSpec executionSpec;

    /**
     * Executes a chat-based operation.
     * This method loads the appropriate {@link LlmChatProvider} based on the {@link ExecutionSpec},
     * creates the {@link ExecutionInfo}, and then applies the provided execution logic.
     *
     * @param specifiedExecution A {@link BiFunction} that defines the chat operation to be performed.
     *                           It takes an {@link LlmChatProvider} and {@link ExecutionInfo} and returns a result.
     * @param <T>                The type of the result returned by the execution.
     * @return The result of the specified execution.
     */
    public <T> T executeChat(@NonNull BiFunction<LlmChatProvider, ExecutionInfo, T> specifiedExecution) {
        ExecutionContext executionContext = this.executionSpec.newExecutionContext();
        LlmChatProvider llmProvider = this.loadLlmProvider(executionContext, LlmProviderRegistry::getChatProvider);
        ExecutionInfo executionInfo = executionSpec.newExecutionInfo(executionContext);
        return specifiedExecution.apply(llmProvider, executionInfo);
    }

    /**
     * Loads an LLM provider from the registry based on the current {@link ExecutionSpec}.
     * It handles selection of the default provider or a filtered provider based on the spec's configuration.
     *
     * @param executionContext The current execution context.
     * @param providerLoader   A function to load a specific type of provider (e.g., chat, embedding) from the registry.
     * @param <P>              The type of the {@link LlmProvider} to load.
     * @return The loaded LLM provider.
     */
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
