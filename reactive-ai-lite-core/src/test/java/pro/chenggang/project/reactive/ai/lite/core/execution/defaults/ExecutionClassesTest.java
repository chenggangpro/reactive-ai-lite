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
package pro.chenggang.project.reactive.ai.lite.core.execution.defaults;

import org.junit.jupiter.api.Test;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.execution.defaults.chat.ChatGeneralExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.defaults.chat.ChatStreamExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.defaults.embedding.EmbeddingGeneralExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.EmbeddingResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.GeneralResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.StreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ChatExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ChatExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.EmbeddingExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.EmbeddingExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.option.Capability;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmChatProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmEmbeddingProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExecutionClassesTest {

    @Test
    void testChatGeneralExecutionNulls() {
        LlmProviderRegistry registry = mock(LlmProviderRegistry.class);
        ChatExecutionSpec spec = mock(ChatExecutionSpec.class);
        
        assertThatThrownBy(() -> ChatGeneralExecution.of(null, spec))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ChatGeneralExecution.of(registry, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testChatGeneralExecution() {
        LlmProviderRegistry registry = mock(LlmProviderRegistry.class);
        ChatExecutionSpec spec = mock(ChatExecutionSpec.class);
        when(spec.isDefaultProvider()).thenReturn(true);
        when(spec.getLlmClientType()).thenReturn(LlmClientType.CHAT);
        
        LlmChatProvider provider = mock(LlmChatProvider.class);
        when(registry.getDefaultProvider(Capability.CHAT)).thenReturn((Mono) Mono.just(provider));
        
        ChatExecutionInfo info = mock(ChatExecutionInfo.class);
        when(spec.newExecutionInfo(any(ExecutionContext.class))).thenReturn(info);
        
        GeneralResponse generalResponse = mock(GeneralResponse.class);
        when(provider.executeGeneral(any())).thenReturn(Mono.just(generalResponse));
        
        RawResponse rawResponse = mock(RawResponse.class);
        when(provider.executeGeneralRaw(any())).thenReturn(Mono.just(rawResponse));
        
        ChatGeneralExecution execution = (ChatGeneralExecution) ChatGeneralExecution.of(registry, spec);
        
        StepVerifier.create(execution.execute())
                .expectNext(generalResponse)
                .verifyComplete();
                
        StepVerifier.create(execution.executeRaw())
                .expectNext(rawResponse)
                .verifyComplete();
    }

    @Test
    void testChatStreamExecutionNulls() {
        LlmProviderRegistry registry = mock(LlmProviderRegistry.class);
        ChatExecutionSpec spec = mock(ChatExecutionSpec.class);
        
        assertThatThrownBy(() -> ChatStreamExecution.of(null, spec))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ChatStreamExecution.of(registry, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testChatStreamExecution() {
        LlmProviderRegistry registry = mock(LlmProviderRegistry.class);
        ChatExecutionSpec spec = mock(ChatExecutionSpec.class);
        when(spec.isDefaultProvider()).thenReturn(true);
        when(spec.getLlmClientType()).thenReturn(LlmClientType.CHAT);
        
        LlmChatProvider provider = mock(LlmChatProvider.class);
        when(registry.getDefaultProvider(Capability.CHAT)).thenReturn((Mono) Mono.just(provider));
        
        ChatExecutionInfo info = mock(ChatExecutionInfo.class);
        when(spec.newExecutionInfo(any(ExecutionContext.class))).thenReturn(info);
        
        StreamResponse streamResponse = mock(StreamResponse.class);
        when(provider.executeStream(any())).thenReturn(Flux.just(streamResponse));
        
        RawStreamResponse rawStreamResponse = mock(RawStreamResponse.class);
        when(provider.executeStreamRaw(any())).thenReturn(Flux.just(rawStreamResponse));
        
        ChatStreamExecution execution = (ChatStreamExecution) ChatStreamExecution.of(registry, spec);
        
        StepVerifier.create(execution.execute())
                .expectNext(streamResponse)
                .verifyComplete();
                
        StepVerifier.create(execution.executeRaw())
                .expectNext(rawStreamResponse)
                .verifyComplete();
    }
    
    @Test
    void testEmbeddingGeneralExecutionNulls() {
        LlmProviderRegistry registry = mock(LlmProviderRegistry.class);
        EmbeddingExecutionSpec spec = mock(EmbeddingExecutionSpec.class);
        
        assertThatThrownBy(() -> EmbeddingGeneralExecution.of(null, spec))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EmbeddingGeneralExecution.of(registry, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
    
    @Test
    void testEmbeddingGeneralExecution() {
        LlmProviderRegistry registry = mock(LlmProviderRegistry.class);
        EmbeddingExecutionSpec spec = mock(EmbeddingExecutionSpec.class);
        when(spec.isDefaultProvider()).thenReturn(true);
        when(spec.getLlmClientType()).thenReturn(LlmClientType.EMBEDDING);
        
        LlmEmbeddingProvider provider = mock(LlmEmbeddingProvider.class);
        when(registry.getDefaultProvider(Capability.EMBEDDING)).thenReturn((Mono) Mono.just(provider));
        
        EmbeddingExecutionInfo info = mock(EmbeddingExecutionInfo.class);
        when(spec.newExecutionInfo(any(ExecutionContext.class))).thenReturn(info);
        
        EmbeddingResponse embeddingResponse = mock(EmbeddingResponse.class);
        when(provider.executeEmbedding(any())).thenReturn(Mono.just(embeddingResponse));
        
        RawResponse rawResponse = mock(RawResponse.class);
        when(provider.executeEmbeddingRaw(any())).thenReturn(Mono.just(rawResponse));
        
        EmbeddingGeneralExecution execution = (EmbeddingGeneralExecution) EmbeddingGeneralExecution.of(registry, spec);
        
        StepVerifier.create(execution.execute())
                .expectNext(embeddingResponse)
                .verifyComplete();
                
        StepVerifier.create(execution.executeRaw())
                .expectNext(rawResponse)
                .verifyComplete();
    }
    
    @Test
    void testContextWriteInExecution() {
        LlmProviderRegistry registry = mock(LlmProviderRegistry.class);
        ChatExecutionSpec spec = mock(ChatExecutionSpec.class);
        when(spec.isDefaultProvider()).thenReturn(true);
        when(spec.getLlmClientType()).thenReturn(LlmClientType.CHAT);
        when(spec.getParentAttributes()).thenReturn(Map.of("key", "value"));
        
        LlmChatProvider provider = mock(LlmChatProvider.class);
        when(registry.getDefaultProvider(Capability.CHAT)).thenReturn((Mono) Mono.just(provider));
        
        ChatExecutionInfo info = mock(ChatExecutionInfo.class);
        // Ensure execution context gets initialized correctly
        when(spec.newExecutionInfo(any(ExecutionContext.class))).thenAnswer(inv -> {
            ExecutionContext ctx = inv.getArgument(0);
            return info;
        });
        
        when(provider.executeGeneral(any())).thenReturn(Mono.just(mock(GeneralResponse.class)));
        
        ChatGeneralExecution execution = (ChatGeneralExecution) ChatGeneralExecution.of(registry, spec);
        StepVerifier.create(execution.execute())
                .expectNextCount(1)
                .verifyComplete();
    }
}
