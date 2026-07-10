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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultLlmProviderRegistryTest {

    @Test
    void testEmptyProviders() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DefaultLlmProviderRegistry(List.of()))
                .withMessageContaining("At least one LlmProvider must be provided.");
    }

    @Test
    void testNullProviderInfo() {
        LlmProvider provider = mock(LlmProvider.class);
        when(provider.info()).thenReturn(null);

        DefaultLlmProviderRegistry registry = new DefaultLlmProviderRegistry(List.of(provider));

        StepVerifier.create(registry.getDefaultProvider(Capability.CHAT))
                .verifyErrorMessage("At least one default LlmProvider is required for CHAT. Use 'LlmProvider.info().isDefault()' to mark a provider as default.");
    }

    @Test
    void testMultipleDefaultProviders() {
        LlmChatProvider provider1 = mock(LlmChatProvider.class);
        LlmProviderInfo info1 = mock(LlmProviderInfo.class);
        when(info1.isDefault()).thenReturn(true);
        when(provider1.info()).thenReturn(info1);
        when(provider1.capability()).thenReturn(Capability.CHAT);

        LlmChatProvider provider2 = mock(LlmChatProvider.class);
        LlmProviderInfo info2 = mock(LlmProviderInfo.class);
        when(info2.isDefault()).thenReturn(true);
        when(provider2.info()).thenReturn(info2);
        when(provider2.capability()).thenReturn(Capability.CHAT);

        DefaultLlmProviderRegistry registry = new DefaultLlmProviderRegistry(List.of(provider1, provider2));

        StepVerifier.create((Mono<LlmProvider>) registry.getDefaultProvider(Capability.CHAT))
                .expectNext(provider1)
                .verifyComplete();
    }

    @Test
    void testGetChatProvider() {
        LlmChatProvider provider1 = mock(LlmChatProvider.class);
        LlmProviderInfo info1 = mock(LlmProviderInfo.class);
        when(info1.name()).thenReturn("chat-1");
        when(provider1.info()).thenReturn(info1);
        when(provider1.capability()).thenReturn(Capability.CHAT);

        DefaultLlmProviderRegistry registry = new DefaultLlmProviderRegistry(List.of(provider1));

        StepVerifier.create(registry.getChatProvider(info -> info.name().equals("chat-1")))
                .expectNext(provider1)
                .verifyComplete();
                
        StepVerifier.create(registry.getChatProvider(info -> info.name().equals("chat-2")))
                .verifyErrorMessage("No LlmChatProvider found that matches the given filter.");
    }

    @Test
    void testGetEmbeddingProvider() {
        LlmEmbeddingProvider provider1 = mock(LlmEmbeddingProvider.class);
        LlmProviderInfo info1 = mock(LlmProviderInfo.class);
        when(info1.name()).thenReturn("embed-1");
        when(provider1.info()).thenReturn(info1);
        when(provider1.capability()).thenReturn(Capability.EMBEDDING);

        DefaultLlmProviderRegistry registry = new DefaultLlmProviderRegistry(List.of(provider1));

        StepVerifier.create(registry.getEmbeddingProvider(info -> info.name().equals("embed-1")))
                .expectNext(provider1)
                .verifyComplete();
                
        StepVerifier.create(registry.getEmbeddingProvider(info -> info.name().equals("embed-2")))
                .verifyErrorMessage("No LlmEmbeddingProvider found that matches the given filter.");
    }

    @Test
    void testGetProvider() {
        LlmChatProvider provider1 = mock(LlmChatProvider.class);
        LlmProviderInfo info1 = mock(LlmProviderInfo.class);
        when(info1.name()).thenReturn("chat-1");
        when(provider1.info()).thenReturn(info1);
        when(provider1.capability()).thenReturn(Capability.CHAT);

        DefaultLlmProviderRegistry registry = new DefaultLlmProviderRegistry(List.of(provider1));

        StepVerifier.create(registry.getProvider(Capability.CHAT, LlmChatProvider.class, info -> info.name().equals("chat-1")))
                .expectNext(provider1)
                .verifyComplete();
                
        StepVerifier.create(registry.getProvider(Capability.CHAT, LlmChatProvider.class, info -> info.name().equals("chat-2")))
                .verifyErrorMessage("No provider found that matches the given capability and filter.");
    }
    @Test
    void testNonNullChecks() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new DefaultLlmProviderRegistry(null))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new DefaultLlmProviderRegistry(java.util.List.of()))
                .isInstanceOf(IllegalArgumentException.class);
                
        pro.chenggang.project.reactive.ai.lite.core.provider.LlmProvider mockProvider = org.mockito.Mockito.mock(pro.chenggang.project.reactive.ai.lite.core.provider.LlmProvider.class);
        pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo mockInfo = org.mockito.Mockito.mock(pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo.class);
        org.mockito.Mockito.when(mockProvider.info()).thenReturn(mockInfo);
        
        DefaultLlmProviderRegistry registry = new DefaultLlmProviderRegistry(java.util.List.of(mockProvider));
        
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> registry.getDefaultProvider(null))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> registry.getChatProvider(null))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> registry.getEmbeddingProvider(null))
                .isInstanceOf(IllegalArgumentException.class);
                
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> registry.getProvider(null, pro.chenggang.project.reactive.ai.lite.core.provider.LlmChatProvider.class, info -> true))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> registry.getProvider(pro.chenggang.project.reactive.ai.lite.core.option.Capability.CHAT, null, info -> true))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> registry.getProvider(pro.chenggang.project.reactive.ai.lite.core.option.Capability.CHAT, pro.chenggang.project.reactive.ai.lite.core.provider.LlmChatProvider.class, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
