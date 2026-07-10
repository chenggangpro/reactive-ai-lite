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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;
import pro.chenggang.project.reactive.ai.lite.core.exception.StructuredMessageExtractFailedException;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.GeneralResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ChatExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.message.AssistantTextMessage;
import pro.chenggang.project.reactive.ai.lite.core.message.defaults.DefaultAssistantTextMessage;
import pro.chenggang.project.reactive.ai.lite.core.provider.defaults.DefaultLlmChatProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatStructuredExecutionTest {

    @Mock
    private LlmProviderRegistry llmProviderRegistry;
    @Mock
    private DefaultLlmChatProvider llmChatProvider;
    
    private ChatStructuredExecution execution;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        ChatExecutionSpec spec = ChatExecutionSpec.builder()
                .llmClientType(pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType.CHAT)
                .modelNameConfigure(ctx -> "test-model").contextConfigure((ctx, attr) -> {}).parentAttributes(java.util.Collections.emptyMap())
                .build();
        when(llmProviderRegistry.getProvider(any(pro.chenggang.project.reactive.ai.lite.core.option.Capability.class), any(Class.class), any()))
            .thenReturn((Mono) Mono.just(llmChatProvider));
            
        execution = ChatStructuredExecution.of(llmProviderRegistry, spec);
    }

    @Test
    void testExecuteWithClass() {
        AssistantTextMessage assistantMsg = DefaultAssistantTextMessage.builder()
                .content("```json\n{\"testKey\":\"testValue\"}\n```")
                .build();
        GeneralResponse generalResponse = mock(GeneralResponse.class);
        when(generalResponse.getAssistantTextMessage()).thenReturn(assistantMsg);
        
        when(llmChatProvider.executeGeneral(any())).thenReturn(Mono.just(generalResponse));

        StepVerifier.create(execution.execute(Map.class))
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getStructuredContent()).isInstanceOf(Map.class);
                    Map map = (Map) response.getStructuredContent();
                    assertThat(map.get("testKey")).isEqualTo("testValue");
                })
                .verifyComplete();
    }
    
    @Test
    void testExecuteWithParameterizedType() {
        AssistantTextMessage assistantMsg = DefaultAssistantTextMessage.builder()
                .content("{\"testKey\":\"testValue\"}")
                .build();
        GeneralResponse generalResponse = mock(GeneralResponse.class);
        when(generalResponse.getAssistantTextMessage()).thenReturn(assistantMsg);
        
        when(llmChatProvider.executeGeneral(any())).thenReturn(Mono.just(generalResponse));

        StepVerifier.create(execution.execute(new ParameterizedTypeReference<Map>() {}))
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    Map<String, String> map = (Map<String, String>) response.getStructuredContent();
                    assertThat(map.get("testKey")).isEqualTo("testValue");
                })
                .verifyComplete();
    }
    
    @Test
    void testExecuteWithNullAssistantMessage() {
        GeneralResponse generalResponse = mock(GeneralResponse.class);
        when(generalResponse.getAssistantTextMessage()).thenReturn(null);
        when(generalResponse.getRawResponseBody()).thenReturn(pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil.OBJECT_MAPPER.createObjectNode());
        
        when(llmChatProvider.executeGeneral(any())).thenReturn(Mono.just(generalResponse));

        StepVerifier.create(execution.execute(Map.class))
                .expectError(StructuredMessageExtractFailedException.class)
                .verify();
    }
    
    @Test
    void testExecuteWithEmptyContent() {
        AssistantTextMessage assistantMsg = DefaultAssistantTextMessage.builder()
                .content("   ")
                .build();
        GeneralResponse generalResponse = mock(GeneralResponse.class);
        when(generalResponse.getAssistantTextMessage()).thenReturn(assistantMsg);
        when(generalResponse.getRawResponseBody()).thenReturn(pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil.OBJECT_MAPPER.createObjectNode());
        
        when(llmChatProvider.executeGeneral(any())).thenReturn(Mono.just(generalResponse));

        StepVerifier.create(execution.execute(Map.class))
                .expectError(StructuredMessageExtractFailedException.class)
                .verify();
    }
    
    @Test
    void testExecuteRawWithClass() {
        RawResponse rawResponse = mock(RawResponse.class);
        when(llmChatProvider.executeGeneralRaw(any())).thenReturn(Mono.just(rawResponse));

        StepVerifier.create(execution.executeRaw(Map.class))
                .expectNext(rawResponse)
                .verifyComplete();
    }
    
    @Test
    void testExecuteRawWithParameterizedType() {
        RawResponse rawResponse = mock(RawResponse.class);
        when(llmChatProvider.executeGeneralRaw(any())).thenReturn(Mono.just(rawResponse));

        StepVerifier.create(execution.executeRaw(new ParameterizedTypeReference<Map>() {}))
                .expectNext(rawResponse)
                .verifyComplete();
    }
    
    @Test
    void testExecuteRawWithString() {
        RawResponse rawResponse = mock(RawResponse.class);
        when(llmChatProvider.executeGeneralRaw(any())).thenReturn(Mono.just(rawResponse));

        StepVerifier.create(execution.executeRaw("{\"type\":\"object\"}"))
                .expectNext(rawResponse)
                .verifyComplete();
    }
    
    @Test
    void testExecuteParseError() {
        AssistantTextMessage assistantMsg = DefaultAssistantTextMessage.builder()
                .content("```json\n{invalid json\n```")
                .build();
        GeneralResponse generalResponse = mock(GeneralResponse.class);
        when(generalResponse.getAssistantTextMessage()).thenReturn(assistantMsg);
        when(generalResponse.getRawResponseBody()).thenReturn(pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil.OBJECT_MAPPER.createObjectNode());
        
        when(llmChatProvider.executeGeneral(any())).thenReturn(Mono.just(generalResponse));

        StepVerifier.create(execution.execute(Map.class))
                .expectError(StructuredMessageExtractFailedException.class)
                .verify();
    }
    
    @Test
    void testExecuteWithParameterizedTypeNullAssistantMessage() {
        GeneralResponse generalResponse = mock(GeneralResponse.class);
        when(generalResponse.getAssistantTextMessage()).thenReturn(null);
        when(generalResponse.getRawResponseBody()).thenReturn(pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil.OBJECT_MAPPER.createObjectNode());
        
        when(llmChatProvider.executeGeneral(any())).thenReturn(Mono.just(generalResponse));

        StepVerifier.create(execution.execute(new ParameterizedTypeReference<Map>() {}))
                .expectError(StructuredMessageExtractFailedException.class)
                .verify();
    }
    
    @Test
    void testExecuteWithParameterizedTypeEmptyContent() {
        AssistantTextMessage assistantMsg = DefaultAssistantTextMessage.builder()
                .content("   ")
                .build();
        GeneralResponse generalResponse = mock(GeneralResponse.class);
        when(generalResponse.getAssistantTextMessage()).thenReturn(assistantMsg);
        when(generalResponse.getRawResponseBody()).thenReturn(pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil.OBJECT_MAPPER.createObjectNode());
        
        when(llmChatProvider.executeGeneral(any())).thenReturn(Mono.just(generalResponse));

        StepVerifier.create(execution.execute(new ParameterizedTypeReference<Map>() {}))
                .expectError(StructuredMessageExtractFailedException.class)
                .verify();
    }
    
    @Test
    void testExecuteWithParameterizedTypeParseError() {
        AssistantTextMessage assistantMsg = DefaultAssistantTextMessage.builder()
                .content("```\n{invalid json\n```")
                .build();
        GeneralResponse generalResponse = mock(GeneralResponse.class);
        when(generalResponse.getAssistantTextMessage()).thenReturn(assistantMsg);
        when(generalResponse.getRawResponseBody()).thenReturn(pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil.OBJECT_MAPPER.createObjectNode());
        
        when(llmChatProvider.executeGeneral(any())).thenReturn(Mono.just(generalResponse));

        StepVerifier.create(execution.execute(new ParameterizedTypeReference<Map>() {}))
                .expectError(StructuredMessageExtractFailedException.class)
                .verify();
    }
    
    @Test
    void testExecuteWithEmptyExtractedContent() {
        AssistantTextMessage assistantMsg = DefaultAssistantTextMessage.builder()
                .content("```json\n   \n```")
                .build();
        GeneralResponse generalResponse = mock(GeneralResponse.class);
        when(generalResponse.getAssistantTextMessage()).thenReturn(assistantMsg);
        when(generalResponse.getRawResponseBody()).thenReturn(pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil.OBJECT_MAPPER.createObjectNode());
        
        when(llmChatProvider.executeGeneral(any())).thenReturn(Mono.just(generalResponse));

        StepVerifier.create(execution.execute(Map.class))
                .expectError(StructuredMessageExtractFailedException.class)
                .verify();
    }
    
    @Test
    void testExecuteWithParameterizedTypeEmptyExtractedContent() {
        AssistantTextMessage assistantMsg = DefaultAssistantTextMessage.builder()
                .content("```json\n   \n```")
                .build();
        GeneralResponse generalResponse = mock(GeneralResponse.class);
        when(generalResponse.getAssistantTextMessage()).thenReturn(assistantMsg);
        when(generalResponse.getRawResponseBody()).thenReturn(pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil.OBJECT_MAPPER.createObjectNode());
        
        when(llmChatProvider.executeGeneral(any())).thenReturn(Mono.just(generalResponse));

        StepVerifier.create(execution.execute(new ParameterizedTypeReference<Map>() {}))
                .expectError(StructuredMessageExtractFailedException.class)
                .verify();
    }
    @Test
    void testConstructorAndOfWithNulls() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> ChatStructuredExecution.of(null, mock(ChatExecutionSpec.class)))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> ChatStructuredExecution.of(mock(LlmProviderRegistry.class), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
    
    @Test
    void testExecuteMethodsWithNulls() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> execution.execute((Class<Object>) null))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> execution.execute((ParameterizedTypeReference<Object>) null))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> execution.executeRaw((Class<Object>) null))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> execution.executeRaw((ParameterizedTypeReference<Object>) null))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> execution.executeRaw((String) null))
                .isInstanceOf(IllegalArgumentException.class);
    }
    
    @Test
    void testExecuteWithNullContent() {
        AssistantTextMessage assistantMsg = mock(AssistantTextMessage.class);
        when(assistantMsg.getContent()).thenReturn(null);
        GeneralResponse generalResponse = mock(GeneralResponse.class);
        when(generalResponse.getAssistantTextMessage()).thenReturn(assistantMsg);
        when(generalResponse.getRawResponseBody()).thenReturn(pro.chenggang.project.reactive.ai.lite.core.util.JsonRelatedUtil.OBJECT_MAPPER.createObjectNode());
        
        when(llmChatProvider.executeGeneral(any())).thenReturn(Mono.just(generalResponse));

        StepVerifier.create(execution.execute(Map.class))
                .expectError(StructuredMessageExtractFailedException.class)
                .verify();
                
        StepVerifier.create(execution.execute(new ParameterizedTypeReference<Map>() {}))
                .expectError(StructuredMessageExtractFailedException.class)
                .verify();
    }
    @Test
    void testExtractJsonContentNull() throws Exception {
        java.lang.reflect.Method method = ChatStructuredExecution.class.getDeclaredMethod("extractJsonContent", String.class);
        method.setAccessible(true);
        Object result = method.invoke(execution, (String) null);
        org.assertj.core.api.Assertions.assertThat(result).isNull();
    }
    @Test
    void testPrivateConstructorWithNulls() throws Exception {
        java.lang.reflect.Constructor<ChatStructuredExecution> constructor = ChatStructuredExecution.class.getDeclaredConstructor(LlmProviderRegistry.class, ChatExecutionSpec.class);
        constructor.setAccessible(true);
        
        try {
            constructor.newInstance(null, mock(ChatExecutionSpec.class));
            org.junit.jupiter.api.Assertions.fail("Should have thrown InvocationTargetException");
        } catch (java.lang.reflect.InvocationTargetException e) {
            org.assertj.core.api.Assertions.assertThat(e.getCause())
                    .isInstanceOf(IllegalArgumentException.class);
        }
        
        try {
            constructor.newInstance(mock(LlmProviderRegistry.class), null);
            org.junit.jupiter.api.Assertions.fail("Should have thrown InvocationTargetException");
        } catch (java.lang.reflect.InvocationTargetException e) {
            org.assertj.core.api.Assertions.assertThat(e.getCause())
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
    @Test
    void testExecuteDefaultMethodsWithConverter() {
        RawResponse rawResponse = mock(RawResponse.class);
        when(llmChatProvider.executeGeneralRaw(any())).thenReturn(Mono.just(rawResponse));
        pro.chenggang.project.reactive.ai.lite.core.execution.converter.RawResponseConverter<Map> converter = response -> java.util.Collections.emptyMap();
        
        StepVerifier.create(execution.execute("schema", converter))
                .expectNext(java.util.Collections.emptyMap())
                .verifyComplete();
                
        StepVerifier.create(execution.execute(Map.class, converter))
                .expectNext(java.util.Collections.emptyMap())
                .verifyComplete();
                
        StepVerifier.create(execution.execute(new ParameterizedTypeReference<Map>() {}, converter))
                .expectNext(java.util.Collections.emptyMap())
                .verifyComplete();
    }
    @Test
    void testExecuteDefaultMethodsWithNulls() {
        pro.chenggang.project.reactive.ai.lite.core.execution.converter.RawResponseConverter<Map> converter = response -> java.util.Collections.emptyMap();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> execution.execute("schema", (pro.chenggang.project.reactive.ai.lite.core.execution.converter.RawResponseConverter<Map>) null))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> execution.execute((String) null, converter))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> execution.execute(Map.class, (pro.chenggang.project.reactive.ai.lite.core.execution.converter.RawResponseConverter<Map>) null))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> execution.execute((Class<Map>) null, converter))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> execution.execute(new ParameterizedTypeReference<Map>() {}, (pro.chenggang.project.reactive.ai.lite.core.execution.converter.RawResponseConverter<Map>) null))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> execution.execute((ParameterizedTypeReference<Map>) null, converter))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
