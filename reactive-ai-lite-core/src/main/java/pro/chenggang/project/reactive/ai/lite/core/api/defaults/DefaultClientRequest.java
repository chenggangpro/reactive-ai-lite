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
package pro.chenggang.project.reactive.ai.lite.core.api.defaults;

import lombok.NonNull;
import pro.chenggang.project.reactive.ai.lite.core.api.ClientRequest;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import pro.chenggang.project.reactive.ai.lite.core.spec.ConfigurableChatSpec;
import pro.chenggang.project.reactive.ai.lite.core.spec.ConfigurableEmbeddingSpec;
import pro.chenggang.project.reactive.ai.lite.core.spec.ConfigurableSpeechSpec;
import pro.chenggang.project.reactive.ai.lite.core.spec.defaults.DefaultConfigurableChatSpec;
import pro.chenggang.project.reactive.ai.lite.core.spec.defaults.DefaultConfigurableEmbeddingSpec;
import pro.chenggang.project.reactive.ai.lite.core.spec.defaults.DefaultConfigurableSpeechSpec;
import pro.chenggang.project.reactive.ai.lite.core.spec.defaults.ProviderConfigureInfo;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/**
 * The default implementation of the {@link ClientRequest} interface.
 * <p>
 * This class serves as the central configuration collector before a specific capability
 * (chat or embedding) is requested. It aggregates settings such as parent attributes,
 * context merging strategy, provider selection logic, and profile resolution.
 * <p>
 * The design separates configuration from actual client execution, allowing a fluent
 * API that captures user intent step by step. Once all settings are provided, the
 * {@code chat()} or {@code embedding()} methods finalize the configuration and produce
 * the appropriate configurable spec.
 * <p>
 * Provider and profile selection are based on lazy-evaluated predicates and functions.
 * This enables dynamic decision-making that depends on the current {@link ExecutionContext}
 * and available LLM profiles.
 * <p>
 * The collected settings are packaged into a {@link ProviderConfigureInfo} object,
 * which is passed to the spec constructors. This clean separation ensures that the
 * configuration is immutable once built, promoting thread safety and predictability.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @see ClientRequest
 * @see ProviderConfigureInfo
 */
public class DefaultClientRequest implements ClientRequest {

    /**
     * Registry holding all available LLM providers. Used later to resolve provider
     * details based on the selected criteria.
     */
    private final LlmProviderRegistry llmProviderRegistry;

    /**
     * Optional parent attributes that will be merged into the execution context
     * when building the final request. Allows callers to attach metadata that
     * should propagate across multiple chained calls.
     */
    private Map<String, Object> parentAttributes;

    /**
     * Custom context merging logic. If set, it overrides the default merging strategy
     * when combining the parent attributes with the current execution context.
     * This enables fine-grained control over how context data is blended.
     */
    private ContextMerger contextConfigure;

    /**
     * Flag indicating whether to use the default provider selection algorithm.
     * When {@code true}, any previously set {@link #providerFilter} is ignored.
     */
    private boolean defaultProvider = false;

    /**
     * Flag indicating whether to use the default profile selection algorithm.
     * When {@code true}, any previously set {@link #profilePicker} is ignored.
     */
    private boolean defaultProfile = false;

    /**
     * Custom predicate for filtering LLM providers. Evaluated at spec construction
     * time, with the current execution context and provider info. If not set,
     * all registered providers are considered.
     */
    private BiPredicate<ExecutionContext, LlmProviderInfo> providerFilter;

    /**
     * Custom function for picking a profile from a set. Evaluated at spec construction
     * time, with the current execution context and the available profile names.
     * If not set, a default picker (usually the first profile) is used.
     */
    private BiFunction<ExecutionContext, Set<String>, String> profilePicker;

    /**
     * Constructs a new {@code DefaultClientRequest} with the mandatory registry.
     *
     * @param llmProviderRegistry The registry that knows all LLM providers; cannot be null.
     */
    public DefaultClientRequest(LlmProviderRegistry llmProviderRegistry) {
        this.llmProviderRegistry = llmProviderRegistry;
    }

    /**
     * Sets optional parent attributes to be merged into the execution context later.
     * These are key-value pairs that may carry authentication tokens, user metadata,
     * or any information relevant to the downstream LLM calls.
     *
     * @param parentAttributes A map of attribute names to values; cannot be null.
     * @return This request instance for method chaining.
     */
    @Override
    public ClientRequest parentAttributes(@NonNull Map<String, Object> parentAttributes) {
        this.parentAttributes = parentAttributes;
        return this;
    }

