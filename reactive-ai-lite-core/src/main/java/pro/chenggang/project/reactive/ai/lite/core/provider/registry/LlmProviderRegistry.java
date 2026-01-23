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

import java.util.function.Predicate;

/**
 * A registry for managing and accessing Large Language Model (LLM) providers.
 * This interface defines methods for retrieving providers based on their capabilities
 * and other criteria.
 *
 * @author Cheng Gang
 * @version 0.1.0
 */
public interface LlmProviderRegistry {

    /**
     * Retrieves the default provider for a specific {@link Capability}.
     *
     * @param capability The capability for which to find the default provider. Must not be null.
     * @return The default {@link LlmProvider}.
     */
    LlmProvider getDefaultProvider(@NonNull Capability capability);

    /**
     * Finds and returns a chat provider that matches the given filter.
     *
     * @param providerFilter A {@link Predicate} to apply to the {@link LlmProviderInfo} of each provider. Must not be null.
     * @return The first {@link LlmChatProvider} that matches the filter.
     */
    LlmChatProvider getChatProvider(@NonNull Predicate<LlmProviderInfo> providerFilter);

}
