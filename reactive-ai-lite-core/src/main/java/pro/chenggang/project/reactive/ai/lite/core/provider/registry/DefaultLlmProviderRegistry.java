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
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import pro.chenggang.project.reactive.ai.lite.core.option.Capability;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmChatProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmEmbeddingProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * The standard, default implementation of the {@link LlmProviderRegistry}.
 * <p>
 * This registry consolidates all discovered or configured {@link LlmProvider} instances.
 * During instantiation it performs an initial validation: any provider with a <code>null</code>
 * {@link LlmProviderInfo} is discarded. The remaining providers are stored immutably.
 * Additionally, the registry identifies the designated default provider for each
 * {@link Capability}—the first provider whose {@link LlmProviderInfo#isDefault()} returns
 * <code>true</code> is chosen, and any further defaults for the same capability are ignored
 * with a warning log.
 * <p>
 * Lookup methods rely on Reactor reactive types to support integration in reactive pipelines.
 * The class is inherently thread-safe because its internal state is fully initialized in
 * the constructor and never modified afterwards.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Slf4j
public class DefaultLlmProviderRegistry implements LlmProviderRegistry {

    /**
     * Immutable map of {@link Capability} → default provider.
     * <p>
     * This map is populated during construction and never changes thereafter.
     * Each entry holds the single provider that is marked as default for its capability.
     * If a capability has no default provider, no entry is present.
     * </p>
     */
    private final Map<Capability, LlmProvider> defaultProviderContainer;

    /**
     * Immutable, ordered list of <em>all</em> valid providers that were accepted after
     * filtering out those with a <code>null</code> {@link LlmProviderInfo}.
     * <p>
     * This list is used by the search-based lookup methods to apply additional
     * {@link Predicate} filters. Its immutability guarantees safe concurrent access.
     * </p>
     */
    private final List<LlmProvider> providers;

    /**
     * Constructs a new {@link DefaultLlmProviderRegistry} from a list of candidate providers.
     * <p>
     * The constructor:
     * <ol>
     *   <li>Rejects an empty or null input list with an {@link IllegalArgumentException}.</li>
     *   <li>Iterates over the providers, skipping any whose {@link LlmProvider#info()} returns
     *       <code>null</code>.</li>
     *   <li>Accumulates valid providers in a temporary list.</li>
     *   <li>If a provider’s metadata declares it as default (via
     *       {@link LlmProviderInfo#isDefault()}), the registry attempts to record it as
     *       the default for its capability. The first such provider wins; any subsequent
     *       default-marked providers for the same capability are ignored, and a warning is
     *       logged.</li>
     *   <li>Finally, the resulting collections are made immutable via
     *       {@link Map#copyOf(Map)} and {@link List#copyOf(java.util.Collection)}.</li>
     * </ol>
     * This strict initialization ensures that the registry’s state is consistent and
     * fully prepared before any lookup occurs.
     * </p>
     *
     * @param providers the list of {@link LlmProvider} instances provided by the
     *                  application context; must not be null or empty
     * @throws IllegalArgumentException if the input list is {@code null} or empty
     */
    public DefaultLlmProviderRegistry(@NonNull List<LlmProvider> providers) {
        if (CollectionUtils.isEmpty(providers)) {
            throw new IllegalArgumentException("At least one LlmProvider must be provided.");
        }
        List<LlmProvider> validProviders = new ArrayList<>();
        Map<Capability, LlmProvider> tempDefaultProviderContainer = new HashMap<>();
        for (LlmProvider llmProvider : providers) {
            LlmProviderInfo llmProviderInfo = llmProvider.info();
            if (Objects.isNull(llmProviderInfo)) {
                continue;
            }
            validProviders.add(llmProvider);
            if (llmProviderInfo.isDefault()) {
                LlmProvider existing = tempDefaultProviderContainer.putIfAbsent(llmProvider.capability(), llmProvider);
                if (existing != null) {
                    log.warn("Multiple default providers detected for capability {}. Keeping {} and ignoring {}.",
                            llmProvider.capability(), existing, llmProvider
                    );
                }
            }
        }
        this.defaultProviderContainer = Map.copyOf(tempDefaultProviderContainer);
        this.providers = List.copyOf(validProviders);
        log.info("Initialized {} LlmProvider instances.", validProviders.size());
    }

    /**
     * Obtains the default provider for the specified capability.
     * <p>
     * The lookup is a direct map access from the immutable default container.
     * The returned {@link Mono} emits the provider or, if no default is registered,
     * completes with an {@link IllegalArgumentException} that explains the requirement.
     * The provider is then cast to the subclass appropriate for the capability
     * (for example {@link LlmChatProvider} for {@link Capability#CHAT}) using
     * {@link Capability#getProviderClass()}, ensuring type safety downstream.
     * </p>
     *
     * @param capability the {@link Capability} required; must not be null
     * @return a {@link Mono} emitting the default {@link LlmProvider} for that capability;
     *         an error signal if none is configured
     */
    @Override
    public Mono<? extends LlmProvider> getDefaultProvider(@NonNull Capability capability) {
        return Mono.justOrEmpty(defaultProviderContainer.get(capability))
                .ofType(capability.getProviderClass())
                .switchIfEmpty(Mono.error(() -> new IllegalArgumentException("At least one default LlmProvider is required for " + capability + ". Use 'LlmProvider.info().isDefault()' to mark a provider as default.")));
    }

    /**
     * Searches for a chat provider whose metadata matches the given filter.
     * <p>
     * The registry filters all stored providers down to those with {@link Capability#CHAT},
     * then applies the provided predicate to the {@link LlmProviderInfo} of each candidate.
     * The first matching provider is returned; if none matches, the resulting
     * {@link Mono} completes with an {@link IllegalStateException}.
     * This reactive, on-demand filtering allows runtime selection of providers based
     * on dynamic criteria such as model name or vendor.
     * </p>
     *
     * @param providerFilter a predicate to test each chat provider’s metadata; must not be null
     * @return a {@link Mono} emitting the matched {@link LlmChatProvider}, or an error if no match
     */
    @Override
    public Mono<LlmChatProvider> getChatProvider(@NonNull Predicate<LlmProviderInfo> providerFilter) {
        return Flux.fromIterable(this.providers)
                .filter(llmProvider -> Capability.CHAT.equals(llmProvider.capability()) && providerFilter.test(llmProvider.info()))
                .next()
                .cast(LlmChatProvider.class)
                .switchIfEmpty(Mono.error(() -> new IllegalStateException("No LlmChatProvider found that matches the given filter.")));
    }

    /**
     * Searches for an embedding provider whose metadata matches the given filter.
     * <p>
     * Works analogously to {@link #getChatProvider(Predicate)} but for
     * {@link Capability#EMBEDDING}. The method first narrows the provider list to
     * embedding-capable ones and then applies the custom filter. The first match
     * is cast to {@link LlmEmbeddingProvider}. If no provider meets the criteria,
     * an {@link IllegalStateException} is signaled.
     * </p>
     *
     * @param providerFilter a predicate to test each embedding provider’s metadata; must not be null
     * @return a {@link Mono} emitting the matched {@link LlmEmbeddingProvider}, or an error if none found
     */
    @Override
    public Mono<LlmEmbeddingProvider> getEmbeddingProvider(@NonNull Predicate<LlmProviderInfo> providerFilter) {
        return Flux.fromIterable(this.providers)
                .filter(llmProvider -> Capability.EMBEDDING.equals(llmProvider.capability()) && providerFilter.test(llmProvider.info()))
                .next()
                .cast(LlmEmbeddingProvider.class)
                .switchIfEmpty(Mono.error(() -> new IllegalStateException("No LlmEmbeddingProvider found that matches the given filter.")));
    }

    /**
     * Generic lookup method for a provider that satisfies a specific capability,
     * an expected subtype, and an arbitrary filter.
     * <p>
     * This method pairs the capability restriction with a client-supplied predicate
     * that can examine the provider’s {@link LlmProviderInfo}. The first provider
     * matching both criteria is returned after being cast to the requested
     * {@code providerClass}. If no provider satisfies the conditions, an
     * {@link IllegalStateException} is signaled. The approach supports extensibility
     * for future capabilities without altering the registry API.
     * </p>
     *
     * @param capability     the desired {@link Capability}; must not be null
     * @param providerClass  the concrete class of the expected provider; used for
     *                       runtime type casting
     * @param providerFilter a predicate to test the provider’s metadata; must not be null
     * @param <P>            the concrete type of the provider
     * @return a {@link Mono} emitting the matched provider, or an error if no match exists
     */
    @Override
    public <P extends LlmProvider> Mono<P> getProvider(@NonNull Capability capability, @NonNull Class<P> providerClass, @NonNull Predicate<LlmProviderInfo> providerFilter) {
        return Flux.fromIterable(this.providers)
                .filter(llmProvider -> capability.equals(llmProvider.capability()) && providerFilter.test(llmProvider.info()))
                .next()
                .cast(providerClass)
                .switchIfEmpty(Mono.error(() -> new IllegalStateException("No provider found that matches the given capability and filter.")));
    }
}