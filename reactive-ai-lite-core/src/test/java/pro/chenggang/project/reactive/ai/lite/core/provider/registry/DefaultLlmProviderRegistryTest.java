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
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmProviderInfo;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultLlmProviderRegistryTest {

    @Test
    void testRegistryInitialization() {
        LlmProvider provider = mock(LlmProvider.class);
        LlmProviderInfo info = mock(LlmProviderInfo.class);
        when(provider.info()).thenReturn(info);
        when(provider.capability()).thenReturn(Capability.CHAT);
        when(info.isDefault()).thenReturn(true);

        DefaultLlmProviderRegistry registry = new DefaultLlmProviderRegistry(Collections.singletonList(provider));
        assertThat(registry.getDefaultProvider(Capability.CHAT)).isEqualTo(provider);
    }

    @Test
    void testGetChatProvider() {
        LlmChatProvider provider = mock(LlmChatProvider.class);
        LlmProviderInfo info = mock(LlmProviderInfo.class);
        when(provider.info()).thenReturn(info);
        when(provider.capability()).thenReturn(Capability.CHAT);
        when(info.name()).thenReturn("test-provider");

        DefaultLlmProviderRegistry registry = new DefaultLlmProviderRegistry(Collections.singletonList(provider));
        LlmChatProvider result = registry.getChatProvider(i -> i.name().equals("test-provider"));
        assertThat(result).isEqualTo(provider);
    }

    @Test
    void testGetChatProviderNotFound() {
        LlmChatProvider provider = mock(LlmChatProvider.class);
        LlmProviderInfo info = mock(LlmProviderInfo.class);
        when(provider.info()).thenReturn(info);
        when(provider.capability()).thenReturn(Capability.CHAT);
        when(info.name()).thenReturn("test-provider");

        DefaultLlmProviderRegistry registry = new DefaultLlmProviderRegistry(Collections.singletonList(provider));
        assertThatThrownBy(() -> registry.getChatProvider(i -> i.name().equals("other")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testEmptyRegistry() {
        assertThatThrownBy(() -> new DefaultLlmProviderRegistry(Collections.emptyList()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testRegistryWithNullInfo() {
        LlmProvider provider = mock(LlmProvider.class);
        when(provider.info()).thenReturn(null);

        LlmProvider validProvider = mock(LlmProvider.class);
        LlmProviderInfo info = mock(LlmProviderInfo.class);
        when(validProvider.info()).thenReturn(info);
        when(validProvider.capability()).thenReturn(Capability.CHAT);
        when(info.isDefault()).thenReturn(true);

        DefaultLlmProviderRegistry registry = new DefaultLlmProviderRegistry(java.util.Arrays.asList(provider, validProvider));
        assertThat(registry.getDefaultProvider(Capability.CHAT)).isEqualTo(validProvider);
    }

    @Test
    void testGetDefaultProviderNotFound() {
        LlmProvider provider = mock(LlmProvider.class);
        LlmProviderInfo info = mock(LlmProviderInfo.class);
        when(provider.info()).thenReturn(info);
        when(provider.capability()).thenReturn(Capability.CHAT);
        when(info.isDefault()).thenReturn(true);

        DefaultLlmProviderRegistry registry = new DefaultLlmProviderRegistry(Collections.singletonList(provider));
        assertThatThrownBy(() -> registry.getDefaultProvider(Capability.AUDIO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
