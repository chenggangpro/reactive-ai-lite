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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.ParameterizedTypeReference;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContextView;
import pro.chenggang.project.reactive.ai.lite.core.execution.StructuredExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.GeneralResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.message.AssistantTextMessage;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmChatProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatStructuredExecutionTest {

    @Mock
    private LlmProviderRegistry registry;

    @Mock
    private ExecutionSpec spec;

    @Mock
    private LlmChatProvider provider;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private ExecutionInfo executionInfo;

    private StructuredExecution execution;

    @BeforeEach
    void setUp() {
        lenient().when(spec.getLlmClientType()).thenReturn(LlmClientType.CHAT);
        execution = ChatStructuredExecution.of(registry, spec);
        lenient().when(executionContext.getContextView()).thenReturn(mock(ExecutionContextView.class));
        
        // Fix for Builder/ExecutionInfo: ensure executionContext and modelNameConfigure are not null
        ExecutionInfo.ExecutionInfoBuilder infoBuilder = ExecutionInfo.builder()
                .executionContext(executionContext)
                .modelNameConfigure(__ -> "test-model");
        
        lenient().when(executionInfo.toBuilder()).thenReturn(infoBuilder);
    }

    @Test
    @DisplayName("Should correctly return execution spec")
    void testExecutionSpec() {
        assertThat(execution.executionSpec()).isEqualTo(spec);
    }

    @Test
    @DisplayName("Should successfully execute structured with class type")
    void testExecuteClass() {
        GeneralResponse generalResponse = mock(GeneralResponse.class);
        AssistantTextMessage assistantTextMessage = mock(AssistantTextMessage.class);
        when(assistantTextMessage.getContent()).thenReturn("\"test\"");
        when(generalResponse.getAssistantTextMessage()).thenReturn(assistantTextMessage);

        when(spec.newExecutionContext()).thenReturn(executionContext);
        when(spec.isDefaultProvider()).thenReturn(true);
        when(registry.getDefaultProvider(any())).thenReturn(Mono.just(provider));
        when(spec.newExecutionInfo(any())).thenReturn(executionInfo);
        when(provider.executeGeneral(any())).thenReturn(Mono.just(generalResponse));

        StepVerifier.create(execution.execute(String.class))
                .expectNextMatches(resp -> "test".equals(resp.getStructuredContent()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should successfully execute structured raw with JSON schema")
    void testExecuteRawSchema() {
        RawResponse response = mock(RawResponse.class);
        String schema = "{}";
        when(spec.newExecutionContext()).thenReturn(executionContext);
        when(spec.isDefaultProvider()).thenReturn(true);
        when(registry.getDefaultProvider(any())).thenReturn(Mono.just(provider));
        when(spec.newExecutionInfo(any())).thenReturn(executionInfo);
        when(provider.executeGeneralRaw(any())).thenReturn(Mono.just(response));

        StepVerifier.create(execution.executeRaw(schema))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should successfully execute structured with parameterized type")
    void testExecuteParameterized() {
        GeneralResponse generalResponse = mock(GeneralResponse.class);
        AssistantTextMessage assistantTextMessage = mock(AssistantTextMessage.class);
        when(assistantTextMessage.getContent()).thenReturn("\"test\"");
        when(generalResponse.getAssistantTextMessage()).thenReturn(assistantTextMessage);

        ParameterizedTypeReference<String> typeRef = new ParameterizedTypeReference<String>() {};
        when(spec.newExecutionContext()).thenReturn(executionContext);
        when(spec.isDefaultProvider()).thenReturn(true);
        when(registry.getDefaultProvider(any())).thenReturn(Mono.just(provider));
        when(spec.newExecutionInfo(any())).thenReturn(executionInfo);
        when(provider.executeGeneral(any())).thenReturn(Mono.just(generalResponse));

        StepVerifier.create(execution.execute(typeRef))
                .expectNextMatches(resp -> "test".equals(resp.getStructuredContent()))
                .verifyComplete();
    }
}
