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
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.execution.GeneralExecution;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.GeneralResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionSpec;
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
class ChatGeneralExecutionTest {

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

    private GeneralExecution execution;

    @BeforeEach
    void setUp() {
        lenient().when(spec.getLlmClientType()).thenReturn(LlmClientType.CHAT);
        execution = ChatGeneralExecution.of(registry, spec);
    }

    @Test
    @DisplayName("Should correctly return execution spec")
    void testExecutionSpec() {
        assertThat(execution.executionSpec()).isEqualTo(spec);
    }

    @Test
    @DisplayName("Should successfully execute chat")
    void testExecute() {
        GeneralResponse response = mock(GeneralResponse.class);
        when(spec.isDefaultProvider()).thenReturn(true);
        when(registry.getDefaultProvider(any())).thenReturn(Mono.just(provider));
        when(spec.newExecutionInfo(any())).thenReturn(executionInfo);
        when(provider.executeGeneral(any())).thenReturn(Mono.just(response));

        StepVerifier.create(execution.execute())
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should successfully execute raw chat")
    void testExecuteRaw() {
        RawResponse response = mock(RawResponse.class);
        when(spec.isDefaultProvider()).thenReturn(true);
        when(registry.getDefaultProvider(any())).thenReturn(Mono.just(provider));
        when(spec.newExecutionInfo(any())).thenReturn(executionInfo);
        when(provider.executeGeneralRaw(any())).thenReturn(Mono.just(response));

        StepVerifier.create(execution.executeRaw())
                .expectNext(response)
                .verifyComplete();
    }
}
