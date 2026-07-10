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

import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.option.Capability;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmChatProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmEmbeddingProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProviderRegistryPackageTest {

    @Test
    void testDefaultLlmProviderRegistry() {
        // Empty
        assertThatIllegalArgumentException().isThrownBy(() -> new DefaultLlmProviderRegistry(List.of()));

        LlmChatProvider chatProvider1 = mock(LlmChatProvider.class);
        when(chatProvider1.capability()).thenReturn(Capability.CHAT);
        LlmProviderInfo info1 = mock(LlmProviderInfo.class);
        when(info1.name()).thenReturn("chat1");
        when(info1.isDefault()).thenReturn(true);
        when(chatProvider1.info()).thenReturn(info1);

        LlmChatProvider chatProvider2 = mock(LlmChatProvider.class);
        when(chatProvider2.capability()).thenReturn(Capability.CHAT);
        LlmProviderInfo info2 = mock(LlmProviderInfo.class);
        when(info2.name()).thenReturn("chat2");
        when(info2.isDefault()).thenReturn(true);
        when(chatProvider2.info()).thenReturn(info2);

        LlmEmbeddingProvider embedProvider = mock(LlmEmbeddingProvider.class);
        when(embedProvider.capability()).thenReturn(Capability.EMBEDDING);
        LlmProviderInfo info3 = mock(LlmProviderInfo.class);
        when(info3.name()).thenReturn("embed");
        when(info3.isDefault()).thenReturn(false);
        when(embedProvider.info()).thenReturn(info3);

        LlmProvider noInfoProvider = mock(LlmProvider.class);
        when(noInfoProvider.info()).thenReturn(null);

        DefaultLlmProviderRegistry registry = new DefaultLlmProviderRegistry(List.of(chatProvider1, chatProvider2, embedProvider, noInfoProvider));

        StepVerifier.create(registry.getDefaultProvider(Capability.CHAT))
                .expectNextMatches(p -> p == chatProvider1)
                .verifyComplete();

        StepVerifier.create(registry.getDefaultProvider(Capability.EMBEDDING))
                .verifyError(IllegalArgumentException.class);

        StepVerifier.create(registry.getChatProvider(info -> info.name().equals("chat2")))
                .expectNext(chatProvider2)
                .verifyComplete();

        StepVerifier.create(registry.getChatProvider(info -> info.name().equals("none")))
                .verifyError(IllegalStateException.class);

        StepVerifier.create(registry.getEmbeddingProvider(info -> info.name().equals("embed")))
                .expectNext(embedProvider)
                .verifyComplete();

        StepVerifier.create(registry.getEmbeddingProvider(info -> info.name().equals("none")))
                .verifyError(IllegalStateException.class);

        StepVerifier.create(registry.getProvider(Capability.CHAT, LlmChatProvider.class, info -> info.name().equals("chat1")))
                .expectNext(chatProvider1)
                .verifyComplete();

        StepVerifier.create(registry.getProvider(Capability.CHAT, LlmChatProvider.class, info -> info.name().equals("none")))
                .verifyError(IllegalStateException.class);
    }
}
