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
 * Central registry for discovering and retrieving concrete {@link LlmProvider} implementations.
 * <p>
 * The registry maintains a collection of all configured providers and exposes query methods
 * that allow the framework or client code to obtain the most appropriate provider instance
 * based on its {@link Capability} (e.g., chat, embedding) and optionally through dynamic
 * filtering using provider metadata ({@link LlmProviderInfo}).
 * </p>
 * <p>
 * When multiple providers support the same capability, the default provider is selected
 * according to implementation-specific rules (typically the one marked as default or the
 * first one registered). For more fine-grained selection, the {@link #getProvider(Capability, Class, Predicate)}
 * method evaluates a predicate against each candidate’s metadata, enabling criteria such as
 * model name, cost preferences, or custom tags.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
public interface LlmProviderRegistry {

    /**
     * Returns the default provider for a given capability.
     * <p>
     * The selection process inspects all registered providers that declare support
     * for the requested {@link Capability}. If one of them is explicitly marked as
     * the default, it is returned; otherwise the first matching provider is used.
     * This guarantees a deterministic result even when no explicit default is configured.
     * </p>
     *
     * @param capability the required capability (e.g., {@link Capability#CHAT}, {@link Capability#EMBEDDING})
     * @return a {@link Mono} that emits the default {@link LlmProvider} for the capability
     * @throws IllegalArgumentException if no provider supports the given capability
     */
    Mono<? extends LlmProvider> getDefaultProvider(@NonNull Capability capability);

    /**
     * Retrieves a chat provider that matches the given filter predicate.
     * <p>
     * This method evaluates the predicate against the {@link LlmProviderInfo} of all
     * registered chat providers and returns the first one that satisfies the condition.
     * It is intended for scenarios where the selection must be dynamic, e.g., based on
     * model attributes or runtime properties.
     * </p>
     *
     * @param providerFilter a predicate applied to each chat provider’s metadata
     * @return a {@link Mono} emitting the first matching {@link LlmChatProvider}
     * @deprecated since 0.1.0, use {@link #getProvider(Capability, Class, Predicate)}
     *             with {@code Capability.CHAT} and {@code LlmChatProvider.class} for
     *             a more type-safe and extensible selection mechanism.
     */
    @Deprecated
    Mono<LlmChatProvider> getChatProvider(@NonNull Predicate<LlmProviderInfo> providerFilter);

    /**
     * Retrieves an embedding provider that matches the given filter predicate.
     * <p>
     * Similar to {@link #getChatProvider(Predicate)}, this method looks for a
     * registered {@link pro.chenggang.project.reactive.ai.lite.core.provider.LlmEmbeddingProvider}
     * whose {@link LlmProviderInfo} satisfies the provided predicate. It is useful when the
     * embedding provider must be chosen dynamically based on metadata.
     * </p>
     *
     * @param providerFilter a predicate applied to each embedding provider’s metadata
     * @return a {@link Mono} emitting the first matching embedding provider
     */
    Mono<pro.chenggang.project.reactive.ai.lite.core.provider.LlmEmbeddingProvider> getEmbeddingProvider(@NonNull Predicate<LlmProviderInfo> providerFilter);

    /**
     * Generic method to retrieve a provider of any supported {@link Capability} and concrete type
     * that satisfies a dynamic filter predicate.
     * <p>
     * The registry first narrows the candidate set to those providers that support the given
     * {@code capability} and are assignable to the requested {@code providerClass}. Then the
     * {@code providerFilter} is evaluated against each candidate’s {@link LlmProviderInfo},
     * and the first match is returned. This provides maximum flexibility for selecting a provider
     * based on runtime conditions such as model name, region, or custom properties.
     * </p>
     *
     * @param capability     the capability the desired provider must support
     * @param providerClass  the expected runtime type of the provider
     * @param providerFilter a predicate to evaluate each provider’s metadata
     * @param <P>            the concrete provider type
     * @return a {@link Mono} that emits the first provider matching all criteria
     * @throws IllegalArgumentException if no provider matches the given parameters
     */
    <P extends LlmProvider> Mono<P> getProvider(@NonNull Capability capability, @NonNull Class<P> providerClass, @NonNull Predicate<LlmProviderInfo> providerFilter);

}