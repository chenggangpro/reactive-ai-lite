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
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContextView;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.TextMessage;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import pro.chenggang.project.reactive.ai.lite.core.spec.ConfigurableChatSpec;
import pro.chenggang.project.reactive.ai.lite.core.spec.ProviderSpec;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * The default implementation of the {@link ProviderSpec} interface.
 * @author Cheng Gang
 * @version 0.1.0
 */
public class DefaultProviderSpec implements ProviderSpec {

    private final LlmClientType llmClientType;
    private final LlmProviderRegistry llmProviderRegistry;
    private final DefaultExecutionContextSpec defaultExecutionContextSpec;

    @Getter(AccessLevel.PROTECTED)
    private boolean defaultProvider = false;
    @Getter(AccessLevel.PROTECTED)
    private boolean defaultProfile = false;
    @Getter(AccessLevel.PROTECTED)
    private BiPredicate<LlmProviderInfo, ExecutionContextView> providerFilter;
    @Getter(AccessLevel.PROTECTED)
    private BiFunction<ExecutionContextView, Set<String>, String> profilePicker;
    @Getter(AccessLevel.PROTECTED)
    private Function<ExecutionContextView, TextMessage> defaultSystemMessageProvider;

    protected DefaultProviderSpec(@NonNull LlmClientType llmClientType, @NonNull LlmProviderRegistry llmProviderRegistry, @NonNull DefaultExecutionContextSpec defaultExecutionContextSpec) {
        this.llmClientType = llmClientType;
        this.llmProviderRegistry = llmProviderRegistry;
        this.defaultExecutionContextSpec = defaultExecutionContextSpec;
    }

    public static DefaultProviderSpec of(@NonNull LlmClientType llmClientType, @NonNull LlmProviderRegistry llmProviderRegistry, @NonNull DefaultExecutionContextSpec defaultExecutionContextSpec) {
        return new DefaultProviderSpec(llmClientType, llmProviderRegistry, defaultExecutionContextSpec);
    }

    @Override
    public ProviderSpec defaultProvider() {
        this.defaultProvider = true;
        return this;
    }

    @Override
    public ProviderSpec firstProvider(@NonNull BiPredicate<LlmProviderInfo, ExecutionContextView> providerFilter) {
        this.defaultProvider = false;
        this.providerFilter = providerFilter;
        return this;
    }

    @Override
    public ProviderSpec defaultProfile() {
        this.defaultProfile = true;
        return this;
    }

    @Override
    public ProviderSpec profile(@NonNull BiFunction<ExecutionContextView, Set<String>, String> profilePicker) {
        this.defaultProfile = false;
        this.profilePicker = profilePicker;
        return this;
    }

    @Override
    public ProviderSpec defaultSystemMessage(@NonNull Function<ExecutionContextView, TextMessage> defaultSystemMessageProvider) {
        this.defaultSystemMessageProvider = defaultSystemMessageProvider;
        return this;
    }

    @Override
    public ConfigurableChatSpec chatSpec() {
        return new DefaultConfigurableChatSpec(this.llmClientType, this.llmProviderRegistry, defaultExecutionContextSpec, this);
    }
}
