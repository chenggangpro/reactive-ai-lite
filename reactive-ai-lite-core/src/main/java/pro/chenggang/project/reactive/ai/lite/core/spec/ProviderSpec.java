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
package pro.chenggang.project.reactive.ai.lite.core.spec;

import lombok.NonNull;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContextView;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.TextMessage;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;


/**
 * A specification for selecting and configuring an AI provider and its associated profile.
 * This interface is part of a fluent API that allows for both static and dynamic
 * selection of providers and their settings based on the execution context.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface ProviderSpec {

    /**
     * Selects the default AI provider configured in the system.
     *
     * @return This {@link ProviderSpec} instance for method chaining.
     */
    ProviderSpec defaultProvider();

    /**
     * Selects the first available AI provider that matches the given predicate.
     * The predicate can use both the provider's information and the current execution
     * context to make a dynamic decision.
     *
     * @param providerFilter A {@link BiPredicate} to test each provider against the execution context.
     * @return This {@link ProviderSpec} instance for method chaining.
     */
    ProviderSpec firstProvider(@NonNull BiPredicate<LlmProviderInfo, ExecutionContextView> providerFilter);

    /**
     * A convenience method to select the first available AI provider that matches
     * the given predicate, without considering the execution context.
     *
     * @param providerFilter A {@link Predicate} to test each provider.
     * @return This {@link ProviderSpec} instance for method chaining.
     */
    default ProviderSpec firstProvider(@NonNull Predicate<LlmProviderInfo> providerFilter) {
        return firstProvider((info, contextView) -> providerFilter.test(info));
    }

    /**
     * Selects the default profile for the chosen AI provider.
     *
     * @return This {@link ProviderSpec} instance for method chaining.
     */
    ProviderSpec defaultProfile();

    /**
     * Dynamically selects a profile for the AI provider using a picker function.
     * The function receives the current execution context and the set of available
     * profile names, and must return the name of the profile to use.
     *
     * @param profilePicker A {@link BiFunction} that determines which profile to use.
     * @return This {@link ProviderSpec} instance for method chaining.
     */
    ProviderSpec profile(@NonNull BiFunction<ExecutionContextView, Set<String>, String> profilePicker);

    /**
     * Selects a profile by its static name.
     *
     * @param profile The name of the profile to use.
     * @return This {@link ProviderSpec} instance for method chaining.
     */
    default ProviderSpec profile(@NonNull String profile) {
        return profile((exchange, allProfiles) -> profile);
    }

    /**
     * Sets a default system message for the chat conversation, generated dynamically
     * based on the execution context.
     *
     * @param defaultSystemMessageProvider A {@link Function} that returns a {@link TextMessage}.
     * @return This {@link ProviderSpec} instance for method chaining.
     */
    ProviderSpec defaultSystemMessage(@NonNull Function<ExecutionContextView, TextMessage> defaultSystemMessageProvider);

    /**
     * Sets a static default system message for the chat conversation.
     *
     * @param systemMessage The content of the system message.
     * @return This {@link ProviderSpec} instance for method chaining.
     */
    default ProviderSpec defaultSystemMessage(@NonNull String systemMessage) {
        return defaultSystemMessage(contextView -> TextMessage.of(systemMessage));
    }

    /**
     * Returns the next specification in the fluent API chain for configuring
     * the chat request itself.
     *
     * @return A {@link ConfigurableChatSpec} instance.
     */
    ConfigurableChatSpec chatSpec();

}
