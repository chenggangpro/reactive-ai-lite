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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * The default implementation of {@link LlmProviderRegistry}.
 * This class manages a collection of {@link LlmProvider} instances, allowing for the retrieval of
 * default providers by capability and the selection of specific providers based on custom filters.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
@Slf4j
public class DefaultLlmProviderRegistry implements LlmProviderRegistry {

    private final Map<Capability, LlmProvider> defaultProviderContainer = new HashMap<>();
    private final List<LlmProvider> providers;

    public DefaultLlmProviderRegistry(@NonNull List<LlmProvider> providers) {
        if (CollectionUtils.isEmpty(providers)) {
            throw new IllegalArgumentException("At least one LlmProvider is provided.");
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
    }

    @Override
    public LlmProvider getDefaultProvider(@NonNull Capability capability) {
        LlmProvider llmProvider = defaultProviderContainer.get(capability);
        if (Objects.isNull(llmProvider)) {
            throw new IllegalArgumentException("At least one default LlmProvider is required for " + capability + ". Use 'LlmProvider.info().isDefault()' to mark a provider as default.");
        }
        return llmProvider;
    }

    @Override
    public LlmChatProvider getChatProvider(@NonNull Predicate<LlmProviderInfo> providerFilter) {
        return this.providers.stream()
                .filter(llmProvider -> Capability.CHAT.equals(llmProvider.capability()) && providerFilter.test(llmProvider.info()))
                .findFirst()
                .map(LlmChatProvider.class::cast)
                .orElseThrow(() -> new IllegalStateException("No LlmProvider found that matches the given filter"));
    }
}
