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
package pro.chenggang.project.reactive.ai.lite.core.provider.registry;

import lombok.NonNull;
import pro.chenggang.project.reactive.ai.lite.core.option.Capability;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmChatProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import reactor.core.publisher.Mono;

import java.util.function.Predicate;

/**
 * A central registry for managing, discovering, and retrieving configured LLM providers.
 * <p>
 * This interface acts as the entry point for the framework to find the appropriate
 * provider implementation based on the required capability or dynamically evaluated
 * selection criteria.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface LlmProviderRegistry {

    /**
     * Retrieves the default provider associated with a specific capability.
     * <p>
     * If multiple providers support the given capability, this method returns the one
     * explicitly marked as default or, if none are marked, the first available provider.
     * </p>
     *
     * @param capability the {@link Capability} (e.g., CHAT, EMBEDDING) for which to find the provider
     * @return a {@link Mono} emitting the default {@link LlmProvider} supporting the requested capability
     */
    Mono<? extends LlmProvider> getDefaultProvider(@NonNull Capability capability);

    /**
     * Finds and returns an {@link LlmChatProvider} that matches the given dynamic filter.
     * <p>
     * This method evaluates the provided predicate against the {@link LlmProviderInfo} of
     * all registered chat providers. It returns the first provider that satisfies the condition.
     * </p>
     *
     * @param providerFilter a {@link Predicate} used to evaluate each provider's info
     * @return a {@link Mono} emitting the first {@link LlmChatProvider} that matches the filter
     */
    Mono<LlmChatProvider> getChatProvider(@NonNull Predicate<LlmProviderInfo> providerFilter);

}
