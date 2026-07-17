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
package pro.chenggang.project.reactive.ai.lite.core.api;

import lombok.NonNull;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import pro.chenggang.project.reactive.ai.lite.core.spec.ConfigurableChatSpec;
import pro.chenggang.project.reactive.ai.lite.core.spec.ConfigurableEmbeddingSpec;
import pro.chenggang.project.reactive.ai.lite.core.spec.ConfigurableSpeechSpec;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/**
 * The top-level interface for constructing a request to a Large Language Model (LLM).
 * <p>
 * This specification serves as the entry point for building a pipeline that carries shared metadata
 * such as parent attributes, execution context customizations, provider selection, and profile
 * configuration. After these common settings are determined, the builder branches into
 * capability-specific sub-specifications (like {@link ConfigurableChatSpec} for chat or
 * {@link ConfigurableEmbeddingSpec} for embeddings) using the {@link #chat()} and
 * {@link #embedding()} methods. The fluent API design allows chaining of configuration
 * steps before executing the final operation.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface ClientRequest {

    /**
     * Functional interface to merge parent attributes and custom configuration logic into an
     * {@link ExecutionContext}. Implementations define how attributes from a parent context (if any)
     * are combined with or transformed for the current request's execution context. The default
     * implementation {@link #APPEND_ALL} simply copies all non-empty parent attributes, but custom
     * mergers can perform selective copying, transformation, or computed additions.
     */
    @FunctionalInterface
    interface ContextMerger {

        /**
         * A predefined {@link ContextMerger} that copies all non-null parent attributes directly
         * into the execution context's attribute map. This is the simplest merging strategy and is
         * suitable when the parent context provides all needed data without modification.
         */
        ContextMerger APPEND_ALL = (executionContext, parentAttributes) -> {
            if (Objects.nonNull(parentAttributes) && !parentAttributes.isEmpty()) {
                executionContext.getAttributes().putAll(parentAttributes);
            }
        };

        /**
         * Merges parent attributes and any additional custom configuration into the provided
         * execution context. Called during request building after the base execution context is
         * created from parent attributes, allowing implementations to overwrite or extend
         * attributes, set special flags, or trigger other side effects.
         *
         * @param executionContext the active execution context to populate
         * @param parentAttributes the attributes from the parent context, may be {@code null}
         */
        void merge(ExecutionContext executionContext, Map<String, Object> parentAttributes);
    }

    /**
     * Sets the static map of parent attributes that will be used to initialize the
     * {@link ExecutionContext} for this request. These attributes typically originate from a
     * higher-level context (e.g., a conversation or a previous request) and can be later modified
     * or extended using {@link #context(ContextMerger)}. Using this method overrides any
     * previously set parent attributes.
     *
     * @param parentAttributes a non-null map of key-value pairs representing inherited data
     * @return this instance for fluent configuration
     */
    ClientRequest parentAttributes(@NonNull Map<String, Object> parentAttributes);

    /**
     * Provides a custom {@link ContextMerger} to manipulate the execution context after it has
     * been populated with the parent attributes. This allows advanced users to programmatically
     * adjust the context based on runtime conditions, such as adding dynamic parameters or
     * sanitizing input. If not called, no custom merging occurs (only parent attributes, if any,
     * are inserted). Typically used in combination with {@link #parentAttributes(Map)} to fully
     * control context initialization.
     *
     * @param contextConfigure a non-null merger implementation
     * @return this instance for fluent configuration
     */
    ClientRequest context(@NonNull ContextMerger contextConfigure);

    /**
     * Instructs the request to use the default provider for the chosen capability. The default
     * provider is typically configured in the application settings and represents a fallback or
     * globally preferred LLM provider. This call is optional; if neither this nor any
     * {@link #provider(String)} overload is used, the framework may apply its own default
     * selection logic.
     *
     * @return this instance for fluent configuration
     */
    ClientRequest defaultProvider();

    /**
     * Specifies a dynamic filter to select a provider based on the runtime {@link ExecutionContext}
     * and the information of each available {@link LlmProviderInfo}. When multiple candidates
     * match, the first one is chosen (implementation-specific order). This method is useful when
     * the appropriate provider depends on request content, user preferences, or other runtime
     * criteria.
     *
     * @param providerFilter a non-null predicate that receives the execution context and a
     *                       provider info; should return {@code true} if the provider is acceptable
     * @return this instance for fluent configuration
     */
    ClientRequest provider(@NonNull BiPredicate<ExecutionContext, LlmProviderInfo> providerFilter);

    /**
     * Convenience method to select a provider by its exact name. Equivalent to calling
     * {@code provider((ctx, info) -> providerName.equals(info.name()))}. Useful for static,
     * pre-known provider configurations.
     *
     * @param providerName the exact name of the desired provider (case-sensitive)
     * @return this instance for fluent configuration
     */
    default ClientRequest provider(@NonNull String providerName) {
        return this.provider((executionContext, llmProviderInfo) -> providerName.equals(llmProviderInfo.name()));
    }

    /**
     * Instructs the request to use the default profile associated with the chosen provider.
     * A profile defines a set of model parameters (e.g., temperature, top-p) and behavioral
     * settings. If not called, a provider-specific default may still be applied, but this makes
     * the intent explicit.
     *
     * @return this instance for fluent configuration
     */
    ClientRequest defaultProfile();

    /**
     * Defines a custom strategy to pick a profile from the available set based on the execution
     * context. The function receives the current execution context and the set of profile names
     * supported by the selected provider, and must return one name. This enables dynamic profile
     * selection, for example, using a different profile depending on the user's role or the task
     * type.
     *
     * @param profilePicker a non-null function that chooses a profile name given context and
     *                      available profiles
     * @return this instance for fluent configuration
     */
    ClientRequest profile(@NonNull BiFunction<ExecutionContext, Set<String>, String> profilePicker);

    /**
     * Convenience method to set a static profile name. Equivalent to calling
     * {@code profile((ctx, profiles) -> profile)}.
     *
     * @param profile the exact name of the desired profile
     * @return this instance for fluent configuration
     */
    default ClientRequest profile(@NonNull String profile) {
        return this.profile((executionContext, profiles) -> profile);
    }

    /**
     * Finalizes the common configuration and branches into chat-specific settings. The returned
     * {@link ConfigurableChatSpec} allows further customization of chat parameters such as
     * messages, prompts, and tool usage. After configuration, the spec can be executed to
     * generate chat responses.
     *
     * @return a new {@link ConfigurableChatSpec} instance with the shared settings applied
     */
    ConfigurableChatSpec chat();

    /**
     * Finalizes the common configuration and branches into embedding-specific settings. The
     * returned {@link ConfigurableEmbeddingSpec} allows further customization of embedding
     * parameters such as input text and model-specific options. After configuration, the spec can
     * be executed to obtain vector embeddings.
     *
     * @return a new {@link ConfigurableEmbeddingSpec} instance with the shared settings applied
     */
    ConfigurableEmbeddingSpec embedding();

    /**
     * Finalizes the common configuration and branches into speech-specific settings. The
     * returned {@link ConfigurableSpeechSpec} allows further customization of speech
     * parameters such as input text, voice, speed and response format. After configuration, the spec can
     * be executed to generate synthesized speech audio.
     *
     * @return a new {@link ConfigurableSpeechSpec} instance with the shared settings applied
     */
    ConfigurableSpeechSpec speech();

}