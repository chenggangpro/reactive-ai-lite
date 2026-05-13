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
 * This class manages a collection of initialized {@link LlmProvider} instances.
 * Upon instantiation, it categorizes providers by their capability and determines
 * the default provider for each capability based on the {@link LlmProviderInfo#isDefault()} flag.
 * It provides efficient lookup methods to resolve providers at runtime.
 * </p>
 *
 * @author Gang Cheng
 * @version 0.1.0
 */
@Slf4j
public class DefaultLlmProviderRegistry implements LlmProviderRegistry {

    /**
     * A map storing the default provider assigned to each capability.
     */
    private final Map<Capability, LlmProvider> defaultProviderContainer = new HashMap<>();

    /**
     * An immutable list of all successfully registered and validated providers.
     */
    private final List<LlmProvider> providers;

    /**
     * Constructs a new {@link DefaultLlmProviderRegistry}.
     * <p>
     * Iterates through the provided list of LLM providers. Providers with null info
     * are skipped. For valid providers, if their info is marked as default, they are
     * registered as the default for their specific capability (the first one encountered wins).
     * </p>
     *
     * @param providers the list of {@link LlmProvider} instances injected or configured
     * @throws IllegalArgumentException if the provided list is empty or null
     */
    public DefaultLlmProviderRegistry(@NonNull List<LlmProvider> providers) {
        if (CollectionUtils.isEmpty(providers)) {
            throw new IllegalArgumentException("At least one LlmProvider must be provided.");
        }
        List<LlmProvider> validProviders = new ArrayList<>();
        for (LlmProvider llmProvider : providers) {
            LlmProviderInfo llmProviderInfo = llmProvider.info();
            if (Objects.isNull(llmProviderInfo)) {
                continue;
            }
            validProviders.add(llmProvider);
            if (llmProviderInfo.isDefault()) {
                defaultProviderContainer.putIfAbsent(llmProvider.capability(), llmProvider);
            }
        }
        this.providers = List.copyOf(validProviders);
        log.info("Initialized {} LlmProvider instances.", validProviders.size());
    }

    /**
     * Retrieves the default provider associated with the specified capability.
     *
     * @param capability the {@link Capability} required
     * @return a {@link Mono} emitting the default {@link LlmProvider} for that capability
     */
    @Override
    public Mono<LlmProvider> getDefaultProvider(@NonNull Capability capability) {
        return Mono.justOrEmpty(defaultProviderContainer.get(capability))
                .switchIfEmpty(Mono.error(() -> new IllegalArgumentException("At least one default LlmProvider is required for " + capability + ". Use 'LlmProvider.info().isDefault()' to mark a provider as default.")));
    }

    /**
     * Finds and returns a chat provider that matches the given predicate filter.
     * <p>
     * This iterates through all registered providers, filtering only for those with
     * the {@link Capability#CHAT} capability, and tests them against the provided filter.
     * The first provider to match is returned.
     * </p>
     *
     * @param providerFilter the predicate used to evaluate each provider's metadata
     * @return a {@link Mono} emitting the matched {@link LlmChatProvider}
     */
    @Override
    public Mono<LlmChatProvider> getChatProvider(@NonNull Predicate<LlmProviderInfo> providerFilter) {
        return Flux.fromIterable(this.providers)
                .filter(llmProvider -> Capability.CHAT.equals(llmProvider.capability()) && providerFilter.test(llmProvider.info()))
                .next()
                .cast(LlmChatProvider.class)
                .switchIfEmpty(Mono.error(() -> new IllegalStateException("No LlmChatProvider found that matches the given filter.")));
    }
}
