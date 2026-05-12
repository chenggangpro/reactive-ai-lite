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
import pro.chenggang.project.reactive.ai.lite.core.execution.response.RawStreamResponse;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionInfo;
import pro.chenggang.project.reactive.ai.lite.core.execution.values.ExecutionSpec;
import pro.chenggang.project.reactive.ai.lite.core.option.LlmClientType;
import pro.chenggang.project.reactive.ai.lite.core.provider.LlmChatProvider;
import pro.chenggang.project.reactive.ai.lite.core.provider.registry.LlmProviderRegistry;
import reactor.test.StepVerifier;

import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LlmProviderExecutorTest {

    @Test
    void testExecuteChat() {
        LlmProviderRegistry registry = mock(LlmProviderRegistry.class);
        ExecutionSpec spec = mock(ExecutionSpec.class);
        when(spec.getLlmClientType()).thenReturn(LlmClientType.CHAT);
        when(spec.isDefaultProvider()).thenReturn(true);

        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(spec.newExecutionContext()).thenReturn(executionContext);

        LlmChatProvider provider = mock(LlmChatProvider.class);
        when(registry.getDefaultProvider(any())).thenReturn(provider);

        ExecutionInfo executionInfo = mock(ExecutionInfo.class);
        when(spec.newExecutionInfo(any())).thenReturn(executionInfo);

        LlmProviderExecutor executor = LlmProviderExecutor.builder()
                .llmProviderRegistry(registry)
                .executionSpec(spec)
                .build();

        BiFunction<LlmChatProvider, ExecutionInfo, String> execution = (p, info) -> "result";

        String result = executor.executeChat(execution);
        assertThat(result).isEqualTo("result");
        verify(spec).newExecutionContext();
        verify(spec).newExecutionInfo(executionContext);
    }

    @Test
    void testLoadLlmProviderDefault() {
        LlmProviderRegistry registry = mock(LlmProviderRegistry.class);
        ExecutionSpec spec = mock(ExecutionSpec.class);
        when(spec.getLlmClientType()).thenReturn(LlmClientType.CHAT);
        when(spec.isDefaultProvider()).thenReturn(true);

        LlmChatProvider provider = mock(LlmChatProvider.class);
        when(registry.getDefaultProvider(any())).thenReturn(provider);

        LlmProviderExecutor executor = LlmProviderExecutor.builder()
                .llmProviderRegistry(registry)
                .executionSpec(spec)
                .build();

        LlmChatProvider result = executor.loadLlmProvider(mock(ExecutionContext.class), LlmProviderRegistry::getChatProvider);
        assertThat(result).isEqualTo(provider);
    }

    @Test
    void testExecuteStream() {
        LlmProviderRegistry registry = mock(LlmProviderRegistry.class);
        ExecutionSpec spec = mock(ExecutionSpec.class);
        when(spec.getLlmClientType()).thenReturn(LlmClientType.CHAT);
        when(spec.isDefaultProvider()).thenReturn(true);

        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(spec.newExecutionContext()).thenReturn(executionContext);

        LlmChatProvider provider = mock(LlmChatProvider.class);
        when(registry.getDefaultProvider(any())).thenReturn(provider);

        ExecutionInfo executionInfo = mock(ExecutionInfo.class);
        when(spec.newExecutionInfo(any())).thenReturn(executionInfo);

        LlmProviderExecutor executor = LlmProviderExecutor.builder()
                .llmProviderRegistry(registry)
                .executionSpec(spec)
                .build();

        RawStreamResponse chunk = mock(RawStreamResponse.class);
        reactor.core.publisher.Flux<RawStreamResponse> execution = reactor.core.publisher.Flux.just(chunk);

        reactor.core.publisher.Flux<RawStreamResponse> result = executor.executeChat((p, info) -> execution);

        StepVerifier.create(result)
                .expectNext(chunk)
                .verifyComplete();
    }

    @Test
    void testExecuteChatWithNoProviderFound() {
        LlmProviderRegistry registry = mock(LlmProviderRegistry.class);
        ExecutionSpec spec = mock(ExecutionSpec.class);
        when(spec.getLlmClientType()).thenReturn(LlmClientType.CHAT);
        when(spec.isDefaultProvider()).thenReturn(true);

        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(spec.newExecutionContext()).thenReturn(executionContext);

        // Return null for default provider
        when(registry.getDefaultProvider(any())).thenReturn(null);

        ExecutionInfo executionInfo = mock(ExecutionInfo.class);
        when(spec.newExecutionInfo(any())).thenReturn(executionInfo);

        LlmProviderExecutor executor = LlmProviderExecutor.builder()
                .llmProviderRegistry(registry)
                .executionSpec(spec)
                .build();

        // This should throw NPE because llmProvider is null
        assertThatThrownBy(() -> executor.executeChat((p, info) -> p.info()))
                .isInstanceOf(NullPointerException.class);
    }
}
