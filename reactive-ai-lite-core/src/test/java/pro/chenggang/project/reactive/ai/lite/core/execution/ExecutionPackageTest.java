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
package pro.chenggang.project.reactive.ai.lite.core.execution;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.exception.StructuredMessageExtractFailedException;
import pro.chenggang.project.reactive.ai.lite.core.execution.defaults.chat.ChatGeneralExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.defaults.chat.ChatStreamExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.defaults.chat.ChatStructuredExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.defaults.embedding.EmbeddingGeneralExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.EmbeddingResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.GeneralResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.StreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ChatExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ChatExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.EmbeddingExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.EmbeddingExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.message.AssistantTextMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.DefaultAssistantTextMessage;
import pro.chenggang.project.reactive.ai.lite.core.option.Capability;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmChatProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmEmbeddingProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExecutionPackageTest {

    private LlmProviderRegistry registry;
    private LlmChatProvider chatProvider;
    private LlmEmbeddingProvider embeddingProvider;

    @BeforeEach
    void setUp() {
        registry = mock(LlmProviderRegistry.class);
        chatProvider = mock(LlmChatProvider.class);
        embeddingProvider = mock(LlmEmbeddingProvider.class);

        when(registry.getProvider(any(), any(), any())).thenAnswer(invocation -> {
            Class<?> clazz = invocation.getArgument(1);
            if (clazz == LlmChatProvider.class) {
                return Mono.just(chatProvider);
            } else {
                return Mono.just(embeddingProvider);
            }
        });
        when(registry.getDefaultProvider(any())).thenAnswer(invocation -> {
            Capability capability = invocation.getArgument(0);
            if (capability == Capability.CHAT) {
                return Mono.just(chatProvider);
            } else {
                return Mono.just(embeddingProvider);
            }
        });
        when(registry.getChatProvider(any())).thenReturn(Mono.just(chatProvider));
        when(registry.getEmbeddingProvider(any())).thenReturn(Mono.just(embeddingProvider));
    }

    @Test
    void testChatGeneralExecution() {
        ChatExecutionSpec spec = ChatExecutionSpec.builder().llmClientType(LlmClientType.CHAT)
                .defaultProvider(true)
                .defaultProfile(true)
                .modelNameConfigure(ctx -> "test_model")
                .build();
        GeneralExecution execution = ChatGeneralExecution.of(registry, spec);

        GeneralResponse response = mock(GeneralResponse.class);
        when(chatProvider.executeGeneral(any(ChatExecutionInfo.class))).thenReturn(Mono.just(response));

        StepVerifier.create(execution.execute())
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void testChatStreamExecution() {
        ChatExecutionSpec spec = ChatExecutionSpec.builder().llmClientType(LlmClientType.CHAT)
                .defaultProvider(true)
                .defaultProfile(true)
                .modelNameConfigure(ctx -> "test_model")
                .build();
        StreamExecution execution = ChatStreamExecution.of(registry, spec);

        StreamResponse response = mock(StreamResponse.class);
        when(chatProvider.executeStream(any(ChatExecutionInfo.class))).thenReturn(Flux.just(response));

        StepVerifier.create(execution.execute())
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void testEmbeddingGeneralExecution() {
        EmbeddingExecutionSpec spec = EmbeddingExecutionSpec.builder().llmClientType(LlmClientType.EMBEDDING)
                .defaultProvider(true)
                .defaultProfile(true)
                .modelNameConfigure(ctx -> "test_model")
                .build();
        EmbeddingExecution execution = EmbeddingGeneralExecution.of(registry, spec);

        EmbeddingResponse response = mock(EmbeddingResponse.class);
        when(embeddingProvider.executeEmbedding(any(EmbeddingExecutionInfo.class))).thenReturn(Mono.just(response));

        StepVerifier.create(execution.execute())
                .expectNext(response)
                .verifyComplete();
    }

    static class TestData {
        public String name;
    }

    @Test
    void testChatStructuredExecutionExecuteClass() {
        ChatExecutionSpec spec = ChatExecutionSpec.builder().llmClientType(LlmClientType.CHAT)
                .defaultProvider(true)
                .defaultProfile(true)
                .modelNameConfigure(ctx -> "test_model")
                .build();
        StructuredExecution execution = ChatStructuredExecution.of(registry, spec);

        GeneralResponse response = mock(GeneralResponse.class);
        AssistantTextMessage atm = DefaultAssistantTextMessage.builder().content("{\"name\":\"test_name\"}").build();
        when(response.getAssistantTextMessage()).thenReturn(atm);
        when(response.getExecutionContext()).thenReturn(ExecutionContext.newContext());
        when(response.getRawResponseBody()).thenReturn(mock(ObjectNode.class));
        
        when(chatProvider.executeGeneral(any(ChatExecutionInfo.class))).thenReturn(Mono.just(response));

        StepVerifier.create(execution.execute(TestData.class))
                .assertNext(res -> {
                    assertThat(res.getStructuredContent().name).isEqualTo("test_name");
                })
                .verifyComplete();
    }

    @Test
    void testChatStructuredExecutionExecuteParameterizedType() {
        ChatExecutionSpec spec = ChatExecutionSpec.builder().llmClientType(LlmClientType.CHAT)
                .defaultProvider(true)
                .defaultProfile(true)
                .modelNameConfigure(ctx -> "test_model")
                .build();
        StructuredExecution execution = ChatStructuredExecution.of(registry, spec);

        GeneralResponse response = mock(GeneralResponse.class);
        AssistantTextMessage atm = DefaultAssistantTextMessage.builder().content("{\"key\":\"value\"}").build();
        when(response.getAssistantTextMessage()).thenReturn(atm);
        when(response.getExecutionContext()).thenReturn(ExecutionContext.newContext());
        when(response.getRawResponseBody()).thenReturn(mock(ObjectNode.class));

        when(chatProvider.executeGeneral(any(ChatExecutionInfo.class))).thenReturn(Mono.just(response));

        StepVerifier.create(execution.execute(new ParameterizedTypeReference<Map<String, String>>() {}))
                .assertNext(res -> {
                    assertThat(res.getStructuredContent()).containsEntry("key", "value");
                })
                .verifyComplete();
    }

    @Test
    void testChatStructuredExecutionExecuteRawString() {
        ChatExecutionSpec spec = ChatExecutionSpec.builder().llmClientType(LlmClientType.CHAT)
                .defaultProvider(true)
                .defaultProfile(true)
                .modelNameConfigure(ctx -> "test_model")
                .build();
        StructuredExecution execution = ChatStructuredExecution.of(registry, spec);

        RawResponse rawResponse = mock(RawResponse.class);
        when(chatProvider.executeGeneralRaw(any(ChatExecutionInfo.class))).thenReturn(Mono.just(rawResponse));

        StepVerifier.create(execution.executeRaw("{\"type\":\"object\"}"))
                .expectNext(rawResponse)
                .verifyComplete();
    }

    @Test
    void testChatStructuredExecutionExecuteRawClass() {
        ChatExecutionSpec spec = ChatExecutionSpec.builder().llmClientType(LlmClientType.CHAT)
                .defaultProvider(true)
                .defaultProfile(true)
                .modelNameConfigure(ctx -> "test_model")
                .build();
        StructuredExecution execution = ChatStructuredExecution.of(registry, spec);

        RawResponse rawResponse = mock(RawResponse.class);
        when(chatProvider.executeGeneralRaw(any(ChatExecutionInfo.class))).thenReturn(Mono.just(rawResponse));

        StepVerifier.create(execution.executeRaw(TestData.class))
                .expectNext(rawResponse)
                .verifyComplete();
    }
    
    @Test
    void testChatStructuredExecutionExecuteRawParameterizedType() {
        ChatExecutionSpec spec = ChatExecutionSpec.builder().llmClientType(LlmClientType.CHAT)
                .defaultProvider(true)
                .defaultProfile(true)
                .modelNameConfigure(ctx -> "test_model")
                .build();
        StructuredExecution execution = ChatStructuredExecution.of(registry, spec);

        RawResponse rawResponse = mock(RawResponse.class);
        when(chatProvider.executeGeneralRaw(any(ChatExecutionInfo.class))).thenReturn(Mono.just(rawResponse));

        StepVerifier.create(execution.executeRaw(new ParameterizedTypeReference<Map<String, String>>() {}))
                .expectNext(rawResponse)
                .verifyComplete();
    }

    @Test
    void testChatStructuredExecutionEmptyContent() {
        ChatExecutionSpec spec = ChatExecutionSpec.builder().llmClientType(LlmClientType.CHAT)
                .defaultProvider(true)
                .defaultProfile(true)
                .modelNameConfigure(ctx -> "test_model")
                .build();
        StructuredExecution execution = ChatStructuredExecution.of(registry, spec);

        GeneralResponse response = mock(GeneralResponse.class);
        AssistantTextMessage atm = DefaultAssistantTextMessage.builder().content("").build();
        when(response.getAssistantTextMessage()).thenReturn(atm);
        when(response.getRawResponseBody()).thenReturn(mock(ObjectNode.class));

        when(chatProvider.executeGeneral(any(ChatExecutionInfo.class))).thenReturn(Mono.just(response));

        StepVerifier.create(execution.execute(TestData.class))
                .expectError(StructuredMessageExtractFailedException.class)
                .verify();
    }
    
    @Test
    void testChatStructuredExecutionNullMessage() {
        ChatExecutionSpec spec = ChatExecutionSpec.builder().llmClientType(LlmClientType.CHAT)
                .defaultProvider(true)
                .defaultProfile(true)
                .modelNameConfigure(ctx -> "test_model")
                .build();
        StructuredExecution execution = ChatStructuredExecution.of(registry, spec);

        GeneralResponse response = mock(GeneralResponse.class);
        when(response.getAssistantTextMessage()).thenReturn(null);
        when(response.getRawResponseBody()).thenReturn(mock(ObjectNode.class));

        when(chatProvider.executeGeneral(any(ChatExecutionInfo.class))).thenReturn(Mono.just(response));

        StepVerifier.create(execution.execute(TestData.class))
                .expectError(StructuredMessageExtractFailedException.class)
                .verify();
    }

    @Test
    void testChatStructuredExecutionInvalidJson() {
        ChatExecutionSpec spec = ChatExecutionSpec.builder().llmClientType(LlmClientType.CHAT)
                .defaultProvider(true)
                .defaultProfile(true)
                .modelNameConfigure(ctx -> "test_model")
                .build();
        StructuredExecution execution = ChatStructuredExecution.of(registry, spec);

        GeneralResponse response = mock(GeneralResponse.class);
        AssistantTextMessage atm = DefaultAssistantTextMessage.builder().content("invalid json").build();
        when(response.getAssistantTextMessage()).thenReturn(atm);
        when(response.getRawResponseBody()).thenReturn(mock(ObjectNode.class));

        when(chatProvider.executeGeneral(any(ChatExecutionInfo.class))).thenReturn(Mono.just(response));

        StepVerifier.create(execution.execute(TestData.class))
                .expectError(StructuredMessageExtractFailedException.class)
                .verify();
    }
    
    @Test
    void testChatStructuredExecutionMarkdownExtraction() {
        ChatExecutionSpec spec = ChatExecutionSpec.builder().llmClientType(LlmClientType.CHAT)
                .defaultProvider(true)
                .defaultProfile(true)
                .modelNameConfigure(ctx -> "test_model")
                .build();
        StructuredExecution execution = ChatStructuredExecution.of(registry, spec);

        GeneralResponse response = mock(GeneralResponse.class);
        AssistantTextMessage atm = DefaultAssistantTextMessage.builder().content("```json\n{\"name\":\"markdown_name\"}\n```").build();
        when(response.getAssistantTextMessage()).thenReturn(atm);
        when(response.getExecutionContext()).thenReturn(ExecutionContext.newContext());
        when(response.getRawResponseBody()).thenReturn(mock(ObjectNode.class));

        when(chatProvider.executeGeneral(any(ChatExecutionInfo.class))).thenReturn(Mono.just(response));

        StepVerifier.create(execution.execute(TestData.class))
                .assertNext(res -> {
                    assertThat(res.getStructuredContent().name).isEqualTo("markdown_name");
                })
                .verifyComplete();
    }

    @Test
    void testLlmProviderExecutorWithoutClientType() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> 
            ChatExecutionSpec.builder()
                .defaultProvider(false)
                .modelNameConfigure(ctx -> "test_model")
                .build()
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
