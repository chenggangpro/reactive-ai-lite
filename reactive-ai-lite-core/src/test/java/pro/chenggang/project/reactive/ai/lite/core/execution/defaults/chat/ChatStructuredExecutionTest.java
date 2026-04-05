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
package pro.chenggang.project.reactive.ai.lite.core.execution.defaults.chat;

import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.StructuredResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmChatProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatStructuredExecutionTest {

    @Test
    void testExecutionSpec() {
        LlmProviderRegistry registry = mock(LlmProviderRegistry.class);
        ExecutionSpec spec = ExecutionSpec.builder()
                .llmClientType(LlmClientType.CHAT)
                .modelNameConfigure(ctx -> "gpt-4")
                .build();
        
        ChatStructuredExecution execution = ChatStructuredExecution.of(registry, spec);
        assertThat(execution.executionSpec()).isEqualTo(spec);
    }

    @Test
    void testExecuteClass() {
        LlmProviderRegistry registry = mock(LlmProviderRegistry.class);
        LlmChatProvider provider = mock(LlmChatProvider.class);
        when(registry.getChatProvider(any())).thenReturn(provider);
        
        ExecutionSpec spec = ExecutionSpec.builder()
                .llmClientType(LlmClientType.CHAT)
                .modelNameConfigure(ctx -> "gpt-4")
                .build();
        
        StructuredResponse<String> response = mock(StructuredResponse.class);
        when(provider.executeStructured(any(), (Class<String>) any())).thenReturn(Mono.just(response));
        
        ChatStructuredExecution execution = ChatStructuredExecution.of(registry, spec);
        StepVerifier.create(execution.execute(String.class))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void testExecuteParameterizedType() {
        LlmProviderRegistry registry = mock(LlmProviderRegistry.class);
        LlmChatProvider provider = mock(LlmChatProvider.class);
        when(registry.getChatProvider(any())).thenReturn(provider);
        
        ExecutionSpec spec = ExecutionSpec.builder()
                .llmClientType(LlmClientType.CHAT)
                .modelNameConfigure(ctx -> "gpt-4")
                .build();
        
        StructuredResponse<Collections> response = mock(StructuredResponse.class);
        when(provider.executeStructured(any(), (ParameterizedTypeReference<Collections>) any())).thenReturn(Mono.just(response));
        
        ChatStructuredExecution execution = ChatStructuredExecution.of(registry, spec);
        StepVerifier.create(execution.execute(new ParameterizedTypeReference<Collections>() {}))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void testExecuteRawSchema() {
        LlmProviderRegistry registry = mock(LlmProviderRegistry.class);
        LlmChatProvider provider = mock(LlmChatProvider.class);
        when(registry.getChatProvider(any())).thenReturn(provider);
        
        ExecutionSpec spec = ExecutionSpec.builder()
                .llmClientType(LlmClientType.CHAT)
                .modelNameConfigure(ctx -> "gpt-4")
                .build();
        
        RawResponse response = mock(RawResponse.class);
        when(provider.executeStructuredRaw(any(), any(String.class))).thenReturn(Mono.just(response));
        
        ChatStructuredExecution execution = ChatStructuredExecution.of(registry, spec);
        StepVerifier.create(execution.executeRaw("{}"))
                .expectNext(response)
                .verifyComplete();
    }
}
