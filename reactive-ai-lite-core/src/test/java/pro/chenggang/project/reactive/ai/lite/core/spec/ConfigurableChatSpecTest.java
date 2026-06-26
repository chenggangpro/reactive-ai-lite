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
package pro.chenggang.project.reactive.ai.lite.core.spec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import pro.chenggang.project.reactive.ai.lite.core.spec.defaults.DefaultConfigurableChatSpec;
import pro.chenggang.project.reactive.ai.lite.core.spec.defaults.DefaultExecutionContextSpec;
import pro.chenggang.project.reactive.ai.lite.core.spec.defaults.DefaultProviderSpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ConfigurableChatSpecTest {

    private LlmProviderRegistry registry;
    private DefaultExecutionContextSpec contextSpec;
    private DefaultProviderSpec providerSpec;
    private ConfigurableChatSpec spec;

    @BeforeEach
    void setUp() {
        registry = mock(LlmProviderRegistry.class);
        contextSpec = DefaultExecutionContextSpec.of(LlmClientType.CHAT, registry);
        providerSpec = DefaultProviderSpec.of(LlmClientType.CHAT, registry, contextSpec);
        // Using DefaultConfigurableChatSpec to test the interface methods
        spec = new DefaultConfigurableChatSpec(LlmClientType.CHAT, registry, providerSpec);
    }

    @Test
    void testDefaultMethods() {
        // These methods now have default implementations in the interface or are implemented by DefaultConfigurableChatSpec
        assertThat(spec.includeUsage()).isEqualTo(spec);
        assertThat(spec.distinctToolCalls(true)).isEqualTo(spec);
    }
}
