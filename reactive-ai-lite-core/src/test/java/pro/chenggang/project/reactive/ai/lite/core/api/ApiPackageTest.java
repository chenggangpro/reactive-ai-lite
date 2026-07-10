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
package pro.chenggang.project.reactive.ai.lite.core.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import pro.chenggang.project.reactive.ai.lite.core.api.defaults.DefaultReactiveLlmClient;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import pro.chenggang.project.reactive.ai.lite.core.spec.ConfigurableChatSpec;
import pro.chenggang.project.reactive.ai.lite.core.spec.ConfigurableEmbeddingSpec;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class ApiPackageTest {

    @Mock
    private LlmProviderRegistry llmProviderRegistry;

    private ReactiveLlmClient reactiveLlmClient;

    @BeforeEach
    void setUp() {
        reactiveLlmClient = new DefaultReactiveLlmClient(llmProviderRegistry);
    }

    @Test
    void testReactiveLlmClientDefaultChat() {
        ConfigurableChatSpec chat = reactiveLlmClient.chat();
        assertThat(chat).isNotNull();
    }

    @Test
    void testReactiveLlmClientDefaultEmbedding() {
        ConfigurableEmbeddingSpec embedding = reactiveLlmClient.embedding();
        assertThat(embedding).isNotNull();
    }

    @Test
    void testClientRequestConfiguration() {
        ClientRequest request = reactiveLlmClient.newRequest()
                .parentAttributes(Map.of("key", "value"))
                .context(ClientRequest.ContextMerger.APPEND_ALL)
                .defaultProvider()
                .provider("testProvider")
                .defaultProfile()
                .profile("testProfile");

        assertThat(request).isNotNull();
        assertThat(request.chat()).isNotNull();
        assertThat(request.embedding()).isNotNull();
    }

    @Test
    void testContextMergerAppendAll() {
        ExecutionContext executionContext = mock(ExecutionContext.class);
        Map<String, Object> attributes = Map.of("key", "value");
        Map<String, Object> targetMap = new java.util.HashMap<>();
        Mockito.when(executionContext.getAttributes()).thenReturn(targetMap);

        ClientRequest.ContextMerger.APPEND_ALL.merge(executionContext, attributes);
        assertThat(targetMap).containsEntry("key", "value");
    }

    @Test
    void testContextMergerAppendAllWithNullOrEmpty() {
        ExecutionContext executionContext = mock(ExecutionContext.class);
        
        ClientRequest.ContextMerger.APPEND_ALL.merge(executionContext, null);
        ClientRequest.ContextMerger.APPEND_ALL.merge(executionContext, Map.of());
        
        Mockito.verifyNoInteractions(executionContext);
    }
}
