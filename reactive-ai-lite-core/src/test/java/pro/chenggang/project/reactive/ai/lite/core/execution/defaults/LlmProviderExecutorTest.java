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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContext;
import pro.chenggang.project.reactive.ai.lite.core.entity.context.ExecutionContextView;
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmChatProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LlmProviderExecutorTest {

    @Mock
    private LlmProviderRegistry registry;

    @Mock
    private ExecutionSpec spec;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private LlmChatProvider provider;

    @Mock
    private ExecutionInfo executionInfo;

    private LlmProviderExecutor executor;

    @BeforeEach
    void setUp() {
        executor = LlmProviderExecutor.builder()
                .llmProviderRegistry(registry)
                .executionSpec(spec)
                .build();
        when(executionContext.getContextView()).thenReturn(org.mockito.Mockito.mock(ExecutionContextView.class));
    }

    @Test
    @DisplayName("Should successfully execute chat operation")
    void testExecuteChat() {
        when(spec.newExecutionContext()).thenReturn(executionContext);
        when(spec.getLlmClientType()).thenReturn(LlmClientType.CHAT);
        when(spec.isDefaultProvider()).thenReturn(true);
        when(registry.getDefaultProvider(any())).thenReturn(Mono.just(provider));
        when(spec.newExecutionInfo(any())).thenReturn(executionInfo);

        Mono<String> result = executor.executeChat((p, info) -> Mono.just("result"));
        
        StepVerifier.create(result)
                .expectNext("result")
                .verifyComplete();
    }

    @Test
    @DisplayName("Should correctly load default provider")
    void testLoadLlmProviderDefault() {
        when(spec.getLlmClientType()).thenReturn(LlmClientType.CHAT);
        when(spec.isDefaultProvider()).thenReturn(true);
        when(registry.getDefaultProvider(any())).thenReturn(Mono.just(provider));

        StepVerifier.create(executor.loadLlmProvider(executionContext, LlmProviderRegistry::getChatProvider))
                .expectNext(provider)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should successfully execute streaming chat operation")
    void testExecuteStream() {
        when(spec.newExecutionContext()).thenReturn(executionContext);
        when(spec.getLlmClientType()).thenReturn(LlmClientType.CHAT);
        when(spec.isDefaultProvider()).thenReturn(true);
        when(registry.getDefaultProvider(any())).thenReturn(Mono.just(provider));
        when(spec.newExecutionInfo(any())).thenReturn(executionInfo);

        RawStreamResponse chunk = org.mockito.Mockito.mock(RawStreamResponse.class);
        Flux<RawStreamResponse> executionFlux = Flux.just(chunk);

        Flux<RawStreamResponse> result = executor.executeChatFlux((p, info) -> executionFlux);

        StepVerifier.create(result)
                .expectNext(chunk)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should throw error when provider is not found")
    void testExecuteChatWithNoProviderFound() {
        when(spec.newExecutionContext()).thenReturn(executionContext);
        when(spec.getLlmClientType()).thenReturn(LlmClientType.CHAT);
        when(spec.isDefaultProvider()).thenReturn(true);
        when(registry.getDefaultProvider(any())).thenReturn(Mono.error(new IllegalStateException("not found")));

        StepVerifier.create(executor.executeChat((p, info) -> Mono.just("test")))
                .expectError(IllegalStateException.class)
                .verify();
    }
}
