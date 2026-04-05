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
package pro.chenggang.project.reactive.ai.lite.core.spec.defaults;

import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import pro.chenggang.project.reactive.ai.lite.core.spec.ConfigurableChatSpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DefaultProviderSpecTest {

    @Test
    void testTransitions() {
        LlmProviderRegistry registry = mock(LlmProviderRegistry.class);
        DefaultExecutionContextSpec contextSpec = DefaultExecutionContextSpec.of(LlmClientType.CHAT, registry);
        DefaultProviderSpec spec = DefaultProviderSpec.of(LlmClientType.CHAT, registry, contextSpec);
        
        spec.defaultProvider()
            .defaultProfile()
            .defaultSystemMessage(ctx -> "system");
            
        assertThat(spec.isDefaultProvider()).isTrue();
        assertThat(spec.isDefaultProfile()).isTrue();
        assertThat(spec.getDefaultSystemMessageProvider().apply(null)).isEqualTo("system");

        ConfigurableChatSpec chatSpec = spec.chatSpec();
        assertThat(chatSpec).isNotNull();
        assertThat(chatSpec).isInstanceOf(DefaultConfigurableChatSpec.class);
    }

    @Test
    void testCustomConfig() {
        LlmProviderRegistry registry = mock(LlmProviderRegistry.class);
        DefaultExecutionContextSpec contextSpec = DefaultExecutionContextSpec.of(LlmClientType.CHAT, registry);
        DefaultProviderSpec spec = DefaultProviderSpec.of(LlmClientType.CHAT, registry, contextSpec);
        
        spec.firstProvider((info, ctx) -> true)
            .profile((ctx, profiles) -> "custom");
            
        assertThat(spec.isDefaultProvider()).isFalse();
        assertThat(spec.getProviderFilter()).isNotNull();
        assertThat(spec.isDefaultProfile()).isFalse();
        assertThat(spec.getProfilePicker()).isNotNull();
    }

    @Test
    void testChatSpecReinitialization() {
        LlmProviderRegistry registry = mock(LlmProviderRegistry.class);
        DefaultExecutionContextSpec contextSpec = DefaultExecutionContextSpec.of(LlmClientType.CHAT, registry);
        DefaultProviderSpec spec = DefaultProviderSpec.of(LlmClientType.CHAT, registry, contextSpec);
        
        ConfigurableChatSpec chatSpec1 = spec.chatSpec();
        ConfigurableChatSpec chatSpec2 = spec.chatSpec();
        assertThat(chatSpec1).isNotSameAs(chatSpec2);
    }
}
