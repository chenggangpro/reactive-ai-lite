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
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;


/**
 * A specification for selecting and configuring an AI provider and its associated profile.
 * <p>
 * This interface is part of a fluent API that allows for both static and dynamic
 * selection of providers and their settings based on the execution context.
 * It provides methods to filter providers, choose specific profiles, and set default
 * system messages before moving on to configure the chat request itself.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface ProviderSpec {

    /**
     * Selects the default AI provider configured in the system.
     * <p>
     * This is typically the first provider registered or one explicitly marked as default.
     * </p>
     *
     * @return this {@link ProviderSpec} instance for method chaining
     */
    ProviderSpec defaultProvider();

    /**
     * Selects the first available AI provider that matches the given predicate.
     * <p>
     * The predicate evaluates both the provider's information and the current execution
     * context, allowing for dynamic provider selection based on runtime conditions.
     * </p>
     *
     * @param providerFilter a {@link BiPredicate} to test each provider against the execution context
     * @return this {@link ProviderSpec} instance for method chaining
     */
    ProviderSpec firstProvider(@NonNull BiPredicate<LlmProviderInfo, ExecutionContext> providerFilter);

    /**
     * Selects the first available AI provider that matches the given predicate.
     * <p>
     * This is a convenience method for when the execution context is not needed
     * to make the selection decision.
     * </p>
     *
     * @param providerFilter a {@link Predicate} to test each provider's info
     * @return this {@link ProviderSpec} instance for method chaining
     */
    default ProviderSpec firstProvider(@NonNull Predicate<LlmProviderInfo> providerFilter) {
        return firstProvider((info, contextView) -> providerFilter.test(info));
    }

    /**
     * Selects the default profile for the currently chosen AI provider.
     * <p>
     * The default profile is usually predefined within the provider's configuration.
     * </p>
     *
     * @return this {@link ProviderSpec} instance for method chaining
     */
    ProviderSpec defaultProfile();

    /**
     * Dynamically selects a profile for the AI provider using a picker function.
     * <p>
     * The provided function receives the current execution context and the set of available
     * profile names for the selected provider, and must return the name of the profile to use.
     * </p>
     *
     * @param profilePicker a {@link BiFunction} that determines which profile to use
     * @return this {@link ProviderSpec} instance for method chaining
     */
    ProviderSpec profile(@NonNull BiFunction<ExecutionContext, Set<String>, String> profilePicker);

    /**
     * Selects a specific profile by its static name.
     * <p>
     * This is a convenience method for when the profile name is known upfront
     * and does not depend on the execution context.
     * </p>
     *
     * @param profile the name of the profile to use
     * @return this {@link ProviderSpec} instance for method chaining
     */
    default ProviderSpec profile(@NonNull String profile) {
        return profile((exchange, allProfiles) -> profile);
    }

    /**
     * Sets a default system message for the chat conversation dynamically.
     * <p>
     * The message is generated based on the execution context when the chat
     * session begins. This message is typically used to set the behavior or
     * persona of the AI.
     * </p>
     *
     * @param defaultSystemMessageProvider a {@link Function} that generates the system message string
     * @return this {@link ProviderSpec} instance for method chaining
     */
    ProviderSpec defaultSystemMessage(@NonNull Function<ExecutionContext, String> defaultSystemMessageProvider);

    /**
     * Sets a static default system message for the chat conversation.
     * <p>
     * This is a convenience method for setting a fixed system message that
     * does not change based on the context.
     * </p>
     *
     * @param systemMessage the content of the system message
     * @return this {@link ProviderSpec} instance for method chaining
     */
    default ProviderSpec defaultSystemMessage(@NonNull String systemMessage) {
        return defaultSystemMessage(contextView -> systemMessage);
    }

    /**
     * Transitions from provider configuration to chat request configuration.
     * <p>
     * This method returns the next specification in the fluent API chain,
     * allowing for the configuration of messages, tools, and options for the
     * actual chat request.
     * </p>
     *
     * @return a {@link ConfigurableChatSpec} instance for further configuration
     */
    ConfigurableChatSpec chatSpec();

}
