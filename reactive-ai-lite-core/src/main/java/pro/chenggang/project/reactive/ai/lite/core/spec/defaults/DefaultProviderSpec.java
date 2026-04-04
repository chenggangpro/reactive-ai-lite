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
 * <p>
 * This class provides the standard logic for configuring and selecting AI providers
 * and their profiles within the reactive execution flow. It holds the configuration state
 * and passes it along to the next stage in the fluent API.
 * </p>
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public class DefaultProviderSpec implements ProviderSpec {

    /**
     * The type of LLM client being configured.
     */
    private final LlmClientType llmClientType;

    /**
     * The registry used to look up available providers.
     */
    private final LlmProviderRegistry llmProviderRegistry;

    /**
     * The execution context specification that preceded this provider configuration.
     */
    private final DefaultExecutionContextSpec defaultExecutionContextSpec;

    /**
     * Flag indicating whether to use the default provider.
     */
    @Getter(AccessLevel.PROTECTED)
    private boolean defaultProvider = false;

    /**
     * Flag indicating whether to use the default profile for the selected provider.
     */
    @Getter(AccessLevel.PROTECTED)
    private boolean defaultProfile = false;

    /**
     * The predicate used to dynamically select a provider based on its info and context.
     */
    @Getter(AccessLevel.PROTECTED)
    private BiPredicate<LlmProviderInfo, ExecutionContextView> providerFilter;

    /**
     * The function used to dynamically select a profile based on context and available profiles.
     */
    @Getter(AccessLevel.PROTECTED)
    private BiFunction<ExecutionContextView, Set<String>, String> profilePicker;

    /**
     * The function used to dynamically generate the default system message.
     */
    @Getter(AccessLevel.PROTECTED)
    private Function<ExecutionContextView, String> defaultSystemMessageProvider;

    /**
     * Constructs a new {@link DefaultProviderSpec}.
     *
     * @param llmClientType             the type of client
     * @param llmProviderRegistry       the registry for looking up providers
     * @param defaultExecutionContextSpec the preceding execution context specification
     */
    protected DefaultProviderSpec(@NonNull LlmClientType llmClientType, @NonNull LlmProviderRegistry llmProviderRegistry, @NonNull DefaultExecutionContextSpec defaultExecutionContextSpec) {
        this.llmClientType = llmClientType;
        this.llmProviderRegistry = llmProviderRegistry;
        this.defaultExecutionContextSpec = defaultExecutionContextSpec;
    }

    /**
     * Creates a new instance of {@link DefaultProviderSpec}.
     *
     * @param llmClientType             the type of client
     * @param llmProviderRegistry       the registry for looking up providers
     * @param defaultExecutionContextSpec the preceding execution context specification
     * @return a new {@link DefaultProviderSpec} instance
     */
    public static DefaultProviderSpec of(@NonNull LlmClientType llmClientType, @NonNull LlmProviderRegistry llmProviderRegistry, @NonNull DefaultExecutionContextSpec defaultExecutionContextSpec) {
        return new DefaultProviderSpec(llmClientType, llmProviderRegistry, defaultExecutionContextSpec);
    }

    /**
     * Configures the spec to use the default provider.
     * <p>
     * This method overrides any previously set provider filter.
     * </p>
     *
     * @return this instance for method chaining
     */
    @Override
    public ProviderSpec defaultProvider() {
        this.defaultProvider = true;
        return this;
    }

    /**
     * Configures the spec to use a dynamically selected provider.
     * <p>
     * This method disables the default provider flag.
     * </p>
     *
     * @param providerFilter a {@link BiPredicate} to select the provider
     * @return this instance for method chaining
     */
    @Override
    public ProviderSpec firstProvider(@NonNull BiPredicate<LlmProviderInfo, ExecutionContextView> providerFilter) {
        this.defaultProvider = false;
        this.providerFilter = providerFilter;
        return this;
    }

    /**
     * Configures the spec to use the default profile for the selected provider.
     * <p>
     * This method overrides any previously set profile picker.
     * </p>
     *
     * @return this instance for method chaining
     */
    @Override
    public ProviderSpec defaultProfile() {
        this.defaultProfile = true;
        return this;
    }

    /**
     * Configures the spec to use a dynamically selected profile.
     * <p>
     * This method disables the default profile flag.
     * </p>
     *
     * @param profilePicker a {@link BiFunction} to select the profile
     * @return this instance for method chaining
     */
    @Override
    public ProviderSpec profile(@NonNull BiFunction<ExecutionContextView, Set<String>, String> profilePicker) {
        this.defaultProfile = false;
        this.profilePicker = profilePicker;
        return this;
    }

    /**
     * Configures a dynamic default system message.
     *
     * @param defaultSystemMessageProvider a {@link Function} to generate the system message
     * @return this instance for method chaining
     */
    @Override
    public ProviderSpec defaultSystemMessage(@NonNull Function<ExecutionContextView, String> defaultSystemMessageProvider) {
        this.defaultSystemMessageProvider = defaultSystemMessageProvider;
        return this;
    }

    /**
     * Transitions to the chat specification configuration.
     *
     * @return a new {@link DefaultConfigurableChatSpec} instance initialized with the current state
     */
    @Override
    public ConfigurableChatSpec chatSpec() {
        return new DefaultConfigurableChatSpec(this.llmClientType, this.llmProviderRegistry, defaultExecutionContextSpec, this);
    }
}
