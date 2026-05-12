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
package pro.chenggang.project.reactive.ai.lite.core.spec.defaults;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import pro.chenggang.project.reactive.ai.lite.core.spec.ExecutionContextSpec;
import pro.chenggang.project.reactive.ai.lite.core.spec.ProviderSpec;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * The default implementation of {@link ExecutionContextSpec}.
 * <p>
 * This class serves as the initial starting point for building an AI request
 * execution specification. It captures fundamental context settings like parent
 * parsingAttributes and custom context configuration callbacks before transitioning to
 * provider configuration.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class DefaultExecutionContextSpec implements ExecutionContextSpec {

    /**
     * The type of LLM client being utilized.
     */
    private final LlmClientType llmClientType;

    /**
     * The registry for looking up available LLM providers.
     */
    private final LlmProviderRegistry llmProviderRegistry;

    /**
     * Attributes inherited from a parent execution context, if any.
     */
    @Getter(AccessLevel.PROTECTED)
    private Map<String, Object> parentAttributes;

    /**
     * A consumer to perform custom configuration on the execution context.
     */
    @Getter(AccessLevel.PROTECTED)
    private Consumer<ExecutionContext> contextConfigure;

    /**
     * Creates a new instance of {@link DefaultExecutionContextSpec}.
     *
     * @param llmClientType       the type of LLM client
     * @param llmProviderRegistry the registry for looking up available providers
     * @return a new {@link DefaultExecutionContextSpec} instance
     */
    public static DefaultExecutionContextSpec of(@NonNull LlmClientType llmClientType, @NonNull LlmProviderRegistry llmProviderRegistry) {
        return new DefaultExecutionContextSpec(llmClientType, llmProviderRegistry);
    }

    /**
     * Sets parsingAttributes inherited from a parent execution context.
     *
     * @param parentAttributes a map of parsingAttributes from the parent context
     * @return this instance for method chaining
     */
    @Override
    public ExecutionContextSpec parentAttributes(Map<String, Object> parentAttributes) {
        if (Objects.nonNull(parentAttributes)) {
            this.parentAttributes = parentAttributes;
        }
        return this;
    }

    /**
     * Provides a consumer for custom configuration of the execution context.
     *
     * @param contextConfigure a {@link Consumer} for custom configuration
     * @return this instance for method chaining
     */
    @Override
    public ExecutionContextSpec contextConfigure(@NonNull Consumer<ExecutionContext> contextConfigure) {
        this.contextConfigure = contextConfigure;
        return this;
    }

    /**
     * Transitions from context configuration to provider configuration.
     *
     * @return a new {@link ProviderSpec} instance initialized with the current state
     */
    @Override
    public ProviderSpec providerSpec() {
        return DefaultProviderSpec.of(llmClientType, llmProviderRegistry, this);
    }
}