    /**
     * Configures a custom context merging strategy.
     * <p>
     * By default, parent attributes are shallow-merged into the execution context.
     * This method allows callers to define their own merging logic, for example
     * deep merging, or handling conflicting keys in a specific way.
     *
     * @param contextConfigure A function that accepts the parent attributes and the
     *                         current execution context and returns the merged context;
     *                         cannot be null.
     * @return This request instance for method chaining.
     */
    @Override
    public ClientRequest context(@NonNull ContextMerger contextConfigure) {
        this.contextConfigure = contextConfigure;
        return this;
    }

    /**
     * Marks that the default provider selection logic should be used.
     * <p>
     * When this method is called, any previously set {@code providerFilter} is
     * discarded. The actual default logic (e.g., first registered provider) is
     * applied when the spec is created.
     *
     * @return This request instance for method chaining.
     */
    @Override
    public ClientRequest defaultProvider() {
        this.defaultProvider = true;
        return this;
    }

    /**
     * Provides a custom filter for selecting an LLM provider.
     * <p>
     * The predicate receives the current execution context and a candidate
     * {@link LlmProviderInfo}. It should return {@code true} for the desired provider.
     * Calling this method disables the default provider logic.
     *
     * @param providerFilter A predicate evaluated later against each registered provider;
     *                       cannot be null.
     * @return This request instance for method chaining.
     */
    @Override
    public ClientRequest provider(@NonNull BiPredicate<ExecutionContext, LlmProviderInfo> providerFilter) {
        this.defaultProvider = false;
        this.providerFilter = providerFilter;
        return this;
    }

    /**
     * Marks that the default profile selection logic should be used.
     * <p>
     * When this method is called, any previously set {@code profilePicker} is
     * discarded. The actual default logic (e.g., first available profile) is
     * applied when the spec is created.
     *
     * @return This request instance for method chaining.
     */
    @Override
    public ClientRequest defaultProfile() {
        this.defaultProfile = true;
        return this;
    }

    /**
     * Provides a custom function to pick a profile from the available set.
     * <p>
     * The function receives the current execution context and a set of profile names
     * (as defined by the selected provider). It must return the chosen profile name.
     * Calling this method disables the default profile logic.
     *
     * @param profilePicker A function that selects a profile among the available ones;
     *                      cannot be null.
     * @return This request instance for method chaining.
     */
    @Override
    public ClientRequest profile(@NonNull BiFunction<ExecutionContext, Set<String>, String> profilePicker) {
        this.defaultProfile = false;
        this.profilePicker = profilePicker;
        return this;
    }

    /**
     * Aggregates all current configuration into an immutable {@link ProviderConfigureInfo}
     * object. This method is called internally when transitioning to a specific spec
     * (chat or embedding) to seal the configuration.
     *
     * @return A new configuration info holding a snapshot of the current settings.
     */
    private ProviderConfigureInfo getConfigureInfo() {
        return ProviderConfigureInfo.builder()
                .defaultProvider(this.defaultProvider)
                .defaultProfile(this.defaultProfile)
                .profilePicker(this.profilePicker)
                .providerFilter(this.providerFilter)
                .parentAttributes(this.parentAttributes)
                .contextConfigure(this.contextConfigure)
                .build();
    }

    /**
     * Finalizes the configuration and returns a {@link ConfigurableChatSpec} that
     * can be further customized for chat operations.
     * <p>
     * The returned spec carries all the settings gathered so far (provider, profile,
     * context merging, etc.) packaged inside an {@link ProviderConfigureInfo}.
     *
     * @return A new configurable chat spec instance, never null.
     */
    @Override
    public ConfigurableChatSpec chat() {
        return new DefaultConfigurableChatSpec(LlmClientType.CHAT, this.llmProviderRegistry, this.getConfigureInfo());
    }

    /**
     * Finalizes the configuration and returns a {@link ConfigurableEmbeddingSpec} that
     * can be further customized for embedding operations.
     * <p>
     * The returned spec carries all the settings gathered so far (provider, profile,
     * context merging, etc.) packaged inside an {@link ProviderConfigureInfo}.
     *
     * @return A new configurable embedding spec instance, never null.
     */
    @Override
    public ConfigurableEmbeddingSpec embedding() {
        return new DefaultConfigurableEmbeddingSpec(LlmClientType.EMBEDDING, this.llmProviderRegistry, this.getConfigureInfo());
    }

    /**
     * Finalizes the configuration and returns a {@link ConfigurableSpeechSpec} that
     * can be further customized for speech operations.
     * <p>
     * The returned spec carries all the settings gathered so far (provider, profile,
     * context merging, etc.) packaged inside an {@link ProviderConfigureInfo}.
     *
     * @return A new configurable speech spec instance, never null.
     */
    @Override
    public ConfigurableSpeechSpec speech() {
        return new DefaultConfigurableSpeechSpec(LlmClientType.SPEECH, this.llmProviderRegistry, this.getConfigureInfo());
    }

}